import java.util.Timer;
import java.util.TimerTask;


public class Timeout {
    Timer timer= new Timer();
    TimeoutTask[] myTimerTask = new TimeoutTask[16]; //MAXSIZE
    Signaling pp;
    public int timeoutlimit=0;
    static int ii=0;
    int k;
    int sendquNo;
    boolean DEBUG=false;
    public void Timeoutset (int i, int milliseconds, Signaling p) {
    	// TimeoutTask 설정: 보낸 패킷의 seq 번호에 따라 timeout 시간을 설정해서 Timer에 설정함
    	k=i;
    	pp=p;
    	myTimerTask[k]=new TimeoutTask(k);
        timer.schedule(myTimerTask[k], milliseconds);
	}
    public void Timeoutcancel (int i) {
    	// TimeoutTask 제거: Ack 받은 패킷의 seq 번호에 따라 timeoutTask를 Timer에서 제거함
    	k=i;
    	if(DEBUG) System.out.println("Time's cancealed! no="+k);
        myTimerTask[k].cancel();
	}

    class TimeoutTask extends TimerTask {
    	int jj;
    	TimeoutTask(int j) {    		
    		jj=j;
    	}
    	public void run() {
            if(DEBUG) System.out.println("Time's up! "+(timeoutlimit));
            pp.timeoutnotifying(jj);
            this.cancel(); //Terminate the timerTask thread
        }
    }
}
