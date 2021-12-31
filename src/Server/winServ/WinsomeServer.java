package winServ;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpResponse;

import sign_in.TooManyTagsException;
import utils.StaticNames;
import utils.User_Data;

public class WinsomeServer {
	/*
	 * Overview: 
	 */
	static final SimpleDateFormat FORMATTER =new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	static final Set<String> GET_OP = Set.of("list_users", "list_following", "view_blog", "show_feed", "get_wallet", "get_wallet_in_bitcoin", "show_post");
	static final Set<String> POST_OP = Set.of("login", "create_post", "add_comment");
	static final Set<String> DELETE_OP = Set.of("logout", "delete_post");
	static final Set<String> PUT_OP = Set.of("follow_user", "unfollow_user", "rewin_post", "rate_post");
	static final Map<String, Set<String>> METHODS_OP= Map.of("GET", GET_OP, "PUT", PUT_OP,  "POST", POST_OP,  "DELETE", DELETE_OP);
	private int port; //on this port will open a listening socket
	private int num_active_con=0;//number of active clients
	private String IP_serv; 
	private int BUFF_LIMIT= 8388608;
	public WinsomeServer(int port, String IP_serv, int BUFF_LIMIT) throws NoSuchMethodException, SecurityException {
		this.port=port;
		this.IP_serv=IP_serv;
		this.BUFF_LIMIT=BUFF_LIMIT;	
		System.out.println(Operations.class.getDeclaredMethod("getSessionId"));
	}
	public void start_serv(int timeout) throws TooManyTagsException {
		try(ServerSocketChannel s_cha=ServerSocketChannel.open()){
			s_cha.socket().bind(new InetSocketAddress(InetAddress.getByName(this.IP_serv), this.port));
			s_cha.socket().setSoTimeout(timeout);
			s_cha.configureBlocking(false);
			Selector sel = Selector.open();
			s_cha.register(sel, SelectionKey.OP_ACCEPT);
			while(true) {
				if(sel.select()==0)
					continue;
				Set<SelectionKey> sel_key=sel.selectedKeys();
				Iterator<SelectionKey> iter=sel_key.iterator();
				while(iter.hasNext()) {
					SelectionKey key=iter.next();
					if(!key.isValid()) {
						continue;
					}
					iter.remove();
					try {
						if(key.isAcceptable()) {
							ServerSocketChannel new_sck= (ServerSocketChannel) key.channel();
							SocketChannel c_cha= new_sck.accept();
							c_cha.configureBlocking(false);
							this.registRead(sel, c_cha);
							this.num_active_con++;
						}
						else if(key.isReadable()) {
							this.readClientMessage(sel, key);
						} else if(key.isWritable()) {
							//this.responde();
						}
					} catch(IOException e) {
						e.printStackTrace();
						key.channel().close();
						key.cancel();
						this.num_active_con--;
					}
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	private void registRead(Selector sel, SocketChannel c_cha) throws IOException {
		ByteBuffer head=ByteBuffer.allocate(BUFF_LIMIT);
		c_cha.register(sel, SelectionKey.OP_READ, head);
	}
	
	private void readClientMessage(Selector sel, SelectionKey key) throws IOException, TooManyTagsException {
		ConcurrentHashMap<String, ReadWriteLock> usernames = new ConcurrentHashMap<String, ReadWriteLock>();
		ConcurrentHashMap<String, ReadWriteLock> tags_in_mem = new ConcurrentHashMap<String, ReadWriteLock>();
		User_Data.load_Usernames(usernames);
		User_Data.load_Tags(tags_in_mem);
		ByteBuffer test = ByteBuffer.allocate(Integer.SIZE+StaticNames.MSG_NOTIFY_MULTICAS.length());
		test.putInt(StaticNames.MSG_NOTIFY_MULTICAS.length());
		test.put(StaticNames.MSG_NOTIFY_MULTICAS.getBytes());
		System.out.println(test.position() + "  " + StaticNames.MSG_NOTIFY_MULTICAS.length());
		test.flip();
		String res = "";
		int length_msg=test.getInt();
		byte[] msg_b = new byte[length_msg];
		test.get(msg_b);
		res = new String(msg_b);
		System.out.println(res);
//		System.out.println(Operations.list_following("&test_user&1", usernames.get("&test_user&1")));
//		System.out.println(Operations.follow_user("&test_user&1", "&test_user&0ghVXXG", usernames.get("&test_user&1"), usernames.get("&test_user&0ghVXXG")));
//		System.out.println(Operations.unfollow_user("&test_user&1", "&test_user&0erDmkW", usernames.get("&test_user&0erDmkW"), usernames.get("&test_user&0erDmkW")));
//		System.out.println(Operations.list_following("&test_user&1", usernames.get("&test_user&1")));
//		System.out.println(Operations.createPost("&test_user&1", "suka", "time to run", usernames.get("&test_user&1")));
//		System.out.println(Operations.createPost("&test_user&1", "title1", "This is a test post", usernames.get("&test_user&1")));
//		System.out.println(Operations.createPost("&test_user&1", "title2", "This is a test post", usernames.get("&test_user&1")));
//		System.out.println(Operations.createPost("&test_user&1", "title3", "This is a test post", usernames.get("&test_user&1")));
//		System.out.println(Operations.view_blog("&test_user&1", usernames.get("&test_user&1")));
//		System.out.println(Operations.show_feed("&test_user&1", usernames.get("&test_user&1")));
//		System.out.println(Operations.show_post("&test_user&1", "Bo4hrPGXgP", usernames.get("&test_user&1")));
//		System.out.println(Operations.delete_post("&test_user&1", "Bo4hrPGXgP", usernames.get("&test_user&1")));
		System.out.println(Operations.show_post("&test_user&0Fdq9M", "J0GsSi0AxCk5", usernames));
//		System.out.println(Operations.rewin_post("&test_user&0ghVXXG", "oZHmgLHn8Q", usernames.get("&test_user&0ghVXXG")));
//		System.out.println(Operations.rate_post("&test_user&0ghVXXG", "oZHmgLHn8Q", -1, usernames));
//		System.out.println(Operations.add_comment("&test_user&0ghVXXG", "oZHmgLHn8Q", "SUka blat", usernames));
//		System.out.println(Operations.add_comment("&test_user&0ghVXXG", "oZHmgLHn8Q", "SUka blat", usernames));
//		System.out.println(Operations.add_comment("&test_user&1", "oZHmgLHn8Q", "SUka blat", usernames));
//		System.out.println(Operations.get_wallet_in_bitcoin("&test_user&1", usernames.get("&test_user&1")));
//		System.out.println(Operations.show_feed("&test_user&0VK7pIl", usernames.get("&test_user&0VK7pIl")));	
		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP", 1, 1), 400 , "Bad Request	");
		resp.setHeader("Date", WinsomeServer.FORMATTER.format(Calendar.getInstance().getTime()));
		System.out.println(resp.toString());
	}
		
}
