import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class SndThread extends Thread {
	static final int HEADER_DATA = 8, HEADER_SEQ=0, HEADER_ACK=1, HEADER_FLAGS=2, HEADER_LENGTH=3,HEADER_CHECKSUM=4; // header length 
	static final int MAXBUFFER=512, MAXTIMEOUT=5, MAXSIZE = 16;
	final static int EVENT_REQSND = 1, EVENT_RCVACK = 2, EVENT_TIMEOUTPACKET = 3;
	public static byte[][] snd_data = (byte[][]) new byte[MAXSIZE][MAXBUFFER];
	DatagramSocket socket;
	DatagramPacket sndPacket;// 송신용 데이터그램 패킷
	public InetAddress myinetaddr, remoteinetaddr;
	public int myport=0, remoteport=0, winsize=2; // s&w ARQ default
	Signaling pp;
	Timeout tout;
	boolean DEBUG=false;
	
	public int sf=0,sn=0,rn=0; 
	public static int seqNo=0,ackNo=0,flag=0,length=0;
	public int current_ackNo=0;
	public DatagramPacket snd_DatagramPacket[]=new DatagramPacket[MAXSIZE];// 송수신용 데이터그램 패킷 array
	public byte[][] snd_Datagramacket_header; // (seq,ack,flag,length,checksum(4byte)) 8 byte 
	static String key_in_data[]= new String[MAXSIZE];

	SndThread (DatagramSocket s,Signaling p, Timeout to) {
		socket = s;
		pp=p;
		tout=to;
	}	
	
	public void init() {
        if(!DEBUG) {System.out.print("Sf sn parameter init");}
		sf=sn=rn=0; // init Control NO.
	}
	
	public void PutData(String a, int i) {
		key_in_data[i] = (String)a;
	} 
	
	public String GetData(int i) {
		return key_in_data[i];
	}
	
    public byte[] getCRC(byte[] frame, int length)	//CRC 계산
    {
    	byte[] tempCRC = new byte[4];
   		int crc  = 0xFFFFFFFF;       // initial contents of LFBSR
        int poly = 0xEDB88320;   // reverse polynomial

        for (int j = 0; j < length; j++) {
            int temp = (crc ^ frame[j]) & 0xff;

            // read 8 bits one at a time
            for (int i = 0; i < 8; i++) {
                if ((temp & 1) == 1) temp = (temp >>> 1) ^ poly;
                else                 temp = (temp >>> 1);
            }
            crc = (crc >>> 8) ^ temp;
        }

        // flip bits
        crc = crc ^ 0xffffffff;
    	
        tempCRC[3] = (byte)(crc & 0xff);
        tempCRC[2] = (byte)(crc >>> 8 & 0xff);
        tempCRC[1] = (byte)(crc >>> 16 & 0xff);
        tempCRC[0] = (byte)(crc >>> 24 & 0xff);
        
        if(DEBUG) {System.out.print("calculated CRC : "+new String(tempCRC));}
        
        return tempCRC;
    }

	
	public DatagramPacket MakeFrame(int isn, String s) { // Make ARQ Key in Frame -> snd_buffer[sn]
    	byte[] tempCRC = new byte[4];
		byte[] tmp= new byte[MAXSIZE];
		tmp = s.getBytes();
		int len= tmp.length;
		snd_data[isn][HEADER_SEQ]=(new Integer(isn).byteValue());
		snd_data[isn][HEADER_ACK]=(new Integer(ackNo).byteValue()); // ackNo could be updated by the RcvThread remotely
		snd_data[isn][HEADER_FLAGS]=(new Integer(flag).byteValue());
		snd_data[isn][HEADER_LENGTH]=(new Integer(len).byteValue()); //s.length, excluding header length(8)
		for(int i=0;i<4;i++) snd_data[isn][i+HEADER_CHECKSUM] = 0x00; // reset 
		for(int j=0;j<len;j++)
			snd_data[isn][j+HEADER_DATA] = tmp[j];
		tempCRC = getCRC(snd_data[isn],len+8);
		for(int i=0;i<4;i++) snd_data[isn][i+HEADER_CHECKSUM] = tempCRC[i]; // reset 
		if(DEBUG) {System.out.print("snd packet");
				for(int i=0;i<(len+8);i++) 
				System.out.print(" "+Byte.toString(snd_data[isn][i])); System.out.println("");
		}
		sndPacket = new DatagramPacket (snd_data[isn], (len+8),remoteinetaddr, remoteport);
		return sndPacket;
	}

	
	public void StoreFrame(int is,DatagramPacket d) {
		snd_DatagramPacket[is] = d;
	}
	
	public void SendFrame(int i){
		try {
			socket.send(snd_DatagramPacket[i]);
		} catch (IOException e) { e.printStackTrace();
		}
	}
	public void sendTimeoutPacket (int i) {
	    try {
		    socket.send(snd_DatagramPacket[i]);
	     } catch (IOException e) { e.printStackTrace();
	     }
	}	
	

	public void run() {
		while(true) { 		//  sndThread는 지속적으로 패킷 전송을 위해 무한 루프로 동작
			pp.waiting(); // waiting until notifying & return wake up state 
			int kk = pp.get_state(); if(DEBUG) System.out.println("notify "+kk);
			if(kk==EVENT_REQSND){
				// Keyboard 입력 데이터를 기반으로 Header와 데이터를 구성하고 UDP 패킷으로 만들어 보냄 
				// 입력: keyboard 입력 스트링, 
				// 동작: Header 및 데이터 구성 후 UDP 패킷으로 전송 그리고 재전송을 위한 타임아웃을 세팅해 놓음 
				if((sn +MAXSIZE-sf)%MAXSIZE < winsize) { // sn is within MAXWIN
					if(Signaling.sndseqNo!=sn) System.out.println("key in position error: sndSeq No="+Signaling.sndseqNo+" sn="+sn);
					String a = GetData(Signaling.sndseqNo);  // Key in data is stored in Keydata[sn]
					if(DEBUG) System.out.println("key in string ="+a+" sndseqNo="+Signaling.sndseqNo+" sn="+sn); 
					DatagramPacket snd = MakeFrame(sn,a);
					StoreFrame(sn, snd); // put DatagramPacket into snd_DatagramPacket[sn] array
					SendFrame(sn); // put DatagramPacket into snd_DatagramPacket[sn] array
				    tout.Timeoutset(sn,1000,pp);	// Timeout Start
					sn=(MAXSIZE+sn+1)%MAXSIZE; // update sn 
				} else if(DEBUG) System.out.println("Seq No: exceed boundary (unable to sent until receive ack)");
			}
			
			if(kk == EVENT_RCVACK){ //ack Received move sf -> rcvackNo
				// 이전에 보낸 패킷에 대한 ACK 패킷을 받은 경우 (rcvThread에서 받음)
				// 동작: 패킷 송신 시 설정된 timeout를 제거하고 무한 재전송을 방지하기 위한 timeout limit를  초기화 함.
				tout.timeoutlimit = 0; // Packet transmission reset
				for(int i=sf;i< Signaling.rcvackNo;i++) // if cumulative ack
					tout.Timeoutcancel(i); 	//cancel timeoutTASK previous task 
				sf=Signaling.rcvackNo; //update seq no.
			}
			
			if(kk==EVENT_TIMEOUTPACKET){
				// Timeout interrupt를 받은 경우 (주기적인 타이머가 세팅한 시간에 interrupt 받음)
				// 동작: 패킷을 재전송하고 다시  타임아웃을 세팅해 놓음, 또한 무한 재전송을 방지하기 위한 기능.
				if(tout.timeoutlimit < MAXTIMEOUT) {
					System.out.println("Timeout task No ="+Signaling.timeouttaskNo+" sf="+sf);
					for(int tmp=sf;tmp<sn;tmp++){
						sendTimeoutPacket (tmp); // sf packet retransmission
						if(tmp>sf) tout.Timeoutcancel(tmp);
						tout.Timeoutset(tmp,1000,pp);	// Timeout Start
					}
					tout.timeoutlimit++;
				} else {
					System.out.println("Timeout reset (unable to be sent)");
					tout.timeoutlimit = 0; // Packet transmission fail
				}
			}else if(kk>EVENT_TIMEOUTPACKET){
				System.out.println("Out of notify state");
				System.exit(0);
			}
		} 
	}
}
