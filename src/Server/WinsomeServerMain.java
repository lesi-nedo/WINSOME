import java.io.IOException;
import java.net.InetAddress;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import com.fasterxml.jackson.core.JsonParseException;

import notify_client.Followers;
import notify_client.FollowersInterface;
import par_file.EmptyFileException;
import par_file.IllegalFileFormatException;
import par_file.ParsingConfFile;
import rec_fol.ReceiveUpdatesInterface;
import sign_in.Sign_In;
import sign_in.TooManyTagsException;
import utils.StaticNames;
import utils.User_Data;
import winServ.WinsomeServer;

/**
 * @author nedo1993
 *
 */
public class WinsomeServerMain {
	private static final String SER_NAME="SIGN_IN";
	private static final String SER_NAME_FOLL="UPDATED_ME";
	private static final String[] CONFS= {"SERVER", "TCPPORT", "UDPPORT", "MULTICAST", "MCASTPORT", "REGHOST", "REGPORT", "TIMEOUT", "GAINPERIOD"};
	private static final String DEFAULT_SERVER="192.168.1.24";
	private static final int DEFAULT_TCPPORT=6666;
	private static final int DEFAULT_UDPPORT=33333;
	private static final String DEFAULT_MULTICAST="239.255.32.32";
	private static final int DAFAULT_MCASTPORT=44444;
	private static final String DEFAULT_REGHOST="localhost";
	private static final int DEFAULT_REGPORT=7777;
	private static final int DEFAULT_TIMEOUT=100000;
	private static final int DEFAULT_GAINPERIOD=60; //in seconds
	/**
	 * @param args
	 * @throws TooManyTagsException 
	 */
	public static void main(String[] args) throws TooManyTagsException {
		// TODO Auto-generated method stub
		try {
			ParsingConfFile confs=new ParsingConfFile("src/Server/conf_server.txt", CONFS);
			User_Data.setSettings_Server(StaticNames.PATH_TO_SSL);
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
			int UDPPORT = conf == null ? DEFAULT_UDPPORT : Integer.valueOf(conf);
			InetAddress m_group = InetAddress.getByName(MULTICAST);
			if(!m_group.isMulticastAddress())
				throw new IllegalArgumentException();
			
			ConcurrentMap<String, ReadWriteLock> tags_in_mem=new ConcurrentHashMap<String, ReadWriteLock>();
			ConcurrentMap<String, ReadWriteLock> usernames=new ConcurrentHashMap<String, ReadWriteLock>();
			ConcurrentMap<String, ReceiveUpdatesInterface> reg_users=new ConcurrentHashMap<String, ReceiveUpdatesInterface>();
			Sign_In sign_ser;
			SslRMIClientSocketFactory csf=new SslRMIClientSocketFactory();
			SslRMIServerSocketFactory ssf=new SslRMIServerSocketFactory(null, null, true);
			Registry reg;	
			FollowersInterface stub= null;
			Followers serv_fol=null;
			
			
			serv_fol=new Followers(reg_users);
			User_Data.load_Tags(tags_in_mem);
			User_Data.load_Usernames(usernames);
			
			sign_ser=new Sign_In(REGPORT, tags_in_mem, usernames);
			LocateRegistry.createRegistry(REGPORT, csf, ssf);
			reg=LocateRegistry.getRegistry(REGHOST, REGPORT, new SslRMIClientSocketFactory());
			reg.bind(SER_NAME, sign_ser);
			stub= (FollowersInterface)UnicastRemoteObject.exportObject(serv_fol,0);
			reg.bind(SER_NAME_FOLL, stub);
			WinsomeServer serv=new WinsomeServer(TCPPORT, SERVER);
			serv.start_serv();
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
		}
 
	}

}
