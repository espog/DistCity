package channels;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TCPClient {

	ObjectInputStream Sinput;	// to read the socket
	ObjectOutputStream Soutput;	// towrite on the socket
	Socket socket;
	boolean isInitialized = false;
	
	TCPClient(String host, int port) {
		try {
			socket = new Socket(host, port);
		}
		catch(Exception e) {
			System.out.println("Error connectiong to server:" + e);
			e.printStackTrace();
			return;
		}
	
		
//		System.out.println("Connection accepted " +
//				socket.getInetAddress() + ":" +
//				socket.getPort());

		/* Creating both Data Stream */
		try
		{
			Sinput  = new ObjectInputStream(socket.getInputStream());
			Soutput = new ObjectOutputStream(socket.getOutputStream());
			isInitialized = true;
		}
		catch (IOException e) {
			System.out.println("Exception creating new Input/output Streams: " + e);
			return;
		}
	
	}

	/**
	 * close the client connection 
	 */
	public void close() {
		try{
			Sinput.close();
			Soutput.close();
		}
		catch(Exception e) {
			//TODO improve
			e.printStackTrace();
		}
	}

	/**
	 * send the given message to the server attached to this client 
	 */
	public void send(Message m) {
		
		if(!isInitialized)
				return;
		
		try {
			Soutput.writeObject(m.getMsg());
			Soutput.flush();
		}
		catch(IOException e) {
			System.out.println("Error writting to the socket: " + e);
			return;
		}
	}  
}
