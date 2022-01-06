package serv_inter;

public class UsernameWrp {
	private String username;//the usrname of the current profile logged
	private Thread thread;//the thread that is listening for the datagram
	public UsernameWrp() {
		super();
	}
	
	public void set_username(String username) {
		this.username=username;
	}
	
	public String get_username() {
		return this.username;
	}
	
	public Thread get_thread() {
		return this.thread;
	}
	
	public void set_thread(Thread thread) {
		this.thread=thread;
	}
}
