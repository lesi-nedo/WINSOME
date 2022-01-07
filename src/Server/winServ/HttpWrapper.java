package winServ;

import java.nio.channels.SocketChannel;

import org.apache.http.HttpResponse;

public class HttpWrapper {
	/*
	 * Overview: it is a wrapper for the response and serves as the object that is being passed to the main thread to be register with the correct operation. 
	 */
	private HttpResponse resp;
	private int length;//the length of the response
	private boolean closed;//if the client requested to close the connection
	private int OP_TYPE;//the type of operation associated to the channel
	private SocketChannel client;
	
	public HttpWrapper(HttpResponse resp, int length, boolean closed) {
		this.closed=closed;
		this.resp=resp;
		this.length=length;
	}
	
	public HttpResponse getResp() {
		return this.resp;
	}
	
	
	public int getLength() {
		return this.length;
	}
	
	public boolean getStatus() {
		return this.closed;
	}
	
	public void set_upd_op_type(int type) {
		this.OP_TYPE=type;
	}
	

	public int get_op() {
		return this.OP_TYPE;
	}
	
	public void set_socket(SocketChannel cl) {
		this.client=cl;
	}
	
	public SocketChannel get_client() {
		return this.client;
	}
}
