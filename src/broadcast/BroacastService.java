package broadcast;

import channels.Message;

public interface BroacastService {
	
	void deliver(int srcPID, Message msg);
	void send(Message msg);

}
