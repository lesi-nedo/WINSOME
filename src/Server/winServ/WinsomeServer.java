package winServ;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.BiFunction;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import rec_fol.ReceiveUpdatesInterface;

public class WinsomeServer {
	/*
	 * Overview: The implementation of the server
	 */
	static final SimpleDateFormat FORMATTER =new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
	private int AWAIT_BEFORE_HARD_TERM=60;

	//Each functionality is stored in a lambda fuction
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> list_users = (Obj, arg) -> Obj.list_users(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> list_following = (Obj, arg) -> Obj.list_following(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> view_blog = (Obj, arg) -> Obj.view_blog(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> show_feed = (Obj, arg) -> Obj.show_feed(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> get_wallet = (Obj, arg) -> Obj.get_wallet(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> get_wallet_in_bitcoin = (Obj, arg) -> Obj.get_wallet_in_bitcoin(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> show_post = (Obj, arg) -> Obj.show_post(arg);
	static final Map<String, BiFunction<ReaderClientMessages, HttpRequest, HttpResponse>> GET_OP = Map.of("list_users", list_users, "list_following", list_following, "view_blog",view_blog, 
			"show_feed", show_feed, "get_wallet", get_wallet, "get_wallet_in_bitcoin", get_wallet_in_bitcoin,  "show_post",show_post);
	
	
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> login = (Obj, arg) -> Obj.login(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> create_post = (Obj, arg) -> Obj.create_post(arg);
	static final Map<String, BiFunction<ReaderClientMessages, HttpRequest, HttpResponse>> POST_OP = Map.of("login", login, "create_post", create_post);
	
	
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> logout = (Obj, arg) -> Obj.logout(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> delete_post = (Obj, arg) -> Obj.delete_post(arg);
	static final Map<String, BiFunction<ReaderClientMessages, HttpRequest, HttpResponse>> DELETE_OP = Map.of("logout", logout, "delete_post", delete_post);
	
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> follow_user = (Obj, arg) -> Obj.follow_user(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> unfollow_user = (Obj, arg) -> Obj.unfollow_user(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> rewin_post = (Obj, arg) -> Obj.rewin_post(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> rate_post = (Obj, arg) -> Obj.rate_post(arg);
	static final BiFunction<ReaderClientMessages, HttpRequest, HttpResponse> add_comment = (Obj, arg) -> Obj.add_comment(arg);
	static final Map<String, BiFunction<ReaderClientMessages, HttpRequest, HttpResponse>> PUT_OP = Map.of("follow_user", follow_user, "unfollow_user", unfollow_user, "rewin_post", rewin_post, "rate_post", rate_post, "add_comment", add_comment);
	
	
	static final Map<String, Map<String, BiFunction<ReaderClientMessages, HttpRequest, HttpResponse>>> METHODS_OP= Map.of("GET", GET_OP, "PUT", PUT_OP,  "POST", POST_OP,  "DELETE", DELETE_OP);
	private int port; //on this port will open a listening socket
	private int num_active_con=0;//number of active clients
	private String IP_serv; 
	private int BUFF_LIMIT= 8388608;
	private ConcurrentMap<String, ReceiveUpdatesInterface> users_to_upd;
	private ThreadPoolExecutor exec_pool;
	private ConcurrentMap<String, ReadWriteLock> tags_in_mem;
	private ConcurrentMap<String, ReadWriteLock> usernames;
	private ConcurrentMap<String, String> logged_users;
	private volatile boolean can_run;//this variable gets update by the shutdownhook
	private BlockingQueue<HttpWrapper> queue = new LinkedBlockingQueue<HttpWrapper>();
	private AtomicBoolean wake_called;
	private SocketChannel client; 
	private Selector sel; 
	
	private int mcast_port;
	private InetAddress mcast_addr;
	private int period;
	private float reward_author;
	private Timer timer;
	
	public WinsomeServer(int port, String IP_serv, int BUFF_LIMIT, ConcurrentMap<String, ReceiveUpdatesInterface> users_to_upd, ConcurrentMap<String, ReadWriteLock> tags_in_mem, ConcurrentMap<String, ReadWriteLock> usernames) {
		this.port=port;
		this.IP_serv=IP_serv;
		this.BUFF_LIMIT=BUFF_LIMIT;	
		this.users_to_upd= users_to_upd;
		this.tags_in_mem=tags_in_mem;
		this.usernames=usernames;
		this.logged_users=new ConcurrentHashMap<String, String>();
		this.can_run=true;
		this.mcast_addr=null;
		this.mcast_port = 0;
		this.period=0;
		this.reward_author=0;
		this.timer= new Timer();
		this.wake_called=new AtomicBoolean(false);
	}
	public void start_serv(int timeout) {
		try(ServerSocketChannel s_cha=ServerSocketChannel.open()){
			s_cha.socket().bind(new InetSocketAddress(InetAddress.getByName(this.IP_serv), this.port));
//			s_cha.socket().setSoTimeout(timeout);
			s_cha.configureBlocking(false);
			sel = Selector.open();
			s_cha.register(sel, SelectionKey.OP_ACCEPT);
			this.exec_pool= (ThreadPoolExecutor) Executors.newCachedThreadPool();
			
			if(this.mcast_addr == null) {
				System.err.println("Multicast variables not initialized");
				return;
			}
			//schedules the calculation of the earnings task in a timer
			this.timer.schedule(new CalcEarningsThread(this.mcast_port, this.mcast_addr, this.usernames, this.reward_author), this.period, this.period);
			//Each ready SocketChannel is passed to the threadPool to be served and than they get back 
			//to the main server throughout the blocking queue
			while(this.can_run) {
				while(sel.select()==0) {
					this.wake_called.set(false);
					reg_from_queue();
					if(sel.selectNow() > 0 || !this.can_run) {
						break;
					}
				}
				if(!this.can_run)
					break;
				this.wake_called.set(false);
				Set<SelectionKey> sel_key=sel.selectedKeys();
				Iterator<SelectionKey> iter=sel_key.iterator();
				while(iter.hasNext()) {
					SelectionKey key=iter.next();
					if(!key.isValid()) {
						continue;
					}
					iter.remove();
					try {
						if(key.isValid() && key.isAcceptable()) {
							ServerSocketChannel new_sck= (ServerSocketChannel) key.channel();
							SocketChannel c_cha= new_sck.accept();
							c_cha.configureBlocking(false);
							c_cha.register(sel, SelectionKey.OP_READ, null);
							this.num_active_con++;
//							System.err.println(this.num_active_con);
						}
						else if(key.isValid() && key.isReadable()) {
							ReaderClientMessages task = new ReaderClientMessages(sel, key, this.BUFF_LIMIT, this.usernames, this.tags_in_mem, logged_users, this.users_to_upd, queue, this.wake_called);
							task.set_mcast_port_addr(mcast_port, mcast_addr);
							key.cancel();
							this.exec_pool.execute(task);
						} else if(key.isValid() && key.isWritable()) {
							WriterMessagesToClient wrt = new WriterMessagesToClient(sel, key, queue, this.wake_called);
							key.cancel();
							this.exec_pool.execute(wrt);
						}
					} catch(IOException e) {
						e.printStackTrace();
						key.channel().close();
						key.cancel();
						this.num_active_con--;
					}
				}
			}
			//shuts the poll and closes the socket
			exec_pool.shutdown();
			while(!exec_pool.isTerminated())
				exec_pool.awaitTermination(AWAIT_BEFORE_HARD_TERM, TimeUnit.SECONDS);
			sel.close();
			s_cha.socket().close();
			s_cha.close();
			return;
		} catch(IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			exec_pool.shutdownNow();
		}
	}
	//@Effects: Initializes the variables related to the multicast service
	//@param port: port to attach to the packet
	//@param period: the rest period of the thread
	//@param addr: the address to attach to the packet
	//@param rewar_author: the percentage that should be given to the author
	public void initMcastVars(int port, int period, InetAddress addr, float reward_author) {
		this.mcast_port=port;
		this.mcast_addr=addr;
		this.period=period*1000;
		this.reward_author=reward_author;
	}
	//@Effects: terminates the server
	public void end_me() {
		this.can_run=false;
		this.sel.wakeup();
	}
	
	public int getMcasPort() {
		return this.mcast_port;
	}
	
	public InetAddress getMcas_addr() {
		return this.mcast_addr;
	}
	//@Effects: takes the wrapper from the queue, extracts the SocketChannel, and registers the new client with the relative operation
	private void reg_from_queue() throws ClosedChannelException {
		if(!this.queue.isEmpty()) {
			HttpWrapper wrp;
			while((wrp = this.queue.poll()) != null) {
				this.client = wrp.get_client();
				this.client.register(this.sel, wrp.get_op(), wrp.getResp()==null ? null : wrp);
			}
				
		}
	}
		
}
