package test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.fasterxml.jackson.core.JsonParseException;

import notify_client.Followers;
import notify_client.FollowersInterface;
import par_file.EmptyFileException;
import par_file.IllegalFileFormatException;
import par_file.ParsingConfFile;
import rec_fol.ReceiveUpdates;
import rec_fol.ReceiveUpdatesInterface;
import serv_inter.IncorrectOperationException;
import serv_inter.InterWithServ;
import serv_inter.UsernameWrp;
import sign_in.Sign_In;
import sign_in.Sign_In_Interface;
import utils.StaticMethods;
import utils.StaticNames;
import utils.StaticNames_Client;
import utils.User_Data;
import winServ.ShutTheServ;
import winServ.WinsomeServer;


class ClientTest {
	
	static final String[] TAGS = {"Racing", "Pigeons",
			"Racquetball",
			"Rafting",
			"Railfans",
			"Rappelling",
			"Rapping",
			"Reading",
			"Relaxing",
			"movies",
			"Robotics",
			"Rockets",
			"Roleplaying",
			"Rugby",
			"Running",
			"Sailing",
			"Scrapbooking",
			"Scuba_diving",
			"Sculling",
			"Sculpting",
			"Sewing",
			"Shooting"};
	
	private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
	private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
	private final PrintStream originalOut = System.out;
	private final PrintStream originalErr = System.err;
	private static final int NUM_REGISTERS=20;
 	private static Thread serv_thr = null;
	private static WinsomeServer serv;
 	
	@BeforeAll
	static void init_serv() {
		String SER_NAME="SIGN_IN"; //name of the RMI service that unable the sign in
		String SER_NAME_FOLL="UPDATED_ME";  //name of the RMI callback service
		String[] CONFS= {"SERVER", "TCPPORT", "UDPPORT", "MULTICAST", "MCASTPORT", "REGHOST", "REGPORT", "TIMEOUT", "GAINPERIOD", "BUFF_LIMIT", "REWARD_AUTHOR"}; //all configurations accepted
		String DEFAULT_SERVER="192.168.1.24";
		int DEFAULT_TCPPORT=6666;
		String DEFAULT_MULTICAST="239.255.32.32";
		int DAFAULT_MCASTPORT=44444;
		String DEFAULT_REGHOST="localhost";
		int DEFAULT_REGPORT=7777;
		int DEFAULT_TIMEOUT=100000;
		int DEFAULT_GAINPERIOD=60; //in seconds
		int DEFAULT_BUFF_LIMIT= 8*1024*1024;
		float DEFAULT_REWARD_AUTHOR = 0.70f;
		try {
			ParsingConfFile confs=new ParsingConfFile("src/Server/conf_server.txt", CONFS); //starts to parse the conf file
			User_Data.setSettings_Server(StaticNames.PATH_TO_SSL); //set the setting for the SslSocket i.e truststore, keystore and password
			//sets the variables needed by the server
			String conf =confs.getConf("REGPORT");
			int REGPORT= conf == null ? DEFAULT_REGPORT: Integer.valueOf(conf);
			conf=confs.getConf("REGHOST");
			String REGHOST = conf == null ? DEFAULT_REGHOST:conf;
			conf=confs.getConf("SERVER");
			String SERVER= conf ==null ? DEFAULT_SERVER : conf;
			conf = confs.getConf("TCPPORT");
			int TCPPORT= conf == null ? DEFAULT_TCPPORT : Integer.valueOf(conf);
			conf = confs.getConf("MULTICAST");
			String MULTICAST = conf == null ? DEFAULT_MULTICAST : conf;
			conf = confs.getConf("MCASTPORT");
			int MCASTPORT = conf == null ? DAFAULT_MCASTPORT : Integer.valueOf(conf);
			conf = confs.getConf("TIMEOUT");
			int TIMEOUT = conf == null ? DEFAULT_TIMEOUT : Integer.valueOf(conf);
			conf=confs.getConf("GAINPERIOD");
			int GAINPERIOD = conf == null ? DEFAULT_GAINPERIOD : Integer.valueOf(conf); 
			conf = confs.getConf("UDPPORT");
			conf = confs.getConf("BUFF_LIMIT");
			int BUFF_LIMIT = conf == null ? DEFAULT_BUFF_LIMIT : Integer.valueOf(conf);
			conf = confs.getConf("REWARD_AUTHOR");
			float reward_author = conf==null? DEFAULT_REWARD_AUTHOR : Float.valueOf(conf);
			
			InetAddress m_group = InetAddress.getByName(MULTICAST);
			if(!m_group.isMulticastAddress() || reward_author > 1.0f)
				throw new IllegalArgumentException();
			
			ConcurrentMap<String, ReadWriteLock> tags_in_mem=new ConcurrentHashMap<String, ReadWriteLock>(); //all tags specified by the users
			ConcurrentMap<String, ReadWriteLock> usernames=new ConcurrentHashMap<String, ReadWriteLock>(); //all names of the users
			ConcurrentMap<String, ReceiveUpdatesInterface> users_to_upd=new ConcurrentHashMap<String, ReceiveUpdatesInterface>(); // users that are signed up for the updates about the followers
			Sign_In sign_ser;
			SslRMIClientSocketFactory csf=new SslRMIClientSocketFactory();//ini secure sockets
			SslRMIServerSocketFactory ssf=new SslRMIServerSocketFactory(null, null, true);
			Registry reg;	
			FollowersInterface stub= null;
			Followers serv_fol=null;
				
			serv_fol=new Followers(users_to_upd);
			//loads from the folder User_Data all tags and usernames
			User_Data.load_Tags(tags_in_mem);
			User_Data.load_Usernames(usernames);
				
			sign_ser=new Sign_In(REGPORT, tags_in_mem, usernames);//initialization of the remote object

			LocateRegistry.createRegistry(REGPORT, csf, ssf);
			reg=LocateRegistry.getRegistry(REGHOST, REGPORT, new SslRMIClientSocketFactory());
			reg.bind(SER_NAME, sign_ser);
			stub= (FollowersInterface)UnicastRemoteObject.exportObject(serv_fol,0);
			reg.bind(SER_NAME_FOLL, stub);
			serv=new WinsomeServer(TCPPORT, SERVER, BUFF_LIMIT, users_to_upd, tags_in_mem, usernames);//initialization of the server
			Runtime.getRuntime().addShutdownHook(new ShutTheServ(serv));
			serv.initMcastVars(MCASTPORT, GAINPERIOD, m_group, reward_author);
			System.out.println("Server has started.");
			serv_thr = new Thread(new ServRunnable(serv, TIMEOUT));
			serv_thr.start();
		} catch (JsonParseException e1) {
				// TODO Auto-generated catch block		
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (EmptyFileException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalFileFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AlreadyBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@BeforeEach
	public void setUpStreams() {
		System.setOut(new PrintStream(outContent));
	    System.setErr(new PrintStream(errContent));
	}

	@AfterEach
	public void restoreStreams() {
	    System.setOut(originalOut);
	    System.setErr(originalErr);
	}
	
	@DisplayName("test client")
	@RepeatedTest(10)
	void test_client() throws InterruptedException {
		String[] CONFS= {"SERVER", "TCPPORT", "REGHOST", "REGPORT", "TIMEOUT", "NAME_SIGN_REG", "NAME_CALLBACK_UPFOL"}; //all configurations accepted
		String DEFAULT_SERV="192.168.1.24";
		int DEFAULT_TCPPORT=6666;
		String DEFAULT_REGHOST="localhost";
		int DEFAULT_REGPORT=7777;
		int DEFAULT_TIMEOUT=1000;
		int FAILURE_STAT_CODE=0;
		String DEFAULT_NAME_SIGN_REG="SIGN_IN";
		String DEFAULT_NAME_CALLBACK_UPFOL="UPDATED_ME";
		String conf = null;
		int REGPORT =0;
		String REGHOST=null;
		String SERVER=null;
		String NAME_SIGN_REG=null;
		String NAME_CALLBACK_UPFOL=null;
		int TCPPORT=0;
		int TIMEOUT = 0;
		Set<String> all_followers=new HashSet<String>();
		Sign_In_Interface sign_r;
		Registry registry = null;
		FollowersInterface upd_foll_r;
		String IP = null;
		ReceiveUpdatesInterface stub;
		ReceiveUpdatesInterface callObj;
		InterWithServ inter;
		UsernameWrp username_wrp=new UsernameWrp();
		
		try(final DatagramSocket sock = new DatagramSocket()) {
			sock.connect(InetAddress.getByName("8.8.8.8"), 10002);
			IP = sock.getLocalAddress().getHostAddress();
			ParsingConfFile confs=new ParsingConfFile(StaticNames_Client.NAME_CONF_FILE, CONFS);
			StaticMethods.setSettings_client(StaticNames_Client.PATH_TO_SSL);
				
			conf =confs.getConf("REGPORT");
			REGPORT= conf == null ? DEFAULT_REGPORT: Integer.valueOf(conf);
			conf=confs.getConf("REGHOST");
			REGHOST = conf == null ? DEFAULT_REGHOST:conf;
			conf=confs.getConf("SERVER");
			SERVER = conf ==null ? DEFAULT_SERV : conf;
			conf = confs.getConf("TCPPORT");
			TCPPORT = conf == null ? DEFAULT_TCPPORT : Integer.valueOf(conf);
			conf = confs.getConf("TIMEOUT");
			TIMEOUT = conf == null ? DEFAULT_TIMEOUT : Integer.valueOf(conf);			
			conf = confs.getConf("NAME_SIGN_REG");
			NAME_SIGN_REG = conf == null ? DEFAULT_NAME_SIGN_REG : conf;
			conf = confs.getConf("NAME_CALLBACK_UPFOL");
			NAME_CALLBACK_UPFOL = conf == null ? DEFAULT_NAME_CALLBACK_UPFOL : conf;
		} catch (IOException | EmptyFileException | IllegalFileFormatException e) {
			// TODO Auto-generated catch block
			System.out.println("Please fix the configuration file than rerun me.");
			e.printStackTrace();
			System.exit(FAILURE_STAT_CODE);
		}
		try( SocketChannel cl_sk = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(SERVER), TCPPORT)); ) {
//			cl_sk.socket().setSoTimeout(TIMEOUT);
			registry = LocateRegistry.getRegistry(REGHOST, REGPORT, new SslRMIClientSocketFactory());
			sign_r = (Sign_In_Interface) registry.lookup(NAME_SIGN_REG);
			upd_foll_r = (FollowersInterface) registry.lookup(NAME_CALLBACK_UPFOL);
			callObj = new ReceiveUpdates(all_followers);
			stub = (ReceiveUpdatesInterface) UnicastRemoteObject.exportObject(callObj, 0);
			inter = new InterWithServ(sign_r, cl_sk, IP, upd_foll_r, stub, username_wrp, all_followers, TIMEOUT);
			String username = null;
			String password = null;
			Random rand = new Random();
			for(int i = 0; i < NUM_REGISTERS; i++) {
				username = "&test_user&" + User_Data.generateString(5);
				password = "password";
				String tags = "";
				for(int j = 0; j< rand.nextInt(5)+1; j++) {
					if(rand.nextInt() % (j+1) == 0) {
						tags = tags.concat(" &test_tag&"+TAGS[rand.nextInt(TAGS.length)]);
						continue;
					}
					tags = tags.concat(" &test_tag&"+User_Data.generateString(rand.nextInt(15)+5));
				}
				tags = tags.trim();
				outContent.reset();
				InterWithServ.send_req(inter, "register " +username + " " + password + " \"" +tags+ "\"");
				assertEquals("ok\n", outContent.toString());
			}
			String out = null;
			String new_user = null;
			Set<String> users = new HashSet<String>();
			Pattern p = Pattern.compile("(?<=\\n)(.+?)(?=\\|)");
			Matcher m= null;
			String id_post = null;
			String temp_out = null;
			boolean found =false;
			outContent.reset();
			InterWithServ.send_req(inter, "login "+username + " " +password);
			assertEquals(username + " logged in\n", outContent.toString());
			outContent.reset();
			InterWithServ.send_req(inter, "list users");
			out = outContent.toString();
			m = p.matcher(out);
			while(m.find()) {
				users.add(m.group(1).strip());
			}
			for(String us: users) {
				if(rand.nextInt() % Thread.currentThread().getId() > (int) (Thread.currentThread().getId()%2) ) {
					outContent.reset();
					new_user=us;
					InterWithServ.send_req(inter, "follow "+us);
					assertEquals("The user: "+username+" now follows "+us+".\n", outContent.toString());
				}
					
			}
			if(new_user != null) {
				outContent.reset();
				InterWithServ.send_req(inter, "logout");
				assertEquals(username+" logged out\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "login "+new_user + " " +password);
				assertEquals(new_user + " logged in\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "post \"this is a post\" \"some content\"");
				temp_out = outContent.toString();
				id_post = temp_out.substring(temp_out.indexOf(':')+1, temp_out.length()-2).strip();
				assertEquals("Post has been succesfully added with id", temp_out.substring(0, temp_out.indexOf(':')));
				outContent.reset();
				InterWithServ.send_req(inter, "blog");
				temp_out = outContent.toString();
				m = p.matcher(temp_out);
				m.find();
				while(m.find()) {
					if(id_post.equals(m.group(1).strip()))
						found=true;
				}
				assertTrue(true);
				found=false;
				outContent.reset();
				InterWithServ.send_req(inter, "show feed");
				temp_out = outContent.toString();
				m = p.matcher(temp_out);
				m.find();
				while(m.find()) {
					if(id_post.equals(m.group(1).strip()))
						found=true;
				}
				assertTrue(true);
				found=false;
				outContent.reset();
				InterWithServ.send_req(inter, "list followers");
				p = Pattern.compile("(?<=-)(.+)");
				temp_out = outContent.toString();
				m = p.matcher(temp_out);
				while(m.find()) {
					if(username.equals(m.group(1).strip()))
						found=true;
				}
				assertTrue(true);
				found=false;
				outContent.reset();
				InterWithServ.send_req(inter, "delete "+id_post);
				assertEquals("The post has been deleted succesfully.\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "post \"this is\" \"wish you were here\"");
				temp_out = outContent.toString();
				id_post = temp_out.substring(temp_out.indexOf(':')+1, temp_out.length()-2).strip();
				assertEquals("Post has been succesfully added with id", temp_out.substring(0, temp_out.indexOf(':')));
				outContent.reset();
				InterWithServ.send_req(inter, "logout");
				assertEquals(new_user+" logged out\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "login "+username + " " +password);
				assertEquals(username + " logged in\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "show feed");
				temp_out=outContent.toString();
				p = Pattern.compile("(?<=\\n)(.+?)(?=\\|)");
				m = p.matcher(temp_out);
				m.find();
				while(m.find()) {
					if(id_post.equals(m.group(1).strip()))
						found=true;
				}
				assertTrue(true);
				found=false;
				outContent.reset();
				InterWithServ.send_req(inter, "list following");
				p = Pattern.compile("(?<=-)(.+)");
				temp_out = outContent.toString();
				m = p.matcher(temp_out);
				m.find();
				while(m.find()) {
					if(new_user.equals(m.group(1).strip()))
						found=true;
				}
				assertTrue(true);
				found=false;
				outContent.reset();
				InterWithServ.send_req(inter, "rate "+ id_post +" 1");
				assertEquals("The post was rated.\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "rewin "+id_post);
				assertEquals("Post got rewinded.\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "comment "+id_post+" \"this is a comment\"");
				assertEquals("The post was commented.\n", outContent.toString());
				outContent.reset();
				InterWithServ.send_req(inter, "unfollow "+new_user);
				assertEquals("The user: "+username+" unfollowed "+new_user+".\n", outContent.toString());
				outContent.reset();
				Thread.sleep(1000);
				InterWithServ.send_req(inter, "list following");
				p = Pattern.compile("(?<=-)(.+)");
				temp_out = outContent.toString();
				m = p.matcher(temp_out);
				while(m.find()) {
					if(new_user.equals(m.group(1).strip()))
						found=true;
				}
				assertFalse(found);
				outContent.reset();
				InterWithServ.send_req(inter, "wallet btc");
				p = Pattern.compile("(\\d.\\d+)|null");
				temp_out = outContent.toString();
				m = p.matcher(temp_out);
				assertTrue(m.find());
				outContent.reset();
				InterWithServ.send_req(inter, "wallet");
				p = Pattern.compile("(\\d.\\d+)|0");
				temp_out = outContent.toString();
				m = p.matcher(temp_out);
				assertTrue(m.find());
			}
			
		} catch (IOException e) {
			restoreStreams();
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			restoreStreams();

			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncorrectOperationException e) {
			restoreStreams();

			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			restoreStreams();

			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if(username_wrp.get_thread()!=null) {
				username_wrp.get_thread().interrupt();
				try {
					username_wrp.get_thread().join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
	}
	@AfterAll
	public static  void cleanup() {
		serv_thr.interrupt();
		try {
			try {
				Stream.of((new File((StaticNames.PATH_TO_PROFILES))).listFiles()).forEach(path -> {
					if(path.isDirectory()) {
						String file_name=null;
						file_name=path.getName().toString();
						if(file_name.startsWith("&test_user&"))
							User_Data.deleteDir(new File(StaticNames.PATH_TO_PROFILES + file_name));
					}
				});
				Stream.of((new File(StaticNames.PATH_TO_TAGS)).listFiles()).forEach(path -> {
					if(path.isDirectory()) {
						String file_name=null;
						file_name=path.getName();
						if(file_name.startsWith("&test_tag&"))
							User_Data.deleteDir(new File(StaticNames.PATH_TO_TAGS + file_name));
					}
				});
				Stream.of((new File(StaticNames_Client.PATH_TO_CLIENT)).listFiles()).forEach(path -> {
					if(path.isDirectory()) {
						String file_name=null;
						file_name=path.getName();
						if(file_name.startsWith("&test_user&"))
							User_Data.deleteDir(new File(StaticNames_Client.PATH_TO_CLIENT + file_name));
					}
				});
				Stream.of((new File(StaticNames.PATH_TO_POSTS)).listFiles()).forEach(path -> {
					if(Files.isSymbolicLink(path.toPath())) {
						try {
							if(!path.exists())Files.delete(path.toPath());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.err.println("Could not delete a symbolic link: " + e.getMessage());
						}
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Server has ended");
			serv.end_me();
			serv_thr.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
