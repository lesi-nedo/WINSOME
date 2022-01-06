package serv_inter;

public class UsernameWrp {
	private String username;
	private Thread thread;
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
