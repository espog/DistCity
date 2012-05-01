package channels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import broadcast.BroacastService;

import tests.Debug;

/**
 * 
 * represents the Perfect point to point link
 *
 */
public class PP2P implements P2PService, Debug {
	
	
	public static boolean TEST= true;
	//TODO will need to be global (because it can be used by Broadcast algo or Failure detector)
	public static final int NB_PROCESSES = 3; 
	
	private final String ACK_MSG ="ack";
	private final String RE_ACK_MSG = "ackack";

	
	//Process Management
	private static Map<Integer,Integer> pid_NxtMsgID_map = new HashMap<Integer, Integer>();
	private static Map<Integer,ProcessInfo> pid_info_map = new HashMap<Integer, PP2P.ProcessInfo>();
	// mapping PID->List<MessageID> which is the list of Message ID deliverd
	public static Map<Integer,List<Integer>>  delivered_map = new HashMap<Integer, List<Integer>>(); 
	private static final int DELAY = 1000; //every second 
	private static final int INITIAL_ID = 1;
	
	//Services
	private P2PService downServ;
	private BroacastService upService; //!!!! Here it is a special service (in Algorithms layer)

	public PP2P(){
		initLink();
	}
	
	public PP2P(BroacastService up, P2PService down) {
			
		this.upService = up;
		this.downServ = down;
		initLink();
		
	}

	private void initLink(){
		
		//Initialize MessageID 
		for (int i= 0 ; i < NB_PROCESSES ; i++){
			pid_NxtMsgID_map.put(new Integer(i+1), new Integer(INITIAL_ID)); // for each process pid i , Initial MsgId is 0
		}
		
		//initializing data structures for process management
		//for each process, init processInfo "bag"
		for (int i = 0; i < NB_PROCESSES; i++) {
			pid_info_map.put(new Integer(i+1), new ProcessInfo());
		}
		
		//delivered map
		for (int i = 0; i < NB_PROCESSES; i++) {
			delivered_map.put(i+1,new LinkedList<Integer>());
		}
		
		//Initializing Timer for resending messages in caseof loss
		Timer t  = initTimer();
		t.start();
	}
	
	
	/**
	 * Declares the timer that fire retransmission of messages that have not been acknowledged
	 */
	private Timer initTimer() {
		Timer t = new Timer(DELAY, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				reTransmitPending();
			}
		});
		return t;
	}

	/**
	 * @param upServ the upServ to set
	 */
	public void setUpServ(BroacastService upServ) {
		this.upService = upServ;
	}

	@Override
	public void deliver(int srcPid, Message msg) {
		
		debug(TEST, "\t\t <== PP2P/deliver: "+msg.getMsg());
		
		//Getting the info related to the process with src PID
		Integer objPID  = new Integer(srcPid);
		ProcessInfo pInfo = pid_info_map.get(objPID);
		Message ackMesg =null ;
			
		int currMsgID  = msg.getId();
		
		//Message is a normal message (containing new Data to deliver)
		if(msg.getType().equals(MessageType.DATA)){
			
			//check if message has a correct msgID 
			if (!isCorrectID(currMsgID)){
						throw new RuntimeException("Expected ID is not correct - Current PID: "+getProcessID());
						
			}
			//check if message hasn't been delivered yet 
			if (!hasBeenDelivered(srcPid, currMsgID)){
					putInDeliveredSet(srcPid,currMsgID);
					//TODO Message ID shouldn'be delivered !!! change
					
					this.upService.deliver(srcPid, msg);
					ackMesg = new Message(ACK_MSG,currMsgID);
					ackMesg.setType(MessageType.ACK);
					pInfo.putMsgAckSent(currMsgID, ackMesg);
					
					debug(TEST, "Test- Message delivered: " + msg.getMsg());
			}else{
			
				ackMesg = pInfo.getAckSending_map().get(currMsgID);
				debug(TEST, "PP2P/deliver - Sending Ack 2nd time: "+ ackMesg.getMsg());

			}
			this.downServ.send(srcPid, ackMesg);
			debug(TEST, "Test - Ack Sent: "+ ackMesg.getMsg());
		}
		
		//Message is an ACK
		if(msg.getType().equals(MessageType.ACK)){
			pInfo.removeFromSent(currMsgID);
			ackMesg = new Message(RE_ACK_MSG,currMsgID);
			ackMesg.setType(MessageType.RE_ACK);
			this.downServ.send(srcPid, ackMesg); //sending a ReACk message
			
			debug(TEST, "Test - Re-Ack message sent "+ ackMesg.getMsg());
		}
		
		//Message is an ACK to an ACK		
		if(msg.getType().equals(MessageType.RE_ACK)){
			
			debug(TEST, "\t\t <== PP2P/deliver (Re-ACK received): "+msg.getMsg());
			if(hasBeenDelivered(srcPid, currMsgID)) //just in case
				throw new RuntimeException("Received a Re ACK for a message that has not been delivered! Problem !!!");
			removeFromDeliverSet(srcPid, currMsgID);
		
		}

		pid_info_map.put(objPID, pInfo); 		
	}

	@Override
	public void send(int dstPid, Message msg) {
		
		debug(TEST,"==> PP2P/send : dstPID:"+ dstPid+ " Content: "+msg.getMsg());
		
		Integer objPID  = new Integer(dstPid);
		ProcessInfo pInfo = pid_info_map.get(objPID);
		
		int nxtMsgID =(int) pid_NxtMsgID_map.get(objPID);
		Message msgToSend = new Message(msg.getMsg(),nxtMsgID);
		msgToSend.setType(MessageType.DATA);
		
		nxtMsgID++;
		pid_NxtMsgID_map.put(objPID, nxtMsgID);
		
		
		pInfo.putSentMessage(nxtMsgID, msgToSend);
		pid_info_map.put(objPID, pInfo);
		
		debug(TEST, "==> PP2P/send: Message To Send: "+ msg.getMsg());
		this.downServ.send(dstPid, msgToSend); //sending the message over the FLP2P link 
			
	}
	
	
	/**
	 * 
	 */
	private  void reTransmitPending() {
		
		for (Integer objPID : pid_info_map.keySet()) {
			ProcessInfo pInfo =  pid_info_map.get(objPID);
			Message m = null;
			
			//Re send all messages for which no acknowledgement hasn't been received yet
			for (Integer msgID: pInfo.getSending_map().keySet()){
				 m = pInfo.getSending_map().get(msgID);
				 this.downServ.send(objPID, m);
			}
			
			//Re send all messages for which no Re ack hasn't been received yet
			for (Integer msgID: pInfo.getAckSending_map().keySet()){
				 m = pInfo.getAckSending_map().get(msgID);
				 this.downServ.send(objPID, m);
			}
		
		}
	}

	/*
	 * says whether the given msgID is correct for given PID
	 */
	private boolean isCorrectID(int msgID){
		

		Integer objPID = new Integer(getProcessID());
		int id = pid_NxtMsgID_map.get(objPID);
//		debug(TEST, "Method PP2P/isCorrectID : Current MsgID="+msgID+" \n Next MsgID for process("+objPID.intValue()+")="+id);
		return msgID < id;
	
	}

	
	/*
	 *  put msgId of message that have been delivered
	 */
	public static void putInDeliveredSet(int pid, int msgId){
			
		Integer pidObj = new Integer(pid);
		Integer midObj = new Integer(msgId);
		
		List<Integer> listID =delivered_map.get(pidObj);
		if(listID.contains(midObj))
				throw new RuntimeException("Unexpected - Message ID:"+ msgId + " already contained in the delivery Set !!!");
		
		listID.add(midObj);
		delivered_map.put(pidObj, listID);
		
		
	}
	/*
	 * remove msgId of message from the delivered set. This avoids
	 */
	public static void removeFromDeliverSet(int pid, int msgId){
		
		Integer pidObj = new Integer(pid);
		Integer midObj = new Integer(msgId);
		
		List<Integer> listID =delivered_map.get(pidObj);
		
		if(!listID.contains(midObj))
			throw new RuntimeException("Unexpected - Message ID:"+ msgId + " is not in the delivery Set !!!");

		listID.remove(midObj);
		delivered_map.put(pidObj, listID);
		

	}
	
	/*
	 * says whether the message with given ID has been delivered 
	 */
	public static boolean hasBeenDelivered(int pid, int msgID){
		Integer pidObj = new Integer(pid);
		Integer midObj = new Integer(msgID);
		return delivered_map.get(pidObj).contains(midObj);
	}
	
	
	
	
	
	
	
	//Inner class for holding single process information
	
	class ProcessInfo {
		
		//Sending Management	
		
		private Map<Integer,Message> sending_map; // contains mapping MsgID->Msg which has been sent but not yet acknowledged by dest Process.
	
		//Delivering Management
		private Map<Integer,Message> ackSending_map; // mapping Mid->Messages for messages received and delivered and for which an ack has been sent to the sender
		
		private ProcessInfo(){		
			this.ackSending_map = new HashMap<Integer, Message>();
			this.sending_map = new HashMap<Integer, Message>();
		}
		
		/*
		 * store msgId and message for which an ack is expected 
		 */
		public void putSentMessage(int msgId, Message m){
			this.sending_map.put(msgId,m);
		}

		/*
		 * store Msg in map of messages send but for which an ack has been sent.   
		 */
		public void putMsgAckSent(int msgId, Message m){
			this.ackSending_map.put(msgId, m);
		}
		
		/*
		 * remove message with corresponding id from the map of messages to send
		 */
		public void removeFromSent(int msgId){
			this.sending_map.remove(new Integer(msgId));
		}

		/*
		 * remove message with msgId from set of messages for which an ack is expected.
		 */
		public void removeFromAckSentSet(int msgId){
			this.ackSending_map.remove(msgId);
		}
		
	

		/**
		 * @return the sending_map
		 */
		public Map<Integer, Message> getSending_map() {
			return sending_map;
		}

		/**
		 * @return the ackSending_map
		 */
		public Map<Integer, Message> getAckSending_map() {
			return ackSending_map;
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
	
	
	/* (non-Javadoc)
	 * @see channels.BaseService#debug(boolean, java.lang.String)
	 */
	public void debug(boolean on, String msg) {		
		if(on)
				System.out.println(msg);
	}

	@Override
	public int getProcessID() {
		return this.downServ.getProcessID();
	}
}
