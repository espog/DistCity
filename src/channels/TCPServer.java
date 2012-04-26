package channels;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class TCPServer {

	private final char msgSep=':';
	
	// the socket used by the server
	private ServerSocket serverSocket;
	private P2PService upService;
	 
	// server constructor
	public TCPServer(int port) {
		
		upService = null;
		try {
			serverSocket = new ServerSocket(port);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}		

	/**
	 * for running the server
	 */
	public void run(){
		
		/* create socket server and wait for connection requests */
		try 
		{
			System.out.println("Server waiting for client on port " + serverSocket.getLocalPort());

			while(true) 
			{
				Socket socket = serverSocket.accept();  // accept connection

				System.out.println("New client asked for a connection");
				TcpThread t = new TcpThread(socket, upService);    // make a thread of it
				System.out.println("Starting a thread for a new Client");
				t.start();
			}
		}
		catch (IOException e) {
			System.out.println("Exception on new ServerSocket: " + e);
		}
		
	}
	
	public void close(){
		
		try {
			this.serverSocket.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * @param listeningServ the listeningServ to set
	 */
	public void setListeningServ(P2PService serv) {
		this.upService = serv;
	}

	/** One instance of this thread will run for each client */
	class TcpThread extends Thread {
		// the socket where to listen/talk
		Socket socket;
		ObjectInputStream Sinput;
		ObjectOutputStream Soutput;
		P2PService service;

		TcpThread(Socket socket, P2PService serv) {
			this.socket = socket;
			this.service = serv;
		}
		public void run() {
			
			int srcPID;
			
			/* Creating both Data Stream */
			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				// create output first
				Soutput = new ObjectOutputStream(socket.getOutputStream());
				Soutput.flush();
				Sinput  = new ObjectInputStream(socket.getInputStream());
			}
			catch (IOException e) {
				System.out.println("Exception creating new Input/output Streams: " + e);
				return;
			}
			System.out.println("Thread waiting for a String from the Client");
			// read a String (which is an object)
			try {
			
				String rawMsg = (String) Sinput.readObject();
				int indxSep = rawMsg.indexOf(msgSep);
				
				//Get the sending process ID
				srcPID = new Integer(rawMsg.substring(0,indxSep));
				
				//Get the rest of the message
				Message  m = new Message(rawMsg.substring(indxSep+1));
				
				//Calling the upper service
				this.service.deliver(srcPID, m);
	
			
			}
			catch (IOException e) {
				System.out.println("Exception reading/writing  Streams: " + e);
				return;				
			}
			// will surely not happen with a String
			catch (ClassNotFoundException o) {
				//TODO improve exception handling
				o.printStackTrace();
			}
			
			catch (Exception e ){
				//TODO improve exception handling
				e.printStackTrace();
			}
			
			finally {
				try {
					Soutput.close();
					Sinput.close();
				}
				catch (Exception e) {					
				}
			}
		}
	}


}
