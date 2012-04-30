package channels;

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
		
		System.out.println("Msg received from:" + srcPid);
		System.out.println("Content: "+msg.getMsg());
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

	

}
