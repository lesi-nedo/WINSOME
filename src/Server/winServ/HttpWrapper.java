package winServ;

import java.nio.channels.SocketChannel;

import org.apache.http.HttpResponse;

public class HttpWrapper {
	private HttpResponse resp;
	private int length;
	private boolean closed;
	private int OP_TYPE;
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
