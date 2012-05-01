package channels;

public class Message {
	
	private String msg;
	private int id;
	private MessageType type;

	/**
	 * @param msg 
	 */ 
	public Message(String msg) {
		this.msg = msg;
		this.id = 0;
	}

	/**
	 * @param msg
	 * @param id
	 */
	public Message(String msg, int id) {
		this.msg = msg;
		this.id = id;
	}
	
	/**
	 * @return the msg
	 */
	public String getMsg() {
		if(id != 0) // the id is being used
				return ""+id+":"+this.msg;
		
		return msg;
	}

	/**
	 * @param msg the msg to set
	 */
	public void setMsg(String msg) {
		this.msg = msg;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the type
	 */
	public MessageType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(MessageType type) {
		this.type = type;
	}
	
		
}
