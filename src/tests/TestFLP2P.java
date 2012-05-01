package tests;

import channels.Message;
import channels.P2PService;

public class TestFLP2P implements P2PService {

	
	private P2PService down;
	private P2PService up;
	/**
	 * 
	 */ 
	public TestFLP2P(){

	}

	@Override
	public void deliver(int srcPid, Message msg) {
		// TODO Auto-generated method stub
		
		System.out.println("Msg received from: " + srcPid);
		System.out.println("Content: "+msg.getMsg());
		System.out.println("sending the message back to: " + srcPid);
		this.down.send(srcPid, new Message(msg.getMsg()+"bis!"));
	
	}

	@Override
	public void send(int dstPid, Message msg) {
		// TODO Auto-generated method stub
		this.down.send(dstPid, msg);
	}

	/**
	 * @param down the down to set
	 */
	public void setDown(P2PService down) {
		this.down = down;
	}

	/**
	 * @param up the up to set
	 */
	public void setUp(P2PService up) {
		this.up = up;
	}

	@Override
	public int getProcessID() {
		// TODO Auto-generated method stub
		return 0;
	}

	

}
