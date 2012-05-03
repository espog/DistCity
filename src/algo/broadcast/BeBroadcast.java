/**
 * 
 */
package algo.broadcast;

import java.util.LinkedList;
import java.util.List;
import channels.Message;
import channels.P2PService;

/**
 * Best-Effort Broadcast
 *
 */
public class BeBroadcast implements BroacastService {

	
	
	private P2PService downServ; //will be the PP2P link
	private BroacastService upServ; //will be Uniform Reliable Broadcast
	private List<Integer> pidList;
	
	
	/**
	 * constructor 
	 */
	public BeBroadcast() {
		
		this.pidList = new LinkedList<Integer>();
		for (int i = 0; i < ProcessManager.NB_PROCESSES; i++) {
			this.pidList.add(new Integer(i+1)); 
		}
	}

	public BeBroadcast(P2PService down) {
		this.downServ = down;		
	}
	

	
	/**
	 *
	 */
	@Override
	public void deliver(int srcPID, Message msg) {
		this.upServ.deliver(srcPID, msg);
	}

	/**
	 * 
	 */
	@Override
	public void send(Message msg) {
		for (Integer objPID : this.pidList) {
			this.downServ.send(objPID, msg);
		}
	}



	/**
	 * @return the downServ
	 */
	public P2PService getDownServ() {
		return downServ;
	}



	/**
	 * @param downServ the downServ to set
	 */
	public void setDownServ(P2PService downServ) {
		this.downServ = downServ;
	}



	/**
	 * @return the upServ
	 */
	public BroacastService getUpServ() {
		return upServ;
	}



	/**
	 * @param upServ the upServ to set
	 */
	public void setUpServ(BroacastService upServ) {
		this.upServ = upServ;
	}

	@Override
	public int getProcessID() {
		return this.downServ.getProcessID();
	}

}
