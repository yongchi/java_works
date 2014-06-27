
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


class RcvThread extends Thread {
	DatagramSocket socket;
	public DatagramPacket rcv_DatagramPacket[] = new DatagramPacket[MAXSIZE];// 송수신용 데이터그램 패킷
	private static final int MAXBUFFER = 512, MAXSIZE=16; //, WINSIZE=8;
	static final int HEADER_DATA = 8, HEADER_SEQ=0, HEADER_ACK=1, HEADER_FLAGS=2, HEADER_LENGTH=3,HEADER_CHECKSUM=4; // header length 
	public static int rcvseqNo=0,rcvackNo=0,rcvflag=0,rcvlength=0,current_ackNo=0;
	public byte[] rcv_data, CRCchecksum=new byte[4];
	boolean	cond=true, DEBUG=false;
	DatagramPacket rcv_packet, sndAck;// 수신용 데이터그램 패킷
	InetAddress dst_ip;
	int dst_port;
	Signaling pp;
	static SndThread sndThread; 
	
	RcvThread (DatagramSocket s,Signaling p,SndThread sndthread) {
		socket = s;
		pp=p;
		sndThread = sndthread;
	}	
	boolean corrupted(byte[] rcvd, int rcvl) { // Not corrupted: true, corrupted: false
		
		byte[] tempCRC=new byte[4]; // ACK size without data
		// byte[] tempdata= rcvd;
		// check Checksum or CRC 
		for(int i=0;i<4;i++) rcvd[i+HEADER_CHECKSUM] = 0x00; // reset CRC
		if(DEBUG) {System.out.print("rcvCRC packet");for(int i=0;i<(rcvl+8);i++) System.out.print(" "+Byte.toString(rcvd[i]));System.out.println("");}
		tempCRC=sndThread.getCRC(rcvd,rcvl+8);  // rcvlenght= data length including header
		if(DEBUG) {System.out.print("rcv CRC");for(int i=0;i<4;i++) System.out.print(" "+Byte.toString(tempCRC[i]));System.out.println("");}
		boolean result=true; //exact matching
		for(int i=0;i<4;i++) if(CRCchecksum[i]!=tempCRC[i]) {result=false; break;} // reset CRC
		return result;
	}
	void sendAck(int a) {
		
    		byte[] tempCRC = new byte[4];
			byte[] tempbyte=new byte[8]; // ACK size without data
			tempbyte[HEADER_SEQ]=(new Integer(sndThread.sn).byteValue());
			tempbyte[HEADER_ACK]=(new Integer(a).byteValue()); // ackNo could be updated by the RcvThread remotely
			tempbyte[HEADER_FLAGS]=(new Integer(1).byteValue()); // ACK Packet flag==1
			tempbyte[HEADER_LENGTH]=(new Integer(0).byteValue()); //s.length, excluding header length(8)
			for(int i=0;i<4;i++) tempbyte[i+HEADER_CHECKSUM] = 0x00; // reset 
			tempCRC = sndThread.getCRC(tempbyte,8);
			for(int i=0;i<4;i++) tempbyte[i+HEADER_CHECKSUM] = tempCRC[i]; // reset 
			sndAck = new DatagramPacket (tempbyte, (8),sndThread.remoteinetaddr, sndThread.remoteport);
			try {
				socket.send(sndAck);
			} catch (IOException e) { e.printStackTrace();
			}
	}

	
	void Receive(DatagramPacket rcv) {
		rcv_data = new byte[MAXBUFFER];
		try {
		       socket.receive(rcv);  // receive Frame
		     
			} catch(IOException e) {
				System.out.println("Thread exception "+e);
			}	
			   rcv_data = rcv_packet.getData();
			  
			   rcvseqNo= (int) ((Byte)rcv_data[HEADER_SEQ]).intValue();
			   rcvackNo= (int) ((Byte)rcv_data[HEADER_ACK]).intValue();
			   rcvflag= (int) ((Byte)rcv_data[HEADER_FLAGS]).intValue();
			   rcvlength = (int) ((Byte)rcv_data[HEADER_LENGTH]).intValue();
			   for(int i=0;i<4;i++) CRCchecksum[i] = rcv_data[i+HEADER_CHECKSUM];
	}
	
	
	public void run() {
		while (cond) {
			byte[] tempbyte=new byte[MAXBUFFER];
			rcv_packet = new DatagramPacket(tempbyte,tempbyte.length);
			Receive(rcv_packet); // Received Packet header and data
			sndThread.remoteinetaddr=rcv_packet.getAddress();// set remote port & remote addrif(sndThread.remoteinetaddr!=rcv_packet.getAddress()) 
			sndThread.remoteport=rcv_packet.getPort();// set remote port & remote addrif(sndThread.remoteport==rcv_packet.getPort()) 
			if(corrupted(rcv_data,rcvlength)) { 
				if((rcvseqNo==sndThread.rn)&&(rcvlength>0)) { // if Expected Seq Number (data packet received)
					String result = new String(rcv_data,8,rcvlength); // data only length
					if(!DEBUG)System.out.println("\n Receive Data(SeqNo="+rcvseqNo+" ackNo="+rcvackNo+") data: " + result); 
					sndThread.rn=(sndThread.rn+1)%MAXSIZE;
				 	sendAck(sndThread.rn); // send ACK PACKET if data packet
				} else if((rcvseqNo+MAXSIZE-sndThread.rn)%MAXSIZE<sndThread.winsize) sendAck(sndThread.rn); // send ACK PACKET if discard data packet
				if((rcvflag&0x1)==1) {
					
					if(((sndThread.sn+MAXSIZE-sndThread.sf)%MAXSIZE>=((rcvackNo+MAXSIZE-sndThread.sf)%MAXSIZE))&&(rcvackNo>sndThread.sf)) {
						sndThread.current_ackNo=rcvackNo; 
						if(!DEBUG){System.out.print("rcv Ack");for(int i=0;i<(rcvlength+8);i++) System.out.print(" "+Byte.toString(rcv_data[i]));System.out.println("");}
						pp.acknotifying(rcvackNo);  //RCVACK Received
					}
				}else if(DEBUG) System.out.println("out of Ack : sf="+sndThread.sf+" sn="+sndThread.sn+" ack="+rcvackNo);
			} else System.out.println("CRC Corrupted : CRC checksum");
		}
		System.out.println("grace out");
	}
	public void graceout(){
		cond=false;
	}
			
} 
