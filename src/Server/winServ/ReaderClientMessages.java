package winServ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Calendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.IdentityInputStream;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.message.BasicHttpResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import rec_fol.ReceiveUpdatesInterface;
import sign_in.TooManyTagsException;
import winServ.WinsomeServer;

public class ReaderClientMessages implements Runnable {
	/*
	 * Overview: each thread executing this class will process the request from the client by calling static methods from class Operations.
	 * 
	 */
	private SelectionKey key;
	private ConcurrentMap<String, ReadWriteLock> usernames;//all names of the users in the system
	private ConcurrentMap<String, ReadWriteLock> tags_in_mem;// all tags specified by the users
	private ConcurrentMap<String, String> logged_users; //all users that are currently logged
	private int BUFF_SIZE; //the ByteBuffer size i.e the maximum bytes readable from the socket
	private ConcurrentMap<String, ReceiveUpdatesInterface> users_to_upd;
	private SessionInputBufferImpl ses_inp;
	private BlockingQueue<HttpWrapper> queue; // all keys that have been served will end up here 
	private Selector sel;
	private AtomicBoolean wake_called;
	private int mcast_port = 0;
	private InetAddress mcast_addr = null;
	
	public ReaderClientMessages(Selector sel, SelectionKey key, int BUFF_SIZE, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, ReadWriteLock> tags_in_mem, 
			ConcurrentMap<String, String> logged_users, ConcurrentMap<String, ReceiveUpdatesInterface> users_to_upd, BlockingQueue<HttpWrapper> queue, AtomicBoolean wake_called) {
		this.key=key;
		this.usernames=usernames;
		this.tags_in_mem=tags_in_mem;
		this.logged_users=logged_users;
		this.BUFF_SIZE = BUFF_SIZE;
		this.users_to_upd=users_to_upd;
		this.queue=queue;
		this.sel=sel;
		this.wake_called=wake_called;
	}
	@Override
	public void run() {
		SocketChannel c_sk=(SocketChannel) key.channel();
		int entity_len =0;
		
		
		if(key.attachment() == null) {
			ByteBuffer bfs = ByteBuffer.allocate(BUFF_SIZE);
			int num_b=0;
			try {
				num_b = c_sk.read(bfs);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				try {
					key.channel().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Client closed the connection");
				return;

			}
			if(num_b == -1) {
				try {
					key.channel().close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("Client closed the connection");
				return;
			}
			bfs.flip();
			ByteArrayInputStream in_str= new ByteArrayInputStream(bfs.array());
			HttpTransportMetricsImpl metrics= new HttpTransportMetricsImpl();
			ses_inp = new SessionInputBufferImpl(metrics, BUFF_SIZE);
			ses_inp.bind(new ByteArrayInputStream(bfs.array()));
			DefaultHttpRequestParser req_par = new DefaultHttpRequestParser(ses_inp);
			HttpResponse resp = null;
			HttpRequest req = null;
			HttpWrapper resp_wrp=null;
			Header conn_header=null;
			boolean closed = false;
			try {
				req = req_par.parse();
				System.out.println(new String(bfs.array()));
				conn_header=req.getFirstHeader("Connection");
				if(conn_header != null && conn_header.getValue().toLowerCase().equals("close"))
					closed = true;
				RequestLine f_head= req.getRequestLine();
				String uri = f_head.getUri();
				String method = f_head.getMethod();
				StringTokenizer t = new StringTokenizer(uri, "/");
				String op = t.nextToken();
				resp = WinsomeServer.METHODS_OP.get(method).get(op).apply(this, req);
				System.out.println("SSS");
				HttpEntity entity = resp.getEntity();
				entity_len = entity != null ?entity.toString().getBytes().length: 0;
				resp_wrp=new HttpWrapper(resp, resp.toString().getBytes().length + entity_len, closed);
			} catch (IOException | NoSuchElementException | NullPointerException | HttpException e) {
				// TODO Auto-generated catch block
				resp = create_resp(400, "Bad Request");
				resp_wrp=new HttpWrapper(resp, (int) (resp.toString().length() + resp.getEntity().getContentLength()), closed);
			} finally {
				try {
					in_str.close();
					System.out.println(resp_wrp);
					resp_wrp.set_upd_op_type(SelectionKey.OP_WRITE);
					resp_wrp.set_socket(c_sk);
					this.queue.put(resp_wrp);
					if(this.wake_called.compareAndSet(false, true)) {
						this.sel.wakeup();
					}
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	//@Effects: Updates the port and InetAddress of the multicast
	//@param port: the port number
	public void set_mcast_port_addr(int port, InetAddress addr) {
		this.mcast_port=port;
		this.mcast_addr=addr;
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
	
	//@Effects: gets the username and session_id if the user is logged than post is loaded on the user's feed
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse rewin_post(HttpRequest req) {
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
				Result res = Operations.rewin_post(username, id_post, usernames);
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
	
	//@Effects: gets the username and session_id if the user is logged then unfollows user y
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse unfollow_user(HttpRequest req) {
		String username=null;
		String unf_user =null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		
		RequestLine f_head= req.getRequestLine();
		String uri = f_head.getUri();
		StringTokenizer t = new StringTokenizer(uri, "/");
		
		
		if((username = is_logged(req)) != null) {
			try {
				t.nextToken();
				unf_user=t.nextToken();
				Result res = Operations.unfollow_user(username, unf_user, usernames, this.users_to_upd);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			}  catch (NoSuchElementException e) {
				return create_resp(400, "Bad Request");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Interanl Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	//@Effects: gets the username and session_id if the user is logged than follows user y
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse follow_user(HttpRequest req) {
		String username=null;
		String f_user =null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		
		RequestLine f_head= req.getRequestLine();
		String uri = f_head.getUri();
		StringTokenizer t = new StringTokenizer(uri, "/");
		
		
		if((username = is_logged(req)) != null) {
			try {
				t.nextToken();
				f_user=t.nextToken();
				Result res = Operations.follow_user(username, f_user, usernames, this.users_to_upd);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			}  catch (NoSuchElementException e) {
				return create_resp(400, "Bad Request");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(500, "Interanl Server Error");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	//@Effects: logs the user
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse login(HttpRequest req) {
		String username=null;
		String password = null;
		String json=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar = null;
		try {
			HttpEntityEnclosingRequest ereq = get_with_entity(req);
			json = new String(ereq.getEntity().getContent().readAllBytes());
			jsonPar = jsonFact.createParser(json);
			while(!jsonPar.isClosed()) {
				JsonToken tok = jsonPar.nextToken();
				
				if(JsonToken.FIELD_NAME.equals(tok)) {
					String f_name=jsonPar.getCurrentName();
					tok=jsonPar.nextToken();
					if("username".equals(f_name)) {
						username=jsonPar.getValueAsString();
					} else if("password".equals(f_name)) {
						password=jsonPar.getValueAsString();
					}
				}
			}
			if(password == null || username == null)
				return create_resp(400, "Bad Request");
			Result res = Operations.login(username, password, this.logged_users, this.usernames, this.users_to_upd, this.mcast_port, this.mcast_addr);
			entity.setContentType("application/json");
			entity.setContentLength(res.getReason().length());
			entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
			res_http=create_resp(res.getResult(), res.getResult_Str());
			res_http.setEntity(entity);
			res_http.addHeader("Set-Cookie", "username="+username+"; Max-Age=3600");
			res_http.addHeader("Set-Cookie", "session_id="+logged_users.get(username)+"; Max-Age=3600");

			return res_http;
		}  catch (NoSuchElementException e) {
			return create_resp(400, "Bad Request");
		} catch (IOException e) {
			// TODO Auto-generated catch blockWS
			e.printStackTrace();
			return create_resp(500, "Interanl Server Error");
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return create_resp(400, "Bad Request");
		}
	}
	
	//@Effects: logs the user
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse add_comment(HttpRequest req) {
		String id_post=null;
		String username =null;
		String content = null;
		String json=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar = null;
		HttpEntityEnclosingRequest ereq = null;
		
		RequestLine f_head= req.getRequestLine();
		String uri = f_head.getUri();
		StringTokenizer t = new StringTokenizer(uri, "/");
		if((username = is_logged(req)) != null) {
			try {
				t.nextToken();
				id_post = t.nextToken();
				ereq = get_with_entity(req);
				json = new String(ereq.getEntity().getContent().readAllBytes());
				jsonPar = jsonFact.createParser(json);
				while(!jsonPar.isClosed()) {
					JsonToken tok = jsonPar.nextToken();
					
					if(JsonToken.FIELD_NAME.equals(tok)) {
						String f_name=jsonPar.getCurrentName();
						tok=jsonPar.nextToken();
						if("content".equals(f_name)) {
							content=jsonPar.getValueAsString();
						}
					}
				}
				if(content == null)
					return create_resp(400, "Bad Request");
				Result res = Operations.add_comment(username, id_post, content, this.usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			}  catch (NoSuchElementException e) {
				return create_resp(400, "Bad Request");
			} catch (IOException e) {
				// TODO Auto-generated catch blockWS
				e.printStackTrace();
				return create_resp(500, "Interanl Server Error");
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(400, "Bad Request");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
	
	//@Effects: logs the user
	//@Return: the response with appropriate code
	//@param req: the request from the client
	public HttpResponse create_post(HttpRequest req) {
		String title=null;
		String username =null;
		String content = null;
		String json=null;
		HttpResponse res_http = null;
		BasicHttpEntity entity = new BasicHttpEntity();
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar = null;
		HttpEntityEnclosingRequest ereq = null;
		
		if((username = is_logged(req)) != null) {
			try {
				ereq = get_with_entity(req);
				json = new String(ereq.getEntity().getContent().readAllBytes());
				System.out.println(json);
				jsonPar = jsonFact.createParser(json);
				while(!jsonPar.isClosed()) {
					JsonToken tok = jsonPar.nextToken();
					
					if(JsonToken.FIELD_NAME.equals(tok)) {
						String f_name=jsonPar.getCurrentName();
						tok=jsonPar.nextToken();
						if("content".equals(f_name)) {
							content=jsonPar.getValueAsString();
						} else if("title".equals(f_name)) {
							title=jsonPar.getValueAsString();
						}
					}
				}
				if(content == null || title == null)
					return create_resp(400, "Bad Request");
				Result res = Operations.create_post(username, title, content, this.usernames);
				entity.setContentType("application/json");
				entity.setContentLength(res.getReason().length());
				entity.setContent(new ByteArrayInputStream(res.getReason().getBytes()));
				res_http=create_resp(res.getResult(), res.getResult_Str());
				res_http.setEntity(entity);
				return res_http;
			}  catch (NoSuchElementException e) {
				return create_resp(400, "Bad Request");
			} catch (IOException e) {
				// TODO Auto-generated catch blockWS
				e.printStackTrace();
				return create_resp(500, "Interanl Server Error");
			} catch (HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return create_resp(400, "Bad Request");
			}
		} else {
			return create_resp(401, "Unauthorized");
		}
	}
		
	
	private String is_logged(HttpRequest req) {
		try {
			String username  = null;
			String session_id=null;
			
			String cookie_val = req.getFirstHeader("Cookie").getValue();
			StringTokenizer toks = new StringTokenizer(cookie_val, "; ");
			while(toks.hasMoreTokens()) {
				String curr_val = toks.nextToken();
				if(curr_val.startsWith("username")) {
					username=curr_val.substring(curr_val.indexOf('=')+1).trim();
				} else if(curr_val.startsWith("session_id"))
					session_id =curr_val.substring(curr_val.indexOf('=')+1).trim();
			}
			if(username == null || session_id == null)
				return null;
			if(this.logged_users.get(username).equals(session_id)) {
				return username;
			}
		} catch (ParseException e) {
				return null;
		}
		return null;
	}
	private HttpResponse create_resp(int code, String reason_ph) {
		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), code , reason_ph);
		BasicHttpEntity entity = new BasicHttpEntity();
		String con = "{\"reason\":" + "\"" +reason_ph +"\"}";
		resp.addHeader("Date", WinsomeServer.FORMATTER.format(Calendar.getInstance().getTime()));
		resp.addHeader("Access-Control-Allow-Origin", "*");
		resp.addHeader("Content-Language", "en-US");
		resp.addHeader("Connection", "Keep-Alive");
		resp.addHeader("Last-Modified", WinsomeServer.FORMATTER.format(Calendar.getInstance().getTime()));
		resp.addHeader("Server", "WINSOME");
		resp.addHeader("Cache-Control", "max-age=60");
		entity.setContentType("application/json");
		entity.setContentLength(con.length());
		entity.setContent(new ByteArrayInputStream(con.getBytes()));
		resp.setEntity(entity);
		return resp;
	}
	
	private HttpEntityEnclosingRequest get_with_entity(HttpRequest req) throws HttpException {
		 HttpEntityEnclosingRequest ereq = (HttpEntityEnclosingRequest) req;
	        ContentLengthStrategy contentLengthStrategy =
	                    StrictContentLengthStrategy.INSTANCE;
	        long len = contentLengthStrategy.determineLength(req);
	        InputStream contentStream = null;
	        if (len == ContentLengthStrategy.CHUNKED) {
	            contentStream = new ChunkedInputStream(this.ses_inp);
	        } else if (len == ContentLengthStrategy.IDENTITY) {
	            contentStream = new IdentityInputStream(this.ses_inp);
	        } else {
	            contentStream = new ContentLengthInputStream(this.ses_inp, len);
	        }
	        BasicHttpEntity ent = new BasicHttpEntity();
	        ent.setContent(contentStream);
	        ereq.setEntity(ent);
	        return ereq;
	}
}
