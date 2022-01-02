package winServ;

import org.apache.http.HttpResponse;

public class HttpWrapper {
	private HttpResponse resp;
	private int length;
	private boolean closed;
	
	
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
}
