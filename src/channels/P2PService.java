package channels;

public interface P2PService {
	
	void deliver(int srcPid, Message msg);
	
	void send(int dstPid, Message msg); 

	int getProcessID();
	
}
