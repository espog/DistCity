package channels;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class TCPClient {


	public static final String DEFAULT_HOST = "localhost";
	Socket socket;
	boolean isInitialized = false;
	PrintWriter out = null;
	BufferedReader reader;

	TCPClient(String host, int port) {
		try {

			InetAddress addr = InetAddress.getByName(host);
			SocketAddress sockaddr = new InetSocketAddress(addr, port);
			socket = new Socket();
			socket.connect(sockaddr);
			out = new PrintWriter(socket.getOutputStream(), true);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			isInitialized = true;

		}
		catch(Exception e) {
			System.out.println("Error init Client :" + e);
			e.printStackTrace();
			return;
		}

		//		System.out.println("Connection accepted from server " + socket.getInetAddress() + ":" + socket.getPort());


	}

	/**
	 * close the client connection 
	 */
	public void close() {
		try{
			out.print("stop"+"\n"); //send stopping message to server side to close communication
			out.close();
			socket.close();
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
			throw new RuntimeException("TCP connection has not been initialized ! ");

		

		try {

			out.print(m.getMsg()+"\n");
			out.flush();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}  
}
