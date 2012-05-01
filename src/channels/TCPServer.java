package channels;
 
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import tests.Debug;


public class TCPServer  implements Debug{

	private final char msgSep=':';
	
	// the socket used by the server
	private ServerSocket serverSocket;
	private P2PService upService;
	
	private static boolean TEST =true;

	 
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
//			System.out.println("Server waiting for client on port " + serverSocket.getLocalPort() + "PID:"+
//																((BaseService)this.upService).getProcess().getPid());
			
			
			while(true) 
			{
				Socket socket = serverSocket.accept();  // accept connection

//				System.out.println("New client asked for a connection");
				if(TEST){
					if (this.upService == null){
							debug(TEST,"upService is null !!!");
					}
				}
				TcpThread t = new TcpThread(socket);    // make a thread of it
//				System.out.println("Starting a thread for a new Client");
				t.start();
			}
		}
		catch (IOException e) {
			System.out.println("Exception on new ServerSocket: " + e);
		}
		
	}
	
	
	/**
	 * @return the port
	 */
	public int getPort() {
		return this.serverSocket.getLocalPort();
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
		BufferedReader reader;
		boolean busy = false;
		
		TcpThread(Socket socket) {
			this.socket = socket;
			
			
			
//			System.out.println("Thread trying to create Object Input/Output Streams");
			try
			{
				reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				busy = true;

			}
			catch (IOException e) {
				System.out.println("Exception while init TCP thread: " + e);
				e.printStackTrace();
				return;
			}
		}
		public void run() {
			
			int srcPID;
			
	
//			System.out.println("Thread waiting for a String from the Client");
			try {
				
				while(busy){
			
					String rawMsg = reader.readLine();					
				
					debug(TEST,"\t\t\t <==  TCPServer - received: message : "+rawMsg);
					
					
					if (rawMsg.equalsIgnoreCase("stop")) // what if client crashes ? if I/O exception -> thread will stop
												busy = false;		
			
					int indxSep = rawMsg.indexOf(msgSep);
					//Get the sending process ID
					srcPID = new Integer(rawMsg.substring(0,indxSep));
					//Get the rest of the message
					Message  m = new Message(rawMsg.substring(indxSep+1));
					//Calling the upper service
					
				
					debug(TEST,"\t\t\t <==  TCPServer : Message to deliver: "+ m.getMsg());
					
					upService.deliver(srcPID, m);
					
				}

			}
			catch (IOException e) {
				System.out.println("Exception reading stream from client: " + e);
				//TODO improve exception handling
				return;				
			}
			catch (Exception e ){
				//TODO improve exception handling
				e.printStackTrace();
			}
			
			finally {
				try {
					reader.close();
				}
				catch (Exception e) {	
					//TODO improve exception handling
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void debug(boolean on, String msg) {
		if(on)
				System.out.println(msg);
		
	}




}
