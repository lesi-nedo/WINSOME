package test;

import winServ.WinsomeServer;

public class ServRunnable implements Runnable {
	private WinsomeServer serv;
	private int timeout;
	public ServRunnable(WinsomeServer serv, int timeout) {
		this.serv=serv;
		this.timeout=timeout;
	}
	
	@Override 
	public void run() {
		serv.start_serv(timeout);
	}
}
