package channels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Timer;

import tests.Debug;
import broadcast.BroacastService;

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
	private final char MSG_SEP=':';


	//Process Management
	private static Map<Integer,Integer> pid_NxtMsgID_map = new HashMap<Integer, Integer>();
	private static Map<Integer,ProcessInfo> pid_info_map = new HashMap<Integer, PP2P.ProcessInfo>();
	// mapping PID->List<MessageID> which is the list of Message ID delivered
	//TODO delete
	private static final int DELAY = 1000; //every second 
	private static final int INITIAL_ID = 1;

	//Services
	private P2PService downServ;
	private BroacastService upService; //!!!! Here it is a special service (in Algorithms layer)
	//other
	private int currentPID = -1; //will be updated later

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

		debug(TEST, "\t\t <== P("+ getProcessID()+ ") PP2P/deliver: "+msg.getMsg());

		//Getting the info related to the process with src PID
		Integer objCurrentPID  = new Integer(getProcessID()); 
		Integer objPID = new Integer(srcPid);

		ProcessInfo pInfo = pid_info_map.get(objCurrentPID); // info of the current process (receiving the delivery)

		Message ackMesg =null ;
		Message toDeliver = null;
		int currMsgID  = msg.getId();


		//Message is a normal message (containing new Data to deliver)
		if(msg.getType().equals(MessageType.DATA)){

			//check if message has a correct msgID 
			if (!isCorrectID(currMsgID)){
				throw new RuntimeException("Expected ID is not correct - Current PID: "+getProcessID()+ "Message: "+msg.getMsg());
			}

			//check if message hasn't been delivered yet 
			if (!pInfo.hasBeenDelivered(srcPid, currMsgID)){ //check if the current message from srcPiD has been delivered 
				pInfo.putInDelivered(srcPid, currMsgID);
				
				//before delivering it, must remove the id of the message in its content.
				int indxOfSep = msg.getMsg().indexOf(MSG_SEP);
				toDeliver = new Message(msg.getMsg().substring(indxOfSep+1)); 
				//deliver it to the upper service
				this.upService.deliver(srcPid, toDeliver);
				
				//prepare the ack message to send back to the src process
				ackMesg = new Message(ACK_MSG,currMsgID);
				ackMesg.setType(MessageType.ACK);

				pInfo.putMsgAckSent(srcPid, currMsgID, ackMesg);

				debug(TEST, "\t\t <== in PP2P/deliver P("+ getProcessID()+ ") Message delivered: " + msg.getMsg());
			}else{

				//get the ack message that has already been sent for the current srcPID
				ackMesg = pInfo.getAckSending_map().get(objPID).get(currMsgID);

				debug(TEST, "\t\t <== P("+ getProcessID()+ ") in PP2P/deliver - Sending Ack 2nd time: "+ ackMesg.getMsg());

			}

			//sending the actual ack message 
			this.downServ.send(srcPid, ackMesg);

			debug(TEST, "\t\t <== in PP2P/deliver P("+ getProcessID()+ ")  - Ack Sent: "+ ackMesg.getMsg());
		}

		//Message is an ACK 
		if(msg.getType().equals(MessageType.ACK)){

			if( pInfo.removeFromSent(srcPid, currMsgID) != null) {
				
				//prepare the re_ack message to send back to the process with srcPID
				ackMesg = new Message(RE_ACK_MSG,currMsgID);
				ackMesg.setType(MessageType.RE_ACK);

				this.downServ.send(srcPid, ackMesg); //sending a ReACk message

				debug(TEST, "\t\t <== P("+ getProcessID()+ ") in PP2P/deliver Test - Re-Ack message sent "+ ackMesg.getMsg());

			}else
					System.out.println("\t\t <== P("+ getProcessID()+ ") in PP2P/deliver :  Ack message received to late !!! - Dropped");
		}

		//Message is an ACK to an ACK		
		if(msg.getType().equals(MessageType.RE_ACK)){

			debug(TEST, "\t\t <== P("+ getProcessID()+ ") PP2P/deliver (ACKACK received) : "+msg.getMsg());
			if(!pInfo.hasBeenDelivered(srcPid, currMsgID)) //just in case
				throw new RuntimeException("Unpected Error !!! current Process P(" +objCurrentPID.intValue()+
				") Received a Re ACK for a message that has not been delivered! Problem !!! ");
			//remove this msg from the deliver set and from AckSending
			if(pInfo.removeFromAckSentSet(srcPid, currMsgID) != null ){
				pInfo.removeFromDelivered(srcPid, currMsgID);

			}else
				System.out.println("\t\t <== P("+ getProcessID()+ ") in PP2P/deliver :  Re- Ack message received to late !!! - Dropped");
			
		}

		pid_info_map.put(objCurrentPID, pInfo); 		
	}

	@Override
	public void send(int dstPid, Message msg) {

		debug(TEST,"==> P("+ getProcessID()+ ") PP2P/send : dstPID:"+ dstPid+ " Content: "+msg.getMsg());

		Integer objCurrentPID  = new Integer(getProcessID()); //current process 
		Integer objPID = new Integer(dstPid);

		ProcessInfo pCurrent = pid_info_map.get(objCurrentPID);

		int nxtMsgID =(int) pid_NxtMsgID_map.get(objPID); // get the next MsgID to use for destination process

		//set the message attributes
		Message msgToSend = new Message(msg.getMsg(),nxtMsgID);
		msgToSend.setType(MessageType.DATA);
		//update the next ID for future use
		nxtMsgID++;
		pid_NxtMsgID_map.put(objPID, nxtMsgID);

		pCurrent.putSentMessage(dstPid, msgToSend.getId(), msgToSend);

		pid_info_map.put(objCurrentPID, pCurrent);

		debug(TEST, "==> P("+ getProcessID()+ ") PP2P/send: Message To Send: "+ msg.getMsg());

		this.downServ.send(dstPid, msgToSend); //sending the message over the FLP2P link 

	}


	/**
	 * 
	 */
	private  void reTransmitPending() {

		Integer objCurrentPID = new Integer(getProcessID());
		ProcessInfo pInfo = pid_info_map.get(objCurrentPID);

		//Re send non Acked messages

		//Messages in Ack_Sending map
		Map<Integer,Message> msgCollectionAck = null;
		for (Integer objPID: pInfo.getAckSending_map().keySet()){

			msgCollectionAck = pInfo.getAckSending_map().get(objPID);

			//for all ack messages need to be resent to objPID process
			for (Integer mID: msgCollectionAck.keySet()){
				Message m = null;
				m = msgCollectionAck.get(mID);
				debug(TEST,"==> P("+ getProcessID()+ ") PP2P/retransmit(AckSending) : dstPID:"+ objPID.intValue()+ " Content: "+m.getMsg());
				this.downServ.send(objPID, m);
			}
		}

		//Messages in Sending map
		Map<Integer,Message> msgCollection = null;
		for (Integer objPID: pInfo.getSending_map().keySet()){

			msgCollection = pInfo.getSending_map().get(objPID);	


			//for all messages needed to be resent to objPID process 
			for (Integer mID: msgCollection.keySet()){
				Message m = null;

				m = msgCollection.get(mID);

				debug(TEST,"==> P("+ getProcessID()+ ") PP2P/retransmit(SendingMap) : dstPID:"+ objPID.intValue()+ " Content: "+m.getMsg());
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



	//Inner class for holding single process information

	class ProcessInfo {


		//Sending Management			
		private Map<Integer, Map<Integer,Message>> sending_map; // mapping PID->Map<MsgID,Message> PID is the dest PID to send message to.
		// for message that have been sent but for which not ack has been received yet
		//Delivering Management
		private Map<Integer, Map<Integer,Message>> ackSending_map;  // mapping PID->Map<MsgID,Message> PID is the dest PID to send message to.
		// for messages delivered but for which no ReAck has been received yet
		private Map<Integer,List<Integer>> delivered; //mapping PID->List<MessageID> 


		private ProcessInfo(){		
			this.sending_map = new HashMap<Integer, Map<Integer,Message>>();
			this.ackSending_map = new HashMap<Integer, Map<Integer,Message>>();
			this.delivered = new HashMap<Integer, List<Integer>>();

		}

		/**
		 * store, for the given pid, the message that has been sent to  
		 * 
		 * @param pid the pid of the process to which the message m has been sent
		 * @param msgId the id of the message
		 * @param m the message its self
		 */
		public void putSentMessage(int pid, int msgId, Message m){
			putMsgInMap(pid, msgId, m, false);

		}


		/**
		 * 
		 * stores, for a given pid, the ack message that has been sent and for which a re-ack is expected
		 * 
		 * @param pid to which the ack message has been sent
		 * @param msgId Id of the ack message
		 * @param m the ack message 
		 */
		public void putMsgAckSent(int pid, int msgId, Message m){
			putMsgInMap(pid, msgId, m, true);
		}

		private void putMsgInMap(int pid, int msgId, Message m, boolean forAck){

			Integer objPID = new Integer(pid);
			Integer objMID = new Integer(msgId);
			Map<Integer,Message> msgCollection = null;


			if(forAck)
				msgCollection = this.ackSending_map.get(objPID);
			else
				msgCollection = this.sending_map.get(objPID);

			if( msgCollection == null) //not yet created
				msgCollection = new HashMap<Integer, Message>();

			msgCollection.put(objMID,m);

			if(forAck)
				this.ackSending_map.put(objPID	,msgCollection);
			else
				this.sending_map.put(objPID	,msgCollection);
		}

		/**
		 *  For given pid, remove the message with given Id from the collection of message sent to this pid
		 * 
		 * @param pid the pid of the process to which message which given Id has been sent
		 * @param msgId the id of the message
		 * @return the removed message if exists
		 */
		public Message removeFromSent(int pid, int msgId){
			return removeFromMap(pid, msgId, false);
		}


		/**
		 *  For given pid, remove the message with given Id from the collection of ack messages sent to this pid
		 * 
		 * @param pid the pid of the process to which message which given Id has been sent
		 * @param msgId the id of the message
		 * @return the removed message, if exists
		 */
		public Message removeFromAckSentSet(int pid, int msgId){
			return removeFromMap(pid, msgId, true);
		}

		private Message removeFromMap(int pid, int msgId, boolean forAck){

			Integer objPID = new Integer(pid);
			Integer objMID = new Integer(msgId);
			Map<Integer,Message> msgCollection = null;
			Message msgToRet = null; 


			if(forAck){
				msgCollection = this.ackSending_map.get(objPID);
				if(msgCollection == null)
					throw new RuntimeException("Unexpected Error - Process: P("+getProcessID()+")"+
							"\n Attempt to remove from an empy Map(AckSending). PID: "+pid+" msgId: "+msgId);
			}else{
				msgCollection = this.sending_map.get(objPID);

				if(msgCollection == null)
					throw new RuntimeException("Unexpected Error - Process: P("+getProcessID()+")"+
							"\n Attempt to remove from an empy Map(Sending). PID: "+pid+" msgId: "+msgId);
			}

			//removing the msgId entry
			msgToRet = msgCollection.remove(objMID);

			if(forAck)
				this.ackSending_map.put(objPID,msgCollection);
			else
				this.sending_map.put(objPID,msgCollection);

			return msgToRet;

		}

		/**
		 * stores, for the give pid, the message that has been delivered
		 * 
		 * @param pid the id of the process from which comes the msg
		 * @param msgId the id of the message
		 */
		public void putInDelivered(int pid, int msgId){

			Integer objPID = new Integer(pid);
			Integer objMID = new Integer(msgId);
			List<Integer> msgList = null;

			msgList = this.delivered.get(objPID);

			if(msgList == null) //not yet created
				msgList = new LinkedList<Integer>();

			msgList.add(objMID);

			this.delivered.put(objPID,msgList);


		}
		/**
		 * removes, for the give pid, the message Id from the delivered set
		 * 
		 * @param pid the id of the process from which came the msg
		 * @param msgId the id of the msg
		 */
		public void removeFromDelivered(int pid, int msgId){

			Integer objPID = new Integer(pid);
			Integer objMID = new Integer(msgId);
			List<Integer> msgList = null;

			msgList = this.delivered.get(objPID);

			if(msgList == null) //not normal, shouldn't happen that the current msgList == null
				throw new RuntimeException("Unexpected Error - Process: P("+getProcessID()+")"+
						"\n Attempt to remove from an empy List(Delivered). PID: "+pid+" msgId: "+msgId);	

			msgList.remove(objMID);

			this.delivered.put(objPID,msgList);

		}

		/**
		 * 
		 * @param pid the process id from which comes the msg
		 * @param msgId the id of message 
		 * @return true if message with msgId has been delivered, false otherwise
		 */
		public boolean hasBeenDelivered(int pid, int msgId){

			Integer objPID = new Integer(pid);
			Integer objMID = new Integer(msgId);
			List<Integer> msgList = this.delivered.get(objPID);

			if(msgList == null ) //not yet created because no
				return false;

			return msgList.contains(objMID);

		}


		/**
		 * @return the sending_map
		 */
		public Map<Integer, Map<Integer, Message>> getSending_map() {
			return sending_map;
		}

		/**
		 * @return the ackSending_map
		 */
		public Map<Integer, Map<Integer, Message>> getAckSending_map() {
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

		if(this.currentPID == -1) //not yet updated
			this.currentPID = this.downServ.getProcessID();

		return this.currentPID;
	}
}
