package tests;

import broadcast.BroacastService;
import channels.BaseService;
import channels.FLP2P;
import channels.LocalProcess;
import channels.Message;
import channels.P2PService;
import channels.PP2P;
import channels.TCPServer;

/*
 * Simulating the broadcast service 
 */
public class TestPP2P implements BroacastService {


	private P2PService down;
	private int currentPID = -1;
	private int nbDeliver = 0;

	/**
	 * @param down
	 */
	public TestPP2P(P2PService down) {
		this.down = down;
	}


	@Override
	public void deliver(int srcPID, Message msg) {
		
		System.out.println("== Simulating Broacast Layer === P("+ getProcessID()+ ") Content: "+msg.getMsg());
		nbDeliver++;
		if(nbDeliver > 1)
			System.err.println("!!!! Nb deliver :"+nbDeliver);
	
	}

	
	public void send(Message msg) {

		for (int i = 0; i < PP2P.NB_PROCESSES; i++) {
			if( getProcessID() != i+1)
				this.down.send(i+1, msg);
					
		}
	}
	public int getProcessID(){
		if(currentPID == -1 )
			currentPID = this.down.getProcessID();
		
		return currentPID;
	}
	
	public static void main(String[] args) {

		TCPServer serv = new TCPServer(1500);
		TCPServer serv2 = new TCPServer(1501);
		TCPServer serv3  = new TCPServer(1502);
//		TCPServer serv4  = new TCPServer(1503);


		LocalProcess p1 = new LocalProcess(1); //runnable 
		p1.setServ(serv);
		LocalProcess p2 = new LocalProcess(2); // runnable
		p2.setServ(serv2);
		LocalProcess p3 = new LocalProcess(3); // runnable
		p3.setServ(serv3);
//		LocalProcess p4 = new LocalProcess(4); // runnable
//		p3.setServ(serv4);
		
		//Register process pid<-->physical address (port number)
		BaseService.register(1,1500);
		BaseService.register(2, 1501);
		BaseService.register(3, 1502);

		BaseService bServ1 = new BaseService(p1);
		BaseService bServ2 = new BaseService(p2);
		BaseService bServ3 = new BaseService(p3);

		
		FLP2P flp2p1 = new FLP2P();
		flp2p1.setDownServ(bServ1);
		bServ1.setUpperServ(flp2p1);
		
		FLP2P flp2p2 = new FLP2P();
		flp2p2.setDownServ(bServ2);
		bServ2.setUpperServ(flp2p2);
		
		
		FLP2P flp2p3 = new FLP2P();
		flp2p3.setDownServ(bServ3);
		bServ3.setUpperServ(flp2p3);

		
		
		PP2P pp2p1 = new PP2P();
		pp2p1.setDownServ(flp2p1);
		flp2p1.setUpServ(pp2p1);
		
		PP2P pp2p2 = new PP2P();
		pp2p2.setDownServ(flp2p2);
		flp2p2.setUpServ(pp2p2);
	
		PP2P pp2p3 = new PP2P();
		pp2p3.setDownServ(flp2p3);
		flp2p3.setUpServ(pp2p3);
		
		
		
		//make the links
		Thread tp1 = new Thread(p1);
		Thread tp2 = new Thread(p2);
		Thread tp3 = new Thread(p3);

		tp1.start();
		tp2.start();
		tp3.start();
		

		TestPP2P bCast1 = new TestPP2P(pp2p1); //process 1 
		pp2p1.setUpServ(bCast1);
		TestPP2P bCast2 = new TestPP2P(pp2p2); //process 2
		pp2p2.setUpServ(bCast2);

		TestPP2P bCast3 = new TestPP2P(pp2p3); // process 3
		pp2p3.setUpServ(bCast3);

		
		//send to the others
		bCast3.send(new Message("15:20:1200"));

	}



}
