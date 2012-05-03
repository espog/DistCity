package channels;

import tests.Debug;

public class FLP2P implements P2PService, Debug{

	private P2PService upServ; // should be a PP2P link
	private P2PService downServ; //should be a BaseService link 
	
	private static boolean TEST = true;


	
	public FLP2P() {
		
	}
	
	public FLP2P(P2PService up, P2PService down) {
		this.upServ =up;
		this.downServ = down;
	}
	

	@Override
	public void deliver(int srcPid, Message msg) {
		debug(TEST, "\t\t <== P("+ getProcessID()+ ") FLP2P/deliver:  SrcPID: "+srcPid+ " Content: "+ msg.getMsg());

		this.upServ.deliver(srcPid, msg); // FLP2P delivers message directly
	}

	@Override
	public void send(int dstPid, Message msg) {
		
		debug(TEST, "==> P("+ getProcessID()+ ") FLP2P/send:  destPID: "+dstPid+ " Content : "+ msg.getMsg());
		
		// TODO PUT AN Probabilistic Message drop !!!
		double nb= Math.random() *100;
	
		if( nb <= 50 ){ //
			//do nothing -> drop message except  Re-Ack messages
			if(!msg.getType().equals(MessageType.RE_ACK)){
				System.err.println("P("+ getProcessID()+ ") FLP2P LINK  Failure : A message has been dropped. DestPID: " +dstPid+" Content: "+ msg.getMsg());
			}else
				this.downServ.send(dstPid, msg);
		}else
			this.downServ.send(dstPid, msg); // call the send method of the base service directly. No transformation preprocessing needed
	
	}

	/**
	 * @return the upServ
	 */
	public P2PService getUpServ() {
		return upServ;
	}

	/**
	 * @param upServ the upServ to set
	 */
	public void setUpServ(P2PService upServ) {
		this.upServ = upServ;
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

	@Override
	public void debug(boolean on, String msg) {
		// TODO Auto-generated method stub
		if(on)
			System.out.println(msg);
	}

	@Override
	public int getProcessID() {
		
		return this.downServ.getProcessID();
	}


}
