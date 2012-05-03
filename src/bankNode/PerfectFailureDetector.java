package bankNode;

import java.util.HashMap;
import java.util.Timer;

public class PerfectFailureDetector {
	
	private HashMap alive;
	private HashMap detected;
	private Timer timer;
	
	public PerfectFailureDetector() {
		alive = getBankMap();
		detected = null;
		
	}
	public void TimeOut(){
		
	}
}
