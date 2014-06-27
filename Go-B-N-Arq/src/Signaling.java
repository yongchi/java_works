
	
public class Signaling {
	final static int SNDPACKET = 1;
	final static int RCVACK = 2;
	final static int TIMEOUTPACKET = 3;
	final static int GRACEOUT = 4;
	public static int notify_state=0,timeouttaskNo=0,rcvackNo=0,sndseqNo=0;

	public synchronized void sendkeynotifying(int i) {// System.out.println("send key board input: ");
		notify_state = SNDPACKET; 
		sndseqNo=i;
		notify();  
	} 
	public synchronized void timeoutnotifying(int i) {// System.out.println("Timeout: "); 
		notify_state = TIMEOUTPACKET; 
		timeouttaskNo=i;
		notify(); 
	} 
	public synchronized void acknotifying(int i) { // System.out.println("Ack Packet received");
		notify_state = RCVACK; 
		rcvackNo=i; 
		notify();  
	} 
	public synchronized void notifying() { // System.out.println("Graceful exit procedure"); 
		notify_state = GRACEOUT; 
		notify(); 
	} 
	public synchronized void waiting() {  // System.out.println("Waiting for the event: "); 
	 try { 	wait(); 	} catch(InterruptedException e) { 
		System.out.println("InterruptedException caught"); 
	 } 
	}
 	public int get_state() {
 		return notify_state;
 	}
	
}
