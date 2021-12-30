package winServ;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;

import sign_in.TooManyTagsException;
import utils.User_Data;

public class WinsomeServer {
	/*
	 * Overview: 
	 */
	static final String[] METHODS= {"GET", "PUT", "POST", "DELETE"};
	static final String[] ALL_OP= {"/login", "/logout", "/list_users", "/list_following", "/follow", "/unfollow", "/blog", "/post_post", "/show_feed", "/show_post", "/delete_post", "/rewin", "/rate_post", "/comment", 
			"/wallet", "/wallet_in_btc"};
	static final int MOD_METH=11;
	static final int MOD_NUM_METH=4;
	private int port; //on this port will open a listening socket
	private int num_active_con=0;//number of active clients
	private String IP_serv; 
	private static final int req_head_limit=16384;
	public WinsomeServer(int port, String IP_serv) {
		this.port=port;
		this.IP_serv=IP_serv;
	}
	public void start_serv() throws TooManyTagsException {
		try(ServerSocketChannel s_cha=ServerSocketChannel.open()){
			s_cha.socket().bind(new InetSocketAddress(InetAddress.getByName(this.IP_serv), this.port));
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
		ByteBuffer head=ByteBuffer.allocate(req_head_limit);
		c_cha.register(sel, SelectionKey.OP_READ, head);
	}
	
	private void readClientMessage(Selector sel, SelectionKey key) throws IOException, TooManyTagsException {
		SocketChannel c_sk=(SocketChannel) key.channel();
		ByteBuffer bfs=(ByteBuffer) key.attachment();
		ConcurrentHashMap<String, ReadWriteLock> usernames = new ConcurrentHashMap<String, ReadWriteLock>();
		ConcurrentHashMap<String, ReadWriteLock> tags_in_mem = new ConcurrentHashMap<String, ReadWriteLock>();
		User_Data.load_Usernames(usernames);
		User_Data.load_Tags(tags_in_mem);
		StringBuilder b_str=new StringBuilder();
		int byte_read =c_sk.read(bfs);
		bfs.flip();
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
		StringTokenizer tokens = new StringTokenizer(new String(bfs.array()), "\r\n");
		while(tokens.hasMoreTokens()) {
			System.out.println(tokens.nextToken());
		}
	}
	
}
