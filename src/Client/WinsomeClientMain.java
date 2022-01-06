import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;
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
	private static final String[] CONFS= {"SERVER", "TCPPORT", "REGHOST", "REGPORT", "TIMEOUT", "NAME_SIGN_REG", "NAME_CALLBACK_UPFOL"}; //all configurations accepted
	private static String DEFAULT_SERV="192.168.1.24";
	private static int DEFAULT_TCPPORT=6666;
	private static String DEFAULT_REGHOST="localhost";
	private static int DEFAULT_REGPORT=7777;
	private static int DEFAULT_TIMEOUT=1000;
	private static int FAILURE_STAT_CODE=0;
	private static String DEFAULT_NAME_SIGN_REG="SIGN_IN";
	private static String DEFAULT_NAME_CALLBACK_UPFOL="UPDATED_ME";
	private static String EXIT_CMD="exit";
	
	public static void main(String[] arg) {
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
		BufferedReader cons_reader = null;
		AtomicBoolean exit = new AtomicBoolean(false);
		String msg = null;
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
				}
			}
			
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
