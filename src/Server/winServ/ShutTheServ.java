package winServ;


public class ShutTheServ extends Thread {
	/*
	 * Overview: serves to shut down the server in a gentle way
	 */
	private WinsomeServer serv;
	public ShutTheServ(WinsomeServer serv) {
		this.serv=serv;
	}
	
	@Override 
	public void run () {
		this.serv.end_me();
	}
}
