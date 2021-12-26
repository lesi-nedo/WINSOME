import java.io.IOException;
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
	private static final String REG_NAME= "localhost";
	private static final String SER_NAME="SIGN_IN";
	private static final String SER_NAME_FOLL="UPDATED_ME";

	private static final int PORT=2021;
	/**
	 * @param args
	 * @throws TooManyTagsException 
	 */
	public static void main(String[] args) throws TooManyTagsException {
		// TODO Auto-generated method stub
		User_Data.setSettings_Server(StaticNames.PATH_TO_SSL);
		int port=9999;
		String ip="192.168.1.24";
		ConcurrentMap<String, ReadWriteLock> tags_in_mem=new ConcurrentHashMap<String, ReadWriteLock>();
		ConcurrentMap<String, ReadWriteLock> usernames=new ConcurrentHashMap<String, ReadWriteLock>();
		ConcurrentMap<String, ReceiveUpdatesInterface> reg_users=new ConcurrentHashMap<String, ReceiveUpdatesInterface>();
		Sign_In sign_ser;
		SslRMIClientSocketFactory csf=new SslRMIClientSocketFactory();
		SslRMIServerSocketFactory ssf=new SslRMIServerSocketFactory(null, null, true);
		Registry reg;	
		FollowersInterface stub= null;
		Followers serv_fol=null;
		
		try {
			serv_fol=new Followers(reg_users);
			User_Data.load_Tags(tags_in_mem);
			User_Data.load_Usernames(usernames);
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}	
		try {
			sign_ser=new Sign_In(PORT, tags_in_mem, usernames);
			LocateRegistry.createRegistry(PORT, csf, ssf);
			reg=LocateRegistry.getRegistry(REG_NAME, PORT, new SslRMIClientSocketFactory());
			reg.bind(SER_NAME, sign_ser);
			stub= (FollowersInterface)UnicastRemoteObject.exportObject(serv_fol,0);
			reg.bind(SER_NAME_FOLL, stub);
		} catch (Exception e) {
			// TODO Auto-generated catch block
            System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
		WinsomeServer serv=new WinsomeServer(port, ip);
		serv.start_serv();
	}

}
