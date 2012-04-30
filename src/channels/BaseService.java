package channels;

import java.util.HashMap;
import java.util.Map;

import tests.Debug;

public class BaseService implements P2PService, Debug { 


	private static Map<Integer,TCPClient> outGoingConnections;
	private static Map<Integer,Integer> pid_addr_map;


	private LocalProcess process;
	private P2PService upperServ;	
	private static final String msgSep = ":";

	private static boolean TEST = true;

	
	
	
	/**
	 * @param process 
	 */
	public BaseService(LocalProcess process) {
		this.process = process;
		//set this as the listening service of the server
		this.process.getServ().setListeningServ(this);
	}
	
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
		debug(TEST, "\t\t <== P("+ this.process.getPid()+ ") BaseService/deliver: "+ msg.getMsg());
		
		//Before delivering it, must determine what kind of message it is.It might be Data,Ack or reAck
		//Parsing the message. The message is of the form : <TYPE>:<MSG_ID>:<CONTENT>
		Message msgToDeliver = null;
		String currentMsg = msg.getMsg();
		//looking for first part: the TYPE of the message
		int indxOfSep = currentMsg.indexOf(':');
		String type = msg.getMsg().substring(0,indxOfSep);
		
		//looking for second part the message ID
		currentMsg = currentMsg.substring(indxOfSep+1);
		indxOfSep = currentMsg.indexOf(':');
		String msgID = currentMsg.substring(0,indxOfSep);
		//building the final message to deliver
		msgToDeliver = new Message(currentMsg.substring(indxOfSep+1),Integer.parseInt(msgID));
		
		
		if(type.equalsIgnoreCase("data")){
			
//			debug(TEST, "\t\t <== P("+ this.process.getPid()+ ") BaseService/deliver: Data delivery ");
			msgToDeliver.setType(MessageType.DATA);
		}
		
		if(type.equalsIgnoreCase("ack")){
			
//			debug(TEST, "\t\t <== P("+ this.process.getPid()+ ") BaseService/deliver: Ack delivery");
			msgToDeliver.setType(MessageType.ACK);
				
		}
		if(type.equalsIgnoreCase("ackack")){
			
//			debug(TEST, "\t\t <== P("+ this.process.getPid()+ ") BaseService/deliver: RE_ACK delivery");
			msgToDeliver.setType(MessageType.RE_ACK);
				
		}
		
//		debug(TEST, "\t\t <== P("+ this.process.getPid()+ ") in BaseService- About to deliver . CurrMsg= "+msgToDeliver.getMsg());

		this.upperServ.deliver(srcPid, msgToDeliver);
	}

	@Override
	public void send(int dstPid, Message msg) {

		debug(TEST, "==>P("+ this.process.getPid()+ ") BaseService/send: "+ msg.getMsg());

		
//		System.out.println("Destination PID: "+ dstPid);
		
		//get the connection to the destination process
		TCPClient client = getConnection(dstPid);
		
		String rawMessage = ""+this.getProcessID();
		
		
		if (msg.getType().equals(MessageType.DATA)){
			rawMessage+=msgSep+"DATA"+msgSep+msg.getMsg(); // example msg = "<pid>:DATA:<msgID>:<content>"
		}
		if(msg.getType().equals(MessageType.ACK)){ 
			rawMessage+=msgSep+"ACK"+msgSep+msg.getMsg();	 //example msg = "<pid>:ACK:<msgID>:<content>"
		}
		if(msg.getType().equals(MessageType.RE_ACK)){
			rawMessage+=msgSep+"ACKACK"+msgSep+msg.getMsg();	//example msg = "<pid>:ACKACK:<msgID>:<content>"
		}
				
		// look up the process
		if (client !=  null){	
			client.send(new Message(rawMessage));
		}else
			throw new RuntimeException("No client connection has been previously established ! ");
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

	/**
	 * 
	 * @param pid the process for which to lookup the connection
	 * @return a TCP client connection  to contact the associated process
	 */
	private static TCPClient getConnection(int pid){
		TCPClient client = null;
//		TCPClient prev_cl = null;
	
		if(outGoingConnections == null)
			outGoingConnections = new HashMap<Integer, TCPClient>();
		client = outGoingConnections.get(new Integer(pid));

		if(client == null){
			client = new TCPClient(TCPClient.DEFAULT_HOST,getAddressPort(pid));
			outGoingConnections.put(new Integer(pid), client);
		}//else{
//			prev_cl = client; 
//			prev_cl.close();
//		}
		
//		client = new TCPClient(TCPClient.DEFAULT_HOST,getAddressPort(pid)); 	
		
		return client;

	}

	public static void register(int pid, int port){
		if(pid_addr_map == null)
			pid_addr_map = new HashMap<Integer, Integer>();

		pid_addr_map.put(new Integer(pid), new Integer(port));
	}

	private static int getAddressPort(int pid){

		int addr_port = -1;

		if(pid_addr_map != null){
			addr_port = pid_addr_map.get(new Integer(pid));
		}
		return addr_port; //TODO TEMPORARY !!!
	}

	/**
	 * @return the upperServ
	 */ 
	public P2PService getUpperServ() {
		return upperServ;
	}

	/**
	 * @param upperServ the upperServ to set
	 */
	public void setUpperServ(P2PService upperServ) {
		this.upperServ = upperServ;
	}

	public void debug(boolean on, String msg) {
		if(on)
			System.out.println(msg);
		
	}

	@Override
	public int getProcessID() {
		return this.process.getPid();
	}

}
