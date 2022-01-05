import java.io.BufferedReader;
import java.io.File;
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import notify_client.FollowersInterface;
import par_file.EmptyFileException;
import par_file.IllegalFileFormatException;
import par_file.ParsingConfFile;
import rec_fol.ReceiveUpdates;
import rec_fol.ReceiveUpdatesInterface;
import serv_inter.IncorrectOperationException;
import serv_inter.InterWithServ;
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
	private static String CONF_FILE_NAME="client_conf.txt";
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
		File file_foll=new File(StaticNames_Client.PATH_TO_FILE_FOLL+StaticNames_Client.NAME_FILE_FOLL+System.currentTimeMillis()+".json");
		Set<String> all_followers=new HashSet<String>();
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar = null;
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

		
		try(final DatagramSocket sock = new DatagramSocket()) {
			sock.connect(InetAddress.getByName("8.8.8.8"), 10002);
			IP = sock.getLocalAddress().getHostAddress();
			ParsingConfFile confs=new ParsingConfFile(CONF_FILE_NAME, CONFS);
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
			System.exit(FAILURE_STAT_CODE);
		}
		if(!file_foll.exists()) {
			System.out.println("Could not find the json file with all the followers: " + StaticNames_Client.NAME_FILE_FOLL);
			System.exit(FAILURE_STAT_CODE);
		}
		try( SocketChannel cl_sk = SocketChannel.open(new InetSocketAddress(SERVER, TCPPORT)); ) {
			jsonPar = jsonFact.createParser(file_foll);
			JsonToken tok = jsonPar.nextToken();
			if(tok != null) {
				while(jsonPar.nextToken() != JsonToken.END_ARRAY) {
					String foll = jsonPar.getText();
					all_followers.add(foll);
				}
			}
			jsonPar.close();
			
			registry = LocateRegistry.getRegistry(REGHOST, REGPORT);
			sign_r = (Sign_In_Interface) registry.lookup(NAME_SIGN_REG);
			upd_foll_r = (FollowersInterface) registry.lookup(NAME_CALLBACK_UPFOL);
			callObj = new ReceiveUpdates(all_followers);
			stub = (ReceiveUpdatesInterface) UnicastRemoteObject.exportObject(callObj, 0);
			inter = new InterWithServ(sign_r, cl_sk, IP, exit, upd_foll_r, stub);
			cons_reader=new BufferedReader(new InputStreamReader(System.in));
			while(!exit.get()) {
				msg = cons_reader.readLine().trim();
				if(msg.equals(EXIT_CMD)){
					exit.set(true);
					continue;
				}
				InterWithServ.send_req(inter, msg);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IncorrectOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			
		
	}
}
