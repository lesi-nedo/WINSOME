package serv_inter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import static java.util.Map.entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.ChunkedInputStream;
import org.apache.http.impl.io.ChunkedOutputStream;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.impl.io.ContentLengthOutputStream;
import org.apache.http.impl.io.DefaultHttpRequestWriter;
import org.apache.http.impl.io.DefaultHttpResponseParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.IdentityInputStream;
import org.apache.http.impl.io.IdentityOutputStream;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.impl.io.SessionOutputBufferImpl;
import org.apache.http.message.BasicHttpRequest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import notify_client.FollowersInterface;
import rec_fol.ReceiveUpdatesInterface;
import sign_in.Sign_In_Interface;
import sign_in.TooManyTagsException;
import sign_in.UsernameAlreadyExistsException;

public class InterWithServ {
	static final SimpleDateFormat FORMATTER =new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	private static final String FWOR_LIST="list";
	private static final String FWOR_SHOW="show";
	private static final String FWOR_WALLET="wallet";
	private static final String GET="GET";
	private static final String POST="POST";
	private static final String DELETE="DELETE";
	private static final String PUT="PUT";
	
	private static final String URI_LIST_USERS="/list_users";
	private static final String URI_LIST_FOLLOWING="/list_following";
	private static final String URI_VIEW_BLOG="/view_blog";
	private static final String URI_SHOW_FEED="/show_feed";
	private static final String URI_WALLET="/get_wallet";
	private static final String URI_WALLET_IN_BTC="/get_wallet_in_bitcoin";
	private static final String URI_SHOW_POST="/show_post";
	private static final String URI_LOGIN="/login";
	private static final String URI_CREATE_POST="/create_post";
	private static final String URI_LOGOUT="/logout";
	private static final String URI_DELETE_POST="/delete_post";
	private static final String URI_FOLLOW_USER="/follow_user";
	private static final String URI_UNFOLLOW_USER="/unfollow_user";
	private static final String URI_REWIN_POST="/rewin_post";
	private static final String URI_RATE_POST="/rate_post";
	private static final String URI_ADD_COMMENT="/add_comment";
	private static final int BUFF_SIZE=8*1024*1024;
	
	//Each functionality is stored in a lambda fuction
	private static final DFunction<InterWithServ, String> list_users = (Obj, arg) -> Obj.list_users(arg);
	private static final DFunction<InterWithServ, String> list_following = (Obj, arg) -> Obj.list_following(arg);
	private static final DFunction<InterWithServ, String> view_blog = (Obj, arg) -> Obj.view_blog(arg);
	private static final DFunction<InterWithServ, String> show_feed = (Obj, arg) -> Obj.show_feed(arg);
	private static final DFunction<InterWithServ, String> get_wallet = (Obj, arg) -> Obj.get_wallet(arg);
	private static final DFunction<InterWithServ, String> get_wallet_in_bitcoin = (Obj, arg) -> Obj.get_wallet_in_bitcoin(arg);
	private static final DFunction<InterWithServ, String> show_post = (Obj, arg) -> Obj.show_post(arg);
	

	
	private static final DFunction<InterWithServ, String> login = (Obj, arg) -> Obj.login(arg);
	private static final DFunction<InterWithServ, String> create_post = (Obj, arg) -> Obj.create_post(arg);
		
		
	private static final DFunction<InterWithServ, String> logout = (Obj, arg) -> Obj.logout(arg);
	private static final DFunction<InterWithServ, String> delete_post = (Obj, arg) -> Obj.delete_post(arg);
		
	private static final DFunction<InterWithServ, String> follow_user = (Obj, arg) -> Obj.follow_user(arg);
	private static final DFunction<InterWithServ, String> unfollow_user = (Obj, arg) -> Obj.unfollow_user(arg);
	private static final DFunction<InterWithServ, String> rewin_post = (Obj, arg) -> Obj.rewin_post(arg);
	private static final DFunction<InterWithServ, String> rate_post = (Obj, arg) -> Obj.rate_post(arg);
	private static final DFunction<InterWithServ, String> add_comment = (Obj, arg) -> Obj.add_comment(arg);
	private static final DFunction<InterWithServ, String> register = (Obj, arg) -> Obj.register(arg);

	
	private static final Map<String, DFunction<InterWithServ, String>> LIST_OP = Map.of("users", list_users, "following", list_following);
	private static final Map<String, DFunction<InterWithServ, String>> SHOW_OP = Map.of("feed", show_feed, "post", show_post);
	private static final Map<String, DFunction<InterWithServ, String>> WALLET_OP = Map.of("btc", get_wallet_in_bitcoin);
	private static final Map<String, DFunction<InterWithServ, String>> ONEWORD_OP = Map.ofEntries(entry("blog", view_blog), entry("wallet", get_wallet), entry("login", login), entry("post", create_post), 
			entry("logout", logout), entry("delete", delete_post), entry("follow", follow_user), entry("unfollow", unfollow_user), entry("rewin", rewin_post), entry("rate", rate_post), entry("comment", add_comment), entry("register", register));
		
	private static final Map<String, Map<String, DFunction<InterWithServ, String>>> TWOWORD_OP= Map.of("list", LIST_OP, "show", SHOW_OP, "wallet", WALLET_OP);
	
	private Sign_In_Interface sign;
	private SessionInputBufferImpl ses_in;
	private HttpTransportMetricsImpl metrs;
	private String cookies = null;
	private SocketChannel cl_skt;
	private HttpTransportMetricsImpl metrics;
	private SessionOutputBufferImpl ses_out;
	private String IP = null;
	private AtomicBoolean exit;
	private boolean logged;
	private String username=null;
	private int mcast_port;
	private InetAddress mcast_group;
	private Thread thread;
	private FollowersInterface serv_service;
	private ReceiveUpdatesInterface stub;
	
	public InterWithServ (Sign_In_Interface sign, SocketChannel cl_skt, String IP, AtomicBoolean exit, FollowersInterface serv_service, ReceiveUpdatesInterface stub) {
		this.sign = sign;
		this.metrs=new HttpTransportMetricsImpl();
		this.ses_in=new SessionInputBufferImpl(metrs, BUFF_SIZE);
		this.ses_out = new SessionOutputBufferImpl(metrics, BUFF_SIZE);
		this.cl_skt=cl_skt;
		this.IP=IP;
		this.exit=exit;
		this.logged=false;
		this.thread=null;
		this.serv_service=serv_service;
		this.stub=stub;
	}
		
	public static void send_req(InterWithServ instance, String req) throws IncorrectOperationException {
		String meth = req.substring(0, req.indexOf(" "));
		String args = req.substring(meth.length()+1);
		String sec_wor=null;
		try {
			if(meth.startsWith(FWOR_LIST) || meth.startsWith(FWOR_SHOW) || 
					(meth.startsWith(FWOR_WALLET) && args.length() >= 1)) {
				sec_wor=args.substring(0, args.indexOf(' '));
				args = args.substring(sec_wor.length()+1);
				TWOWORD_OP.get(meth).get(sec_wor).apply(instance, args);
			} else {
				ONEWORD_OP.get(meth).apply(instance, args);
			}
		} catch (NullPointerException e) {
			throw new IncorrectOperationException("Such a opertaion, " + meth +", is not implemented.");
		}
	}
	
	private void register(String args) {
		try {
			StringTokenizer toks = new StringTokenizer(args, " ");
			String username = toks.nextToken();
			String password = toks.nextToken();
			String tags = toks.nextToken();
			sign.register(username, password, tags);
			
		} catch (NoSuchElementException e) {
			this.exit.set(true);
			System.out.println("Incorrect input");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsernameAlreadyExistsException e) {
			this.exit.set(true);
			System.out.println("Username already exists.");
		} catch (TooManyTagsException e) {
			this.exit.set(true);
			System.out.println("Incorrect input");
		}
	}
	
	private void list_users(String args) {
		HttpRequest req = create_req(GET, URI_LIST_USERS);
		writer_skt(req, null);
		String res = reader_skt();
		if(res != null) {
			try {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				String tags =null;
				System.out.println(String.format("%-5s|%20s", "Users", "Tags"));
				Stream.generate(() -> "-").limit(50).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					String user = par.getText();
					par.nextToken();
					while(par.nextToken() != JsonToken.END_ARRAY) {
						String tag = par.getText();
						tags = tags.concat(", " + tag);
					}
					tags.replaceFirst(", ", "");
					System.out.println(String.format("%-5s|%20s", user, tags));
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return;
		
	}
	
	private void list_following(String args) {
		HttpRequest req = create_req(GET, URI_LIST_FOLLOWING);
		writer_skt(req, null);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				System.out.println("Following:");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_ARRAY) {
					String user = par.getText();
					System.out.println(user);
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void view_blog(String args) {
		HttpRequest req = create_req(GET, URI_VIEW_BLOG);
		writer_skt(req, null);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				String title =null;
				String author = null;
				System.out.println(String.format("%-15s|%20s|%40s", "Id", "Author", "Title"));
				Stream.generate(() -> "-").limit(70).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					String id_post = par.getText();
					par.nextToken();
					while(par.nextToken() != JsonToken.END_OBJECT) {
						String tag = par.getText();
						if(tag.equals("author")) {
							author = par.nextTextValue();
						} else {
							title = par.nextTextValue();
						}
					}
					System.out.println(String.format("%-15s|%20s|%40s", id_post, author, title));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void show_feed(String args) {
		HttpRequest req = create_req(GET, URI_SHOW_FEED);
		writer_skt(req, null);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				String title =null;
				String author = null;
				System.out.println(String.format("%-15s|%20s|%40s", "Id", "Author", "Title"));
				Stream.generate(() -> "-").limit(70).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					String id_post = par.getText();
					par.nextToken();
					while(par.nextToken() != JsonToken.END_OBJECT) {
						String tag = par.getText();
						if(tag.equals("author")) {
							author = par.nextTextValue();
						} else {
							title = par.nextTextValue();
						}
					}
					System.out.println(String.format("%-15s|%20s|%40s", id_post, author, title));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void get_wallet(String args) {
		HttpRequest req = create_req(GET, URI_WALLET);
		writer_skt(req, null);
		String times = null;
		String value = null;
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				System.out.println(String.format("%-15s|%20s", "Value", "Timestamp"));
				Stream.generate(() -> "-").limit(45).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					value = par.getText();
					par.nextToken();
					times = par.getText();
					System.out.println(String.format("%-15s|%20s", value, times));
				}
				if(value == null)
					System.out.println(String.format("%-15s|%20s", "0", "------"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void get_wallet_in_bitcoin(String args) {
		HttpRequest req = create_req(GET, URI_WALLET_IN_BTC);
		writer_skt(req, null);
		String times = null;
		String value = null;
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				System.out.println(String.format("%-15s|%20s", "Value in bitcoin", "Timestamp"));
				Stream.generate(() -> "-").limit(45).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					value = par.getText();
					par.nextToken();
					times = par.getText();
					System.out.println(String.format("%-15s|%20s", value, times));
				}
				if(value == null)
					System.out.println(String.format("%-15s|%20s", "0", "------"));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void show_post(String args) {
		HttpRequest req = create_req(GET, URI_SHOW_POST);
		writer_skt(req, null);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				par.nextToken();
				par.nextToken();
				System.out.print(par.getText() +": ");
				par.nextToken();
				System.out.println(par.getText()+".");
				par.nextToken();
				System.out.print(par.getText() +": ");
				par.nextToken();
				System.out.println(par.getText()+".");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					String tok = par.getText();
					System.out.print(tok +": ");
					par.nextToken();
					tok = par.getText();
					System.out.println(tok + ".");
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void login(String args) {
		StringTokenizer toks = new StringTokenizer(args, " ");
		String res = null;
		Pattern p = Pattern.compile("(?<=:)(\\d+| \"\\d+.\\d+.\\d+.\\d+)(?!=\")");
		Matcher m = null;
		
		if(!this.logged) {
			try {
				HttpRequest req = create_req(POST, URI_LOGIN);
				String username = toks.nextToken();
				String password = toks.nextToken();
				BasicHttpEntity ent = new BasicHttpEntity();
				String json = "{\"username\":" + username + ", \"password\":\"" + password+ "\"}";
				ByteArrayInputStream stream = new ByteArrayInputStream(json.getBytes());
				ent.setContent(stream);
				req.setHeader("Content-Type", "application/json");
				req.setHeader("Content-Length", json.length()+"");
				writer_skt(req, ent);
				stream.close();
				res = reader_skt();
				 if(res != null) {
					 this.username=username;
					 this.serv_service.registerMe(username, this.stub);
					 this.logged=true;
					 System.out.println(username + " logged in");
					 m = p.matcher(res);
					 m.find();
					 this.mcast_port = Integer.valueOf(m.group(1));
					 m.find();
					 this.mcast_group = InetAddress.getByName(m.group(1).replaceFirst(" \"", ""));
					 this.thread = new Thread(new ReaderNotifCalc(this.mcast_port, this.mcast_group));
					 this.thread.run();
				 }
			} catch (NoSuchElementException e) {
				this.exit.set(true);
				System.out.println("Usage: login <username> <password>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			this.exit.set(true);
			System.err.println("There is a connected user, firstly log out.");
		}
	}
	
	private void logout(String args) {
		HttpRequest req = create_req(DELETE, URI_LOGOUT);
		String res = null;
		writer_skt(req,null);
		this.logged=false;
		res = reader_skt();
		if(res != null) {
		 System.out.println(this.username + " logged out");
	 }
	}
	
	private void follow_user(String args) {
		HttpRequest req = create_req(PUT, URI_FOLLOW_USER+"/"+args);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null);
		String res = reader_skt();
		Matcher m = p.matcher(res);
		m.find();
		System.out.println(m.group(2)+".");
	}
	
	private void unfollow_user(String args) {
		HttpRequest req = create_req(PUT, URI_UNFOLLOW_USER+"/"+args);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null);
		String res = reader_skt();
		Matcher m = p.matcher(res);
		m.find();
		System.out.println(m.group(2)+".");
	}
	
	private void rewin_post(String args) {
		HttpRequest req = create_req(PUT, URI_REWIN_POST+"/"+args);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null);
		String res = reader_skt();
		Matcher m = p.matcher(res);
		m.find();
		System.out.println(m.group(2)+".");
	}
	
	private void rate_post(String args) {
		StringTokenizer toks = new StringTokenizer(args, " ");
		try {
			String id_post = toks.nextToken();
			String react = toks.nextToken();
			HttpRequest req = create_req(PUT, URI_RATE_POST+"/"+id_post+"/"+react);
			Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
			writer_skt(req, null);
			String res = reader_skt();
			Matcher m = p.matcher(res);
			m.find();
			System.out.println(m.group(2)+".");
		} catch (NoSuchElementException e) {
			this.exit.set(true);
			System.err.println("Usage: login <username> <password>");
		}
	}
	
	private void add_comment(String args) {
		StringTokenizer toks = new StringTokenizer(args, " ");
		if(!this.logged) {
			try {
				String id_post = toks.nextToken();
				String content = toks.nextToken();
				HttpRequest req = create_req(PUT, URI_ADD_COMMENT+"/" +id_post);
				BasicHttpEntity ent = new BasicHttpEntity();
				
				String json = "{\"content\":" + content + "\"}";
				ByteArrayInputStream stream = new ByteArrayInputStream(json.getBytes());
				ent.setContent(stream);
				req.setHeader("Content-Type", "application/json");
				req.setHeader("Content-Length", json.length()+"");
				Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
				writer_skt(req, null);
				String res = reader_skt();
				Matcher m = p.matcher(res);
				m.find();
				System.out.println(m.group(2)+".");
				stream.close();
			} catch (NoSuchElementException e) {
				this.exit.set(true);
				System.err.println("Usage: comment <idPost> <comment>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			this.exit.set(true);
			System.err.println("There is a connected user, firstly log out.");
		}
	}
	
	private void create_post(String args) {
		StringTokenizer toks = new StringTokenizer(args, " ");
		try {
			HttpRequest req = create_req(POST, URI_CREATE_POST);
			String title = toks.nextToken();
			String content = toks.nextToken();
			BasicHttpEntity ent = new BasicHttpEntity();
			String json = "{\"title\":" + title + ", \"content\":\"" + content+ "\"}";
			ByteArrayInputStream stream = new ByteArrayInputStream(json.getBytes());
			ent.setContent(stream);
			req.setHeader("Content-Type", "application/json");
			req.setHeader("Content-Length", json.length()+"");
			Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
			writer_skt(req, null);
			String res = reader_skt();
			Matcher m = p.matcher(res);
			m.find();
			System.out.println(m.group(2)+".");
			stream.close();
		} catch (NoSuchElementException e) {
			this.exit.set(true);
			System.out.println("Usage: post <title> <content>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void delete_post(String arg) {
		HttpRequest req= create_req(DELETE, URI_DELETE_POST +"/"+arg);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null);
		String res = reader_skt();
		Matcher m = p.matcher(res);
		m.find();
		System.out.println(m.group(2)+".");
	}
	
	
	
	private void writer_skt (HttpRequest req, HttpEntity ent) {
		int length = req.toString().length();
		try {
			if(ent != null)
				length =+ (int)ent.getContentLength();
			ByteArrayOutputStream out_stream=new ByteArrayOutputStream(length);
			this.ses_out.bind(out_stream);
			DefaultHttpRequestWriter wrt = new DefaultHttpRequestWriter(this.ses_out);
			wrt.write(req);
			StrictContentLengthStrategy con_len_stra =new StrictContentLengthStrategy();
			long len = con_len_stra.determineLength(req);
			OutputStream out_entity=null;
			if(len == ContentLengthStrategy.CHUNKED) {
				out_entity= new ChunkedOutputStream(2048, ses_out);
			} else if(len == ContentLengthStrategy.IDENTITY) {
				out_entity = new IdentityOutputStream(ses_out);
			} else {
				out_entity = new ContentLengthOutputStream(ses_out, len);
			}
			if(ent != null) {
				ent.writeTo(out_entity);
			}
			out_entity.close();
			ses_out.flush();
			ByteBuffer buf = ByteBuffer.wrap(out_stream.toByteArray());
			cl_skt.write(buf);
			cl_skt.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private String reader_skt() {
		Header[] cookie = null;
		Pattern r = Pattern.compile("(?<=\\s).*=.*;");
		Matcher m = null;
		byte[] bytes = null;
		try {
			this.ses_in.bind(this.cl_skt.socket().getInputStream());
			DefaultHttpResponseParser res = new DefaultHttpResponseParser(this.ses_in);
			HttpResponse res_par = res.parse();
			if((cookie = res_par.getHeaders("Set-Cookie")) != null) {
				for(Header h: cookie) {
					m = r.matcher(h.getValue());
					if(m.find()) {
						cookies =cookies.concat(m.group(1) + " ");
						System.out.println(cookies);
					}
				}
			}
			
			ContentLengthStrategy contentLengthStrategy =
	                StrictContentLengthStrategy.INSTANCE;
			long len = contentLengthStrategy.determineLength(res_par);
			InputStream contentStream = null;
			if (len == ContentLengthStrategy.CHUNKED) {
				contentStream = new ChunkedInputStream(this.ses_in);
			} else if (len == ContentLengthStrategy.IDENTITY) {
				contentStream = new IdentityInputStream(this.ses_in);
			} else {
				contentStream = new ContentLengthInputStream(this.ses_in, len);
			}
			BasicHttpEntity ent = new BasicHttpEntity();
			ent.setContent(contentStream);
			res_par.setEntity(ent);
			StatusLine status = res_par.getStatusLine();
			if(status.getStatusCode() >=400 || status.getStatusCode() == 204) {
				System.out.println(status.getStatusCode() + " " + new String(ent.getContent().readAllBytes()));
				if(this.thread != null) {
					this.thread.interrupt();
					this.thread.join();
				}
				this.exit.set(true);
				return null;
			}
			bytes=ent.getContent().readAllBytes();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new String(bytes);
	}
	
	private HttpRequest create_req(String method, String uri) {
		HttpRequest resp = new BasicHttpRequest(method, uri, new ProtocolVersion("HTTP", 1, 1));
		resp.addHeader("Date", FORMATTER.format(Calendar.getInstance().getTime()));
		resp.addHeader("Accept", "application/json");
		resp.addHeader("Accept-Language", "en-US");
		resp.addHeader("Connection", "Keep-Alive");
		resp.addHeader("Cache-Control", "max-age=60");
		resp.addHeader("Host", IP);
		if(this.cookies != null)
			resp.addHeader("Cookie", cookies);
		return resp;
	}
	
}











