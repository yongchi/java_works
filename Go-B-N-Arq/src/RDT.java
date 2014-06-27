
import java.io.*; 
import java.net.*; 


public class RDT {
	final static int MAXBUFFER = 512, MAXSIZE=16;
	static int keyinposition =0,winsize=MAXSIZE;
	static SndThread sndThread; 
	static RcvThread rcThread; 
	public static DatagramSocket socket;  // main socket 
	public static Signaling p = new Signaling();  // signaling Interface
	public static Timeout tclick = new Timeout(); // Timeout Interface

	public static void main(String[] args) {
		int port=0;
		int mode =0; // mode=1: server mode, mode=2: client mode
		InetAddress addr=null;
		if (args.length == 0) {
				System.out.println("사용법: java UDPChatting localhost port or  java UDPChatting myport");
				System.exit(0);
		} else if(args.length == 1){
				// server mode without sending first: wait for an incoming message
			port = Integer.parseInt(args[0]); //server mode
			mode=1;//server mode
		} else if(args.length == 2){				// client mode
				mode=2;//Client mode
				port = Integer.parseInt(args[1]);
			try {
				addr = InetAddress.getByName(args[0]);
			} catch (UnknownHostException e) { 
				e.printStackTrace();
			}
		} else if(args.length == 3){
			// client mode & MAXSIZE
			mode=2;//Client mode
			port = Integer.parseInt(args[1]);
			try {
				addr = InetAddress.getByName(args[0]);
			} catch (UnknownHostException e) { 
				e.printStackTrace();
			}
			winsize=sndThread.winsize=Integer.parseInt(args[2]);
		} else {
			System.out.println("Mode Error ");
		}
		
		try {
			if(mode==1) socket = new DatagramSocket(port); //Server mode
			else {socket = new DatagramSocket(); } // Client mode
			
            // DatagramPacket recv_packet;// 수신용 데이터그램 패킷
			sndThread = new SndThread(socket, p, tclick);
			sndThread.start();
			rcThread = new RcvThread(socket, p, sndThread);
			rcThread.start();
			if(mode>1) { // set_remote_address_port(addr, port} if needed;
				sndThread.remoteinetaddr=addr;
				sndThread.remoteport=port;
			}
            System.out.println("Datagram my address on "+socket.getLocalAddress().getHostAddress()+" my port "+socket.getLocalPort());
			
				// 키보드 입력 읽기
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			    
			
			while (true) {
				System.out.print("Input Data : ");
				 String data = br.readLine();
				if (data.length() == 0){ // no char carriage return 
					System.out.println("grace out call");
					break;
				} else if(sndThread.remoteinetaddr!=null) {  // Valid data
					sndThread.PutData(data,keyinposition); 
				   p.sendkeynotifying(keyinposition); //sndThread.sendkey(RDTbuffer.key_data+"\0");  /* Key board type to be sent through Datagram Socket*/
				   keyinposition=(keyinposition+1)%MAXSIZE; // update key in position within the WIndow (winsize<16)
				} System.out.println("server mode waiting for incoming packet");
			}

			// 프로그램 종료을 위한 과정 처리
			rcThread.graceout(); // grace exit of Receive Thread 
			socket.close(); // close socket
			System.exit(0); 
			
		} catch(UnknownHostException ex) {
			System.out.println("Error in the host address ");
		} catch(IOException e) {
			System.out.println(e);
		}
	}
}


