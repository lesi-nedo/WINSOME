package winServ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.http.HeaderElement;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.message.BasicHttpResponse;

import sign_in.TooManyTagsException;
import winServ.WinsomeServer;

public class ReaderClientMessages implements Runnable {
	/*
	 * Overview: each thread executing this class will process the request from the client by calling static methods from class Operations.
	 * 
	 */
	private Selector sel;
	private SelectionKey key;
	private ConcurrentMap<String, ReadWriteLock> usernames;//all names of the users in the system
	private ConcurrentMap<String, ReadWriteLock> tags_in_mem;// all tags specified by the users
	private ConcurrentMap<String, String> logged_users; //all users that are currently logged
	private int BUFF_SIZE; //the ByteBuffer size i.e the maximum bytes readable from the socket
	
	
	public ReaderClientMessages(Selector sel, SelectionKey key, int BUFF_SIZE, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, ReadWriteLock> tags_in_mem, ConcurrentMap<String, String> logged_users) {
		this.sel=sel;
		this.key=key;
		this.usernames=usernames;
		this.tags_in_mem=tags_in_mem;
		this.logged_users=logged_users;
		this.BUFF_SIZE = BUFF_SIZE;
	}
	@Override
	public void run() {
		SocketChannel c_sk=(SocketChannel) key.channel();
		ByteBuffer bfs=(ByteBuffer) key.attachment();
		int num_b=0;
		try {
			num_b = c_sk.read(bfs);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			key.cancel();
			e1.printStackTrace();
			return;

		}
		if(num_b == -1) {
			key.cancel();
			return;
		}
		if(!bfs.hasRemaining()) {
			bfs.flip();
			HttpTransportMetricsImpl metrics= new HttpTransportMetricsImpl();
			SessionInputBufferImpl ses_inp = new SessionInputBufferImpl(metrics, BUFF_SIZE);
			ses_inp.bind(new ByteArrayInputStream(bfs.array()));
			DefaultHttpRequestParser req_par = new DefaultHttpRequestParser(ses_inp);
			try {
				HttpRequest req = req_par.parse();
				RequestLine f_head= req.getRequestLine();
				String uri = f_head.getUri();
				String method = f_head.getMethod();
				StringTokenizer t = new StringTokenizer(uri, "/");
			} catch (IOException | HttpException e) {
				// TODO Auto-generated catch block
				HttpResponse resp = create_resp(400, "Bad Request");
			
				try {
					c_sk.register(sel, SelectionKey.OP_WRITE, resp);
				} catch (ClosedChannelException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					
				}
				e.printStackTrace();
			}
		}
	}
	//@Effects: gets username ad session_id from cookie header checks if is correct than computes a sting in format json that holds all users with at least one tag in common
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse list_users(HttpRequest req) {
		String username=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		if((username = is_logged(req)) != null) {
			try {
				Result res = Operations.list_users(username, usernames, tags_in_mem);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			} catch (IOException | TooManyTagsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	//@Effects: gets the username and session_id if the user is logged than builds a json string with all following
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse list_following(HttpRequest req) {
		String username=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		if((username = is_logged(req)) != null) {
			try {
				Result res = Operations.list_following(username, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	//@Effects: gets the username and session_id if the user is logged than builds a json string with posts published by the user
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse view_blog(HttpRequest req) {
		String username=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		if((username = is_logged(req)) != null) {
			try {
				Result res = Operations.view_blog(username, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}

	//@Effects: gets the username and session_id if the user is logged than builds a json string with posts published/rewinded by the user
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse show_feed(HttpRequest req) {
		String username=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		if((username = is_logged(req)) != null) {
			try {
				Result res = Operations.show_feed(username, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	
	//@Effects: gets the username and session_id if the user is logged than builds a json string that represents the wallet
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse get_wallet(HttpRequest req) {
		String username=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		if((username = is_logged(req)) != null) {
			try {
				Result res = Operations.get_wallet(username, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}

	//@Effects: gets the username and session_id if the user is logged than builds a json string that represents the wallet in bitcoins
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse get_wallet_in_bitcoin(HttpRequest req) {
		String username=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		if((username = is_logged(req)) != null) {
			try {
				Result res = Operations.get_wallet_in_bitcoin(username, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	
	//@Effects: gets the username and session_id if the user is logged than builds a json string that represents a post 
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse show_post(HttpRequest req) {
		String username=null;
		String id_post;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		
		RequestLine f_head= req.getRequestLine();
		String uri = f_head.getUri();
		StringTokenizer t = new StringTokenizer(uri, "/");
		
		
		if((username = is_logged(req)) != null) {
			try {
				t.nextToken();
				id_post=t.nextToken();
				Result res = Operations.show_post(username, id_post, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			} catch (NoSuchElementException e) {
				return create_resp(400, "Bad Request");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	
	//@Effects: gets the username and session_id if the user is logged than than it logged out the user
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse logout(HttpRequest req) {
		String username=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		if((username = is_logged(req)) != null) {
			Result res = Operations.logout(username, this.logged_users);
			entity.setContentType("application/json");
			entity.setContentLength(res.getReason().length());
			entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
			res_http=create_resp(res.getResult(), res.getResult_Str());
			res_http.setEntity(entity);
			return res_http;
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	//@Effects: gets the username and session_id if the user is logged and the author of the post is 
	// the same user than deletes the post 
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse delete_post(HttpRequest req) {
		String username=null;
		String id_post;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		
		RequestLine f_head= req.getRequestLine();
		String uri = f_head.getUri();
		StringTokenizer t = new StringTokenizer(uri, "/");
		
		
		if((username = is_logged(req)) != null) {
			try {
				t.nextToken();
				id_post=t.nextToken();
				Result res = Operations.delete_post(username, id_post, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			}  catch (NoSuchElementException e) {
				return create_resp(400, "Bad Request");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	//@Effects: gets the username and session_id if the user is logged and  the post is 
	// in user's feed than the reaction accepted 
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse rate_post(HttpRequest req) {
		String username=null;
		String id_post;
		int reaction = 0;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		
		RequestLine f_head= req.getRequestLine();
		String uri = f_head.getUri();
		StringTokenizer t = new StringTokenizer(uri, "/");
		
		
		if((username = is_logged(req)) != null) {
			try {
				t.nextToken();
				id_post=t.nextToken();
				reaction = Integer.valueOf(t.nextToken());
				Result res = Operations.rate_post(username, id_post, reaction, usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			}  catch (NoSuchElementException | NumberFormatException e) {
				return create_resp(400, "Bad Request");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Internal Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	private String is_logged(HttpRequest req) {
		try {
			NameValuePair username  = null;
			NameValuePair session_id=null;
			
			HeaderElement[] cookie_val = req.getFirstHeader("Cookie").getElements();
			for(HeaderElement e: cookie_val) {
				username = e.getParameterByName("username");
				session_id =e.getParameterByName("session_id");
			}
			if(username == null || session_id == null)
				return null;
			if(this.logged_users.get(username.getValue()).equals(session_id.getValue())) {
				return username.getValue();
			}
		} catch (ParseException e) {
				return null;
		}
		return null;
	}
	private HttpResponse create_resp(int code, String reason_ph) {
		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), code , reason_ph);
		resp.addHeader("Date", WinsomeServer.FORMATTER.format(Calendar.getInstance().getTime()));
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Content-Language", "it-IT");
		resp.addHeader("Connection", "Keep-Alive");
		resp.addHeader("Last-Modified", WinsomeServer.FORMATTER.format(Calendar.getInstance().getTime()));
		resp.addHeader("Server", "WINSOME");
		resp.addHeader("Cache-Control", "max-age=60");
		return resp;
	}
}
