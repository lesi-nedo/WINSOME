package winServ;


public class ShutTheServ extends Thread {
	private WinsomeServer serv;
	public ShutTheServ(WinsomeServer serv) {
		this.serv=serv;
	}
	
	@Override 
	public void run () {
		this.serv.end_me();
	}
}
