package channels;

public class LocalProcess  implements Runnable {

	
		private int pid;
		private TCPServer serv;
		/**
		 * @param pid
		 */
		public LocalProcess(int pid) {
			this.pid = pid;
			
		}
		
		/**
		 * @return the pid
		 */
		public int getPid() {
			return pid;
		}
		/**
		 * @param pid the pid to set
		 */
		public void setPid(int pid) {
			this.pid = pid;
		}
		/**
		 * @return the serv
		 */
		public TCPServer getServ() {
			return serv;
		}
		/**
		 * @param serv the serv to set
		 */
		public void setServ(TCPServer serv) {
			this.serv = serv;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			this.serv.run();
		}
		
		
		
		
}
