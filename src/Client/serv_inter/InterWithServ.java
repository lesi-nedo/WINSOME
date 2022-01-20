package serv_inter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
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
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import notify_client.FollowersInterface;
import rec_fol.ReceiveUpdatesInterface;
import sign_in.Sign_In_Interface;
import sign_in.TooManyTagsException;
import sign_in.UsernameAlreadyExistsException;
import utils.StaticNames_Client;

public class InterWithServ {
	static final SimpleDateFormat FORMATTER =new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	private static final String FWOR_LIST="list";//commands that start with list
	private static final String FWOR_SHOW="show";//commands that start with show
	private static final String FWOR_WALLET="wallet";//commands that start with wallet
	//acceptable methods
	private static final String GET="GET";
	private static final String POST="POST";
	private static final String DELETE="DELETE";
	private static final String PUT="PUT";
	//acceptable uris
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
	private static final int BUFF_SIZE=8*1024*1024;//the size of the receiving buffer
	
	//Each functionality is stored in a lambda fuction
	private static final DFunction<InterWithServ, String> list_users = (Obj, arg) -> Obj.list_users(arg);
	private static final DFunction<InterWithServ, String> list_following = (Obj, arg) -> Obj.list_following(arg);
	private static final DFunction<InterWithServ, String> view_blog = (Obj, arg) -> Obj.view_blog(arg);
	private static final DFunction<InterWithServ, String> show_feed = (Obj, arg) -> Obj.show_feed(arg);
	private static final DFunction<InterWithServ, String> get_wallet = (Obj, arg) -> Obj.get_wallet(arg);
	private static final DFunction<InterWithServ, String> get_wallet_in_bitcoin = (Obj, arg) -> Obj.get_wallet_in_bitcoin(arg);
	private static final DFunction<InterWithServ, String> show_post = (Obj, arg) -> Obj.show_post(arg);
	private static final DFunction<InterWithServ, String> list_followers = (Obj, arg) -> Obj.list_followers(arg);
	private static final DFunction<InterWithServ, String> help = (Obj, arg) -> Obj.help(arg);
	
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

	
	private static final Map<String, DFunction<InterWithServ, String>> LIST_OP = Map.of("users", list_users, "following", list_following, "followers", list_followers);
	private static final Map<String, DFunction<InterWithServ, String>> SHOW_OP = Map.of("feed", show_feed, "post", show_post);
	private static final Map<String, DFunction<InterWithServ, String>> WALLET_OP = Map.of("btc", get_wallet_in_bitcoin);
	private static final Map<String, DFunction<InterWithServ, String>> ONEWORD_OP = Map.ofEntries(entry("blog", view_blog), entry("wallet", get_wallet), entry("login", login), entry("post", create_post), 
			entry("logout", logout), entry("delete", delete_post), entry("follow", follow_user), entry("unfollow", unfollow_user), entry("rewin", rewin_post), entry("rate", rate_post), entry("comment", add_comment), 
			entry("register", register), entry("help", help));
		
	private static final Map<String, Map<String, DFunction<InterWithServ, String>>> TWOWORD_OP= Map.of("list", LIST_OP, "show", SHOW_OP, "wallet", WALLET_OP);
	
	private Sign_In_Interface sign;
	private SessionInputBufferImpl ses_in;
	private HttpTransportMetricsImpl metrs;
	private String cookies = "";
	private SocketChannel cl_skt;
	private SessionOutputBufferImpl ses_out;
	private String IP = null;
	private boolean logged;
	private UsernameWrp username_wrp=null;
	private int mcast_port;
	private InetAddress mcast_group;
	private FollowersInterface serv_service;
	private ReceiveUpdatesInterface stub;
	private Set<String> all_followers;
	private int timeout;
	private ReaderNotifCalc mcast_not;
	private String NAME_NET_INTER;
	
	//@Effects: initializes of the object
	//@param sign: the interface of the sign up service
	//@param cl_skt: that allows communication with the server
	//@param IP: ip associated to the host
	//@param serv_service: the interface for registering for the callbacks about new follwers/unfollowers
	//@param stub: client's stub
	//@param username_wrp: wrapper for the username and thread that is waiting for the datagrams.
	//@param all_followers: the empty set where all followers will be saved
	//@param timeout: the timeout for the socket cl_Skt
	//@param NAME_NET_INTE: name of the network interface
	public InterWithServ (Sign_In_Interface sign, SocketChannel cl_skt, String IP, FollowersInterface serv_service, ReceiveUpdatesInterface stub, 
			UsernameWrp username_wrp, Set<String> all_followers, int timeout, String NAME_NET_INTER) {
		this.sign = sign;
		this.metrs=new HttpTransportMetricsImpl();
		this.ses_in=new SessionInputBufferImpl(this.metrs, BUFF_SIZE);
		this.ses_out = new SessionOutputBufferImpl(this.metrs, BUFF_SIZE);
		this.cl_skt=cl_skt;
		this.IP=IP;
		this.logged=false;
		this.serv_service=serv_service;
		this.stub=stub;
		this.username_wrp=username_wrp;
		this.all_followers=all_followers;
		this.timeout=timeout;
		this.NAME_NET_INTER=NAME_NET_INTER;
	}
	
	//@Effects: method that sends the request to the server
	//@Throw IncorrectOperationException: if a command is not in two maps (ONEWORD_OP, TWOWORD_OP)
	//@param instance: the instance of this
	//@param req: the command associated with a functionality
	public static void send_req(InterWithServ instance, String req) throws IncorrectOperationException {
		int ind_sec = req.indexOf(" ");
		String args = null;
		String meth = null;
		String sec_wor=null;
		if(ind_sec > 0) {
			//splits the string in command and arguments
			meth = req.substring(0, ind_sec);
			args  = req.substring(meth.length()+1);
		} else {
			meth=req;
		}
		try {
			if(meth.startsWith(FWOR_LIST) || meth.startsWith(FWOR_SHOW) || 
					(meth.startsWith(FWOR_WALLET) && args != null)) {
				//if a two words command the gets it the second part
				ind_sec = args.indexOf(' ');
				if(ind_sec == -1)
					ind_sec = args.length();
				sec_wor=args.substring(0, ind_sec);
				args = args.substring(ind_sec);
				TWOWORD_OP.get(meth).get(sec_wor).apply(instance, args);
			} else {
				ONEWORD_OP.get(meth).apply(instance, args);
			}
		} catch (NullPointerException e) {
			throw new IncorrectOperationException("Such a opertaion, " + meth +", is not implemented.");
		}
	}
	//@Effects: registers the user 
	//@param args: arguments <username> <password> <tags>
	private void register(String args) {
		try {
			Pattern p = Pattern.compile("(?<=\")(\\X.+?)(?=\")");
			Matcher m = null;
			StringTokenizer toks = new StringTokenizer(args, " ");
			String username = toks.nextToken();
			String password = toks.nextToken();
			m = p.matcher(args.substring(username.length()+password.length()+2));
			m.find();
			//extracts the tags from the string with a regex
			String tags = m.group(1);
			sign.register(username, password, tags);
			//creates a folder in Client that will stored all followed users
			File path = new File(StaticNames_Client.PATH_TO_CLIENT+username);
			File file_foll = new File(StaticNames_Client.PATH_TO_CLIENT+username+StaticNames_Client.NAME_FILE_FOLL);
			path.mkdir();
			file_foll.createNewFile();
			System.out.println("ok");
			System.out.flush();
		} catch (NoSuchElementException e) {
			System.err.println("Missing an argument");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			System.err.println(e.getMessage());
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UsernameAlreadyExistsException e) {
			System.err.println("Username already exists.");
		} catch (TooManyTagsException e) {
			System.err.println("Incorrect input");
		} catch (StringIndexOutOfBoundsException e) {
			System.err.println("Please provide the tags");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//@Effects:prints all the users that are followers of the profile logged
	//@param args: in this case should be empty
	private void list_followers(String args) {
		System.out.println("Followers:");
		for(String us: this.all_followers) {
			System.out.println("-" + us);
		}
		System.out.flush();
	}
	//@Effects: ask the server to list all users that have one tag in common with a profile that is logged in 
	//@param args: should be empty
	private void list_users(String args) {
		HttpRequest req = create_req(GET, URI_LIST_USERS);
		writer_skt(req, null, 0);
		String res = reader_skt();
		if(res != null) {
			try {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				String tags ="";
				System.out.println(String.format("%1$-20s|%2$30s", "Users", "Tags"));
				Stream.generate(() -> "-").limit(90).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					String user = par.getText();
					par.nextToken();
					while(par.nextToken() != JsonToken.END_ARRAY) {
						String tag = par.getText();
						tags = tags.concat(", " + tag);
					}
					tags = tags.replaceFirst(", ", "");
					System.out.println(String.format("%1$-20s|  %2$-50s", user, tags));
					tags="";
				}
				System.out.flush();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		return;
		
	}
	//@Effects: ask the server to list all users that are followed
	//@param args: should be empty
	private void list_following(String args) {
		HttpRequest req = create_req(GET, URI_LIST_FOLLOWING);
		writer_skt(req, null, 0);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				System.out.println("Following:");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_ARRAY) {
					String user = par.getText();
					System.out.println("-" + user);
				}
				System.out.flush();
			}
			System.out.flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	//@Effects: ask the server to list all published rewinded posts
	//@param args: should be empty
	private void view_blog(String args) {
		HttpRequest req = create_req(GET, URI_VIEW_BLOG);
		writer_skt(req, null, 0);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				String title =null;
				String author = null;
				System.out.println(String.format("%1$-15s|%2$-21s|%3$40s", "Id", "Author", "Title"));
				Stream.generate(() -> "-").limit(130).forEach(System.out::print);
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
					System.out.println(String.format("%1$-15s|%2$-21s|%3$40s", id_post, author, title));
				}
			}
			System.out.flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	//@Effects: ask the server to list all published-rewinded-follower's posts
	//@param args: should be empty
	private void show_feed(String args) {
		HttpRequest req = create_req(GET, URI_SHOW_FEED);
		writer_skt(req, null, 0);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				String title =null;
				String author = null;
				System.out.println(String.format("%1$-15s|%2$-20s|%3$-40s", "Id", "Author", "Title"));
				Stream.generate(() -> "-").limit(120).forEach(System.out::print);
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
					System.out.println(String.format("%1$-15s|%2$-20s|%3$-40s", id_post, author, title));
				}
			}
			System.out.flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	//@Effects: ask the server the value of the wallet
	//@param args: should be empty
	private void get_wallet(String args) {
		HttpRequest req = create_req(GET, URI_WALLET);
		writer_skt(req, null, 0);
		String times = null;
		String value = null;
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				System.out.println(String.format("%-15s|%35s", "Value", "Timestamp"));
				Stream.generate(() -> "-").limit(65).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				while(par.nextToken() != JsonToken.END_OBJECT) {
					value = par.getText();
					par.nextToken();
					times = par.getText();
					System.out.println(String.format("%-15s|%35s", value, times));
				}
				if(value == null)
					System.out.println(String.format("%-15s|%35s", "0", "------"));
			}
			System.out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	//@Effects: ask the server the value of the wallet in bitcoins
	//@param args: should be empty
	private void get_wallet_in_bitcoin(String args) {
		HttpRequest req = create_req(GET, URI_WALLET_IN_BTC);
		writer_skt(req, null, 0);
		String times = null;
		String value = null;
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				System.out.println(String.format("%1$-35s|%2$30s", "Value in bitcoin", "Timestamp"));
				Stream.generate(() -> "-").limit(65).forEach(System.out::print);
				System.out.println("");
				par.nextToken();
				value = par.getText();
				if(value == null) {
					System.out.println(String.format("%1$-35s|%2$30s", "0", "------"));
					return;
				}
				par.nextToken();
				par.nextToken();
				value = par.getText();
				par.nextToken();
				par.nextToken();
				times = par.getText();
				System.out.println(String.format("%1$-35s|%2$30s", value, times));
				
			}
			System.out.flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	//@Effects: asks the server the information about the post
	//@param args: the identifier of the post to be shown
	private void show_post(String args) {
		args = args.strip();
		HttpRequest req = create_req(GET, URI_SHOW_POST+"/"+args);
		writer_skt(req, null, 0);
		String res = reader_skt();
		try {
			if(res != null) {
				JsonFactory jsonFact = new JsonFactory();
				JsonParser par = jsonFact.createParser(res);
				par.nextToken();
				par.nextToken();
				System.out.print(par.getText() +": ");
				par.nextToken();
				System.out.println(par.getText());
				par.nextToken();
				System.out.print(par.getText() +": ");
				par.nextToken();
				System.out.println(par.getText());
				par.nextToken();
				System.out.print(par.getText()+": ");
				par.nextToken();
				System.out.println(par.getText());
				par.nextToken();
				System.out.print(par.getText()+": ");
				par.nextToken();
				System.out.println(par.getText());
				par.nextToken();
				System.out.print(par.getText()+": ");
				par.nextToken();
				System.out.print(par.getText()+" ");
				while(par.nextToken() != JsonToken.END_OBJECT) {
					par.nextToken();
					System.out.println("");
					System.out.print("    ");
					Stream.generate(() -> "-").limit(30).forEach(System.out::print);
					while(par.nextToken() != JsonToken.END_OBJECT) {
						String tok = par.getText();
						System.out.println("");
						System.out.print("    |" + tok +": ");
						par.nextToken();
						tok = par.getText();
						System.out.print(tok);
					}
					System.out.println("");
					System.out.print("    ");
					Stream.generate(() -> "-").limit(30).forEach(System.out::print);
				}
				System.out.println("");
				System.out.println("     " + par.getText());
				System.out.flush();
			}
			System.out.flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	//@Effects: asks the server to log in
	//@ param: should contain the username password
	private void login(String args) {
		StringTokenizer toks = new StringTokenizer(args, " ");
		String res = null;
		Pattern p = Pattern.compile("(?<=:)(\\d+|\"\\d+.\\d+.\\d+.\\d+)(?!=\")");
		Matcher m = null;
		String username = null;
		HttpRequest req = null;
		String password = null;
		JsonFactory jsonFact =null;
		File file_foll = null;
		JsonParser jsonPar = null;
		BasicHttpEntity ent = null;
		String json = null;
		ByteArrayInputStream stream = null;
		
		if(!this.logged) {
			try {
				req  = create_req(POST, URI_LOGIN);
				username = toks.nextToken();
				password = toks.nextToken();
				jsonFact = new JsonFactory();
				file_foll = new File(StaticNames_Client.PATH_TO_CLIENT+username+StaticNames_Client.NAME_FILE_FOLL);
				jsonPar = null;
				
				jsonPar = jsonFact.createParser(file_foll);
				JsonToken tok = jsonPar.nextToken();
				if(tok != null) {
					while(jsonPar.nextToken() != JsonToken.END_ARRAY) {
						String foll = jsonPar.getText();
						all_followers.add(foll);
					}
				}
				jsonPar.close();
				this.serv_service.registerMe(username, this.stub);//inserts for the callback
				ent = new BasicHttpEntity();
				json = "{\"username\":\"" + username + "\", \"password\":\"" + password+ "\"}";
				stream = new ByteArrayInputStream(json.getBytes());
				ent.setContent(stream);
				req.setHeader("Content-Type", "application/json");
				req.setHeader("Content-Length", json.length()+"");
				writer_skt(req, ent, json.length());
				stream.close();
				res = reader_skt();
				 if(res != null) {
					 this.username_wrp.set_username(username);
					 this.logged=true;
					 System.out.println(username + " logged in");
					 System.out.flush();
					 m = p.matcher(res);
					 m.find();
					 this.mcast_port = Integer.valueOf(m.group(1));
					 m.find();
					 this.mcast_group = InetAddress.getByName(m.group(1).replaceFirst("\"", ""));
					 this.mcast_not = new ReaderNotifCalc(this.mcast_port, this.mcast_group, timeout, this.NAME_NET_INTER);
					 this.username_wrp.set_thread(new Thread(mcast_not));
					 this.username_wrp.get_thread().start();
				 }
			} catch (NoSuchElementException e) {
				System.err.println("Usage: login <username> <password>");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.err.println("Please first: register <username> <password>");
			} finally {
				if(res == null && username != null)
					try {
						this.serv_service.deregisterMe(username, this.stub);
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

			}
			
		} else {
			System.err.println("There is a connected user, firstly log out.");
		}
	}
	//@Effects: asks the server to log out
	//@param args: should be empty
	private void logout(String args) {
		HttpRequest req = create_req(DELETE, URI_LOGOUT);
		JsonFactory jsonFact = new JsonFactory();
		JsonGenerator jsonGen = null;
		File file_foll = new File(StaticNames_Client.PATH_TO_CLIENT + username_wrp.get_username() + StaticNames_Client.NAME_FILE_FOLL);
		String res = null;
		writer_skt(req,null, 0);
		this.logged=false;
		this.cookies="";
		res = reader_skt();
		if(res != null) {
			try {
				this.serv_service.deregisterMe(this.username_wrp.get_username(), this.stub);
				jsonGen = jsonFact.createGenerator(file_foll, StaticNames_Client.ENCODING);
				jsonGen.useDefaultPrettyPrinter();
				jsonGen.writeStartArray();
				for(String us: this.all_followers) {
					jsonGen.writeString(us);
				}
				jsonGen.writeEndArray();
				jsonGen.flush();
				jsonGen.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.all_followers.clear();
			this.mcast_not.leave();
			System.out.println(this.username_wrp.get_username() + " logged out");
			System.out.flush();
	 }
	}
	//@Effects: asks the server to follow the user x
	//@param args: the name of the user to be follow
	private void follow_user(String args) {
		args = args.strip();
		HttpRequest req = create_req(PUT, URI_FOLLOW_USER+"/"+args);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null, 0);
		String res = reader_skt();
		if(res !=null) {
			Matcher m = p.matcher(res);
			m.find();
			System.out.println(m.group(2)+".");
			System.out.flush();
		}
	}
	
	//@Effects: asks the server to unfollow the user x
	//@param args: the name of the user to be unfollowed
	private void unfollow_user(String args) {
		args = args.strip();
		HttpRequest req = create_req(PUT, URI_UNFOLLOW_USER+"/"+args);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null, 0);
		String res = reader_skt();
		if(res !=null) {
			Matcher m = p.matcher(res);
			m.find();
			System.out.println(m.group(2)+".");
			System.out.flush();
		}
	}
	
	private void rewin_post(String args) {
		args = args.strip();
		HttpRequest req = create_req(PUT, URI_REWIN_POST+"/"+args);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null, 0);
		String res = reader_skt();
		if(res !=null) {
			Matcher m = p.matcher(res);
			m.find();
			System.out.println(m.group(2)+".");
			System.out.flush();
		}
	}
	//@effects: asks the server to rewind the post
	//@param args: the id of the post to be rewinded
	private void rate_post(String args) {
		StringTokenizer toks = new StringTokenizer(args, " ");
		try {
			String id_post = toks.nextToken().strip();
			String react = toks.nextToken();
			HttpRequest req = create_req(PUT, URI_RATE_POST+"/"+id_post+"/"+react);
			Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
			writer_skt(req, null, 0);
			String res = reader_skt();
			if(res !=null) {
				Matcher m = p.matcher(res);
				m.find();
				System.out.println(m.group(2)+".");
				System.out.flush();
			}
		} catch (NoSuchElementException e) {
			System.err.println("Usage: login <username> <password>");
		}
	}
	
	//@Effects: asks the server to add a comment to a post
	//@param args: the id of the post and the content of the comment
	private void add_comment(String args) {
		Pattern p = Pattern.compile("(?=\\w)(\\w+\\s*?\\w.*?)(?=\")");
		Matcher m = p.matcher(args);
		try {
			m.find();
			String id_post = m.group(1).strip();
			m.find();
			String content = m.group(1);
			HttpRequest req = create_req(PUT, URI_ADD_COMMENT+"/" +id_post);
			BasicHttpEntity ent = new BasicHttpEntity();
			
			String json = "{\"content\":\"" + content + "\"}";
			ByteArrayInputStream stream = new ByteArrayInputStream(json.getBytes());
			ent.setContent(stream);
			req.setHeader("Content-Type", "application/json");
			req.setHeader("Content-Length", json.length()+"");
			p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
			writer_skt(req, ent, json.length());
			String res = reader_skt();
			if(res !=null) {
				m = p.matcher(res);
				m.find();
				System.out.println(m.group(2)+".");
				System.out.flush();
			}
			stream.close();
		} catch (NoSuchElementException e) {
			System.err.println("Usage: comment <idPost> <comment>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//@Effects: asks the server to create a post
	//@param args: title and the content of the post
	private void create_post(String args) {
		Pattern p = Pattern.compile("(?<=\")(\\w+\\s*?\\w.*?)(?=\")");
		Matcher m = p.matcher(args);
		try {
			HttpRequest req = create_req(POST, URI_CREATE_POST);
			m.find();
			String title = m.group(1);
			m.find();
			String content = m.group(1);
			BasicHttpEntity ent = new BasicHttpEntity();
			String json = "{\"title\":\"" + title + "\", \"content\":\"" + content+ "\"}";
			ByteArrayInputStream stream = new ByteArrayInputStream(json.getBytes());
			ent.setContent(stream);
			req.setHeader("Content-Type", "application/json");
			req.setHeader("Content-Length", json.length()+"");
			p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
			writer_skt(req, ent, json.length());
			String res = reader_skt();
			if(res !=null) {
				m = p.matcher(res);
				m.find();
				System.out.println(m.group(2)+".");
				System.out.flush();
			}
			stream.close();
		} catch (NoSuchElementException e) {
			System.err.println("Usage: post <title> <content>");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//@Effects: asks the server to delete a post
	//@param args: should empty
	private void delete_post(String arg) {
		arg = arg.strip();
		HttpRequest req= create_req(DELETE, URI_DELETE_POST +"/"+arg);
		Pattern p = Pattern.compile("(?<=\")(:\"([a-zA-z].+?))(?=\")");
		writer_skt(req, null, 0);
		String res = reader_skt();
		if(res !=null) {
			Matcher m = p.matcher(res);
			m.find();
			System.out.println(m.group(2)+".");
			System.out.flush();
		}
	}
	
	
	//@Effects: writes the request to the socket
	//@param req: the request to be written
	//@param ent: the entity(content) of request
	//@ length_cont_ent: the length of content associated with the request
	private void writer_skt (HttpRequest req, HttpEntity ent, int length_cont_ent) {
		try {
			this.ses_out.bind(cl_skt.socket().getOutputStream());
			DefaultHttpRequestWriter wrt = new DefaultHttpRequestWriter(this.ses_out);
			wrt.write(req);
			if(ent != null) {
			
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
			}
			ses_out.flush();
			cl_skt.socket().getOutputStream().flush();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	//Effects: reads from the socket the response
	private String reader_skt() {
		Header[] cookie = null;
		Pattern r = Pattern.compile("(.*=.*)(?=;)");
		Matcher m = null;
		HttpResponse res_par;
		String ret_val = null;
		byte[] bytes = null;
		try {
			this.ses_in.bind(this.cl_skt.socket().getInputStream());
			DefaultHttpResponseParser res = new DefaultHttpResponseParser(this.ses_in);
			res_par = res.parse();
			if((cookie = res_par.getHeaders("Set-Cookie")) != null && this.cookies.length() == 0) {
				for(Header h: cookie) {					
					m = r.matcher(h.getValue());
					if(m.find()) {
						cookies =cookies.concat("; " +m.group(1));
					}
				}
				this.cookies=this.cookies.replaceFirst("; ", "");
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
				r = Pattern.compile("(?<=\")(\\w.+?|:)(?=\")");
				ret_val = new String(ent.getContent().readAllBytes());
				m = r.matcher(ret_val);
				m.find();
				System.err.print("code: " + status.getStatusCode() + " " +m.group(1));
				m.find();
				System.err.print(m.group(1) + " ");
				m.find();
				System.err.println(m.group(1));
				System.out.flush();
				m.find();
				return null;
			}
			bytes=ent.getContent().readAllBytes();
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new String(bytes);
	}
	
	private void help(String arg) {
		System.out.println(String.format("%1$40s%2$40s%3$40s%4$20s%5$20s%6$20s%7$20s%8$20s%9$20s%10$20s%11$20s%12$20s%13$20s%14$20s%15$20s%16$20s%17$20s%18$20s", "register <username> <password> <\"tags\">\n",
				 "login <username> <password>\n", "logout\n", "list users\n", "list followers\n", "list following\n", "follow <username>\n", "unfollow <username>\n", "blog\n", "post <\"title\"> <\"content\">\n", "show feed\n", "show post <idPost>\n", 
				 "delete <idPost>\n", "rewin <idPost> <vote>\n", "comment <idPost> <\"comment\">\n", "wallet\n", "wallet btc", "help", "where: <tags> are list of words enclosed in \"\n     <title> title enclosed in \" with a length <= 20\n     <content> content enclosed in \" with a maxium length of 500 characters"));
	}
	//@effects: creates an http request with the same headers
	private HttpRequest create_req(String method, String uri) {
		HttpRequest resp = new BasicHttpRequest(method, uri, new ProtocolVersion("HTTP", 1, 1));
		resp.addHeader("Date", FORMATTER.format(Calendar.getInstance().getTime()));
		resp.addHeader("Accept", "application/json");
		resp.addHeader("Accept-Language", "en-US");
		resp.addHeader("Connection", "Keep-Alive");
		resp.addHeader("Cache-Control", "max-age=60");
		resp.addHeader("Host", IP);
		if(this.cookies.length() > 0)
			resp.addHeader("Cookie", cookies);
		return resp;
	}
	
}











