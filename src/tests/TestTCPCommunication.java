package tests;

import channels.BaseService;
import channels.LocalProcess;
import channels.Message;
import channels.PP2P;
import channels.TCPServer;


public class TestTCPCommunication {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
			
		TCPServer serv = new TCPServer(1500);
		TCPServer serv2 = new TCPServer(1501);
	
		LocalProcess p1 = new LocalProcess(1); //runnable 
		p1.setServ(serv);
		LocalProcess p2 = new LocalProcess(2); // runnable
		p2.setServ(serv2);
		
		//Register process pid<-->physical address (port number)
		BaseService.register(1,1500);
		BaseService.register(2, 1501);
			
		Thread tp1 = new Thread(p1);
		Thread tp2 = new Thread(p2);
		
		TestFLP2P flp1 = new TestFLP2P();
		TestFLP2P flp2 = new TestFLP2P();

		BaseService bserv1 = new BaseService(p1, flp1); 
		BaseService bserv2 = new BaseService(p2, flp2);
		
		flp1.setDown(bserv1);
		flp2.setDown(bserv2);

		tp1.start();
		tp2.start();
		
		flp1.send(2, new Message("hello world!"));
			
		
	}

}
