package channels;

import java.util.Map;

public class BaseService implements P2PService {
	
	public static Map<Integer,TCPClient> map;
	
	
	private LocalProcess process;
	private P2PService upperServ;
	private TCPClient client = null; //TODO for testing
	

	

	/**
	 * @param process
	 * @param upperServ
	 */
	public BaseService(LocalProcess process, P2PService upperServ) {
		this.process = process;
		this.upperServ = upperServ;
		//set this as the listening service of the server
		this.process.getServ().setListeningServ(this);
	}
	

	@Override
	public void deliver(int srcPid, Message msg) {
		// TODO
		this.upperServ.deliver(srcPid, msg);
	}

	@Override
	public void send(int dstPid, Message msg) {
		// TODO Auto-generated method stub
		
		String rawMessage = ""+this.process.getPid();
		rawMessage+=":"+msg.getMsg();
		
		// look up the process
		if (this.client == null){	
			client = new TCPClient("localhost", dstPid); //TODO for testing use dstPid as destination Port 
			client.send(new Message(rawMessage));
		}
	}

	/**
	 * @return the process
	 */
	public LocalProcess getProcess() {
		return process;
	}

	/**
	 * @param process the process to set
	 */
	public void setProcess(LocalProcess process) {
		this.process = process;
	}
	
}
