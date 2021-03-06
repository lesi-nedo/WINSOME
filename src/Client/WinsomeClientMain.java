import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.rmi.ssl.SslRMIClientSocketFactory;

import notify_client.FollowersInterface;
import par_file.EmptyFileException;
import par_file.IllegalFileFormatException;
import par_file.ParsingConfFile;
import rec_fol.ReceiveUpdates;
import rec_fol.ReceiveUpdatesInterface;
import serv_inter.IncorrectOperationException;
import serv_inter.InterWithServ;
import serv_inter.UsernameWrp;
import sign_in.Sign_In_Interface;
import utils.StaticMethods;
import utils.StaticNames_Client;

public class WinsomeClientMain {
	/*
	 * Overview: implementation of the client 
	 */
	
	public static final String[] CONFS= {"SERVER", "TCPPORT", "REGHOST", "REGPORT", "TIMEOUT", "NAME_SIGN_REG", "NAME_CALLBACK_UPFOL", "NAME_NET_INTER"}; //all configurations accepted
	private static final  String DEFAULT_SERV="192.168.1.24";
	private static final int DEFAULT_TCPPORT=6666;
	private static final String DEFAULT_REGHOST="localhost";
	private static final int DEFAULT_REGPORT=7777;
	private static final int DEFAULT_TIMEOUT=1000;
	private static final int FAILURE_STAT_CODE=0;
	private static final String DEFAULT_NAME_SIGN_REG="SIGN_IN";
	private static final String DEFAULT_NAME_CALLBACK_UPFOL="UPDATED_ME";
	private static final String EXIT_CMD="exit";
	private static final String DEFAULT_NAME_NET_INTER="wlo1";
	
	public static void main(String[] arg) {
		String conf = null; //holds the configuration value
		int REGPORT =0;//porto associated to the registry
		String REGHOST=null; //address of the registry
		String SERVER=null; //address of the server
		String NAME_SIGN_REG=null;//the name in the registry of the sign up service 
		String NAME_CALLBACK_UPFOL=null;//the name of the updates about new followers
		int TCPPORT=0;//port associated to the server
		int TIMEOUT = 0;//timeout for the socket
		String NAME_NET_INTER=""; //the name of the network interface
		Set<String> all_followers= (new ConcurrentHashMap<>()).newKeySet(); //in this variable are stored all followers than when a user logs out it gets transferred to a json file
		Sign_In_Interface sign_r; //the interface of the service
		Registry registry = null;
		FollowersInterface upd_foll_r;
		BufferedReader cons_reader = null;
		AtomicBoolean exit = new AtomicBoolean(false);//if true than the server will terminated
		String msg = null;//holda the input from the human
		String IP = null;//holds the ip associated to the machine, it is used for the header HOST
		ReceiveUpdatesInterface stub;
		ReceiveUpdatesInterface callObj;
		InterWithServ inter;
		UsernameWrp username_wrp=new UsernameWrp();//this variable holds the current log user with the thread that has joined the group for the multicast.
		
		try(final DatagramSocket sock = new DatagramSocket()) {
			//gets the host ip
			sock.connect(InetAddress.getByName("8.8.8.8"), 10002);
			IP = sock.getLocalAddress().getHostAddress();
			//parse the configuration file
			ParsingConfFile confs=new ParsingConfFile(StaticNames_Client.NAME_CONF_FILE, CONFS);
			//sets the parameters for the sslSocket
			StaticMethods.setSettings_client(StaticNames_Client.PATH_TO_SSL);
			//Saves all the configuration in variables from the conf_file.txt
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
			conf = confs.getConf("NAME_NET_INTER");
			NAME_NET_INTER = conf== null ? DEFAULT_NAME_NET_INTER : conf;
		} catch (IOException | EmptyFileException | IllegalFileFormatException e) {
			// TODO Auto-generated catch block
			System.out.println("Please fix the configuration file than rerun me.");
			e.printStackTrace();
			System.exit(FAILURE_STAT_CODE);
		}
		System.err.println("Trying to connect to the server....");
		try( SocketChannel cl_sk = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(SERVER), TCPPORT)); ) {
			System.err.println("Connected");
			registry = LocateRegistry.getRegistry(REGHOST, REGPORT, new SslRMIClientSocketFactory());
			sign_r = (Sign_In_Interface) registry.lookup(NAME_SIGN_REG);
			upd_foll_r = (FollowersInterface) registry.lookup(NAME_CALLBACK_UPFOL);
			callObj = new ReceiveUpdates(all_followers);
			stub = (ReceiveUpdatesInterface) UnicastRemoteObject.exportObject(callObj, 0);
			inter = new InterWithServ(sign_r, cl_sk, IP, upd_foll_r, stub, username_wrp, all_followers, TIMEOUT, NAME_NET_INTER);
			cons_reader=new BufferedReader(new InputStreamReader(System.in));
			while(!exit.get()) {
				System.out.println("Ready: ");
				msg = cons_reader.readLine().trim();
				if(msg.equals(EXIT_CMD)){
					exit.set(true);
					continue;
				}
				try {
					InterWithServ.send_req(inter, msg);
				} catch (IncorrectOperationException e) {
					// TODO Auto-generated catch block
					System.err.println(e.getMessage());
				} catch (IllegalStateException e) {
					System.err.println(e.getMessage() + " --> probably arguments are incorrect");
				}
			}
			
		} catch(ConnectException e) {
			System.err.println("Server is down.");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  finally {
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
		return;
		
	}
}
