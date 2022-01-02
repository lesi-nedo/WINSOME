import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.http.ProtocolVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.io.DefaultHttpRequestParser;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.SessionInputBufferImpl;
import org.apache.http.message.BasicHttpResponse;

import com.fasterxml.jackson.core.JsonParseException;

import notify_client.Followers;
import notify_client.FollowersInterface;
import par_file.EmptyFileException;
import par_file.IllegalFileFormatException;
import par_file.ParsingConfFile;
import rec_fol.ReceiveUpdatesInterface;
import sign_in.Sign_In;
import sign_in.TooManyTagsException;
import sign_in.UsernameAlreadyExistsException;
import utils.StaticNames;
import utils.User_Data;
import winServ.ShutTheServ;
import winServ.WinsomeServer;

/**
 * @author nedo1993
 *
 */
public class WinsomeServerMain {
	private static final String SER_NAME="SIGN_IN"; //name of the RMI service that unable the sign in
	private static final String SER_NAME_FOLL="UPDATED_ME";  //name of the RMI callback service
	private static final String[] CONFS= {"SERVER", "TCPPORT", "UDPPORT", "MULTICAST", "MCASTPORT", "REGHOST", "REGPORT", "TIMEOUT", "GAINPERIOD", "BUFF_LIMIT", "REWARD_AUTHOR"}; //all configurations accepted
	private static final String DEFAULT_SERVER="192.168.1.24";
	private static final int DEFAULT_TCPPORT=6666;
	private static final int DEFAULT_UDPPORT=33333;
	private static final String DEFAULT_MULTICAST="239.255.32.32";
	private static final int DAFAULT_MCASTPORT=44444;
	private static final String DEFAULT_REGHOST="localhost";
	private static final int DEFAULT_REGPORT=7777;
	private static final int DEFAULT_TIMEOUT=100000;
	private static final int DEFAULT_GAINPERIOD=60; //in seconds
	private static final int DEFAULT_BUFF_LIMIT= 8*1024*1024;
	private static final float DEFAULT_REWARD_AUTHOR = 0.70f;
	/**
	 * @param args
	 * @throws TooManyTagsException 
	 */
	public static void main(String[] args) throws TooManyTagsException {
		// TODO Auto-generated method stub
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
			int UDPPORT = conf == null ? DEFAULT_UDPPORT : Integer.valueOf(conf);
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
			HttpTransportMetricsImpl metrics= new HttpTransportMetricsImpl();
			SessionInputBufferImpl ses_inp = new SessionInputBufferImpl(metrics, 10);
			ses_inp.bind(new ByteArrayInputStream(("POST /echo HTTP/1.1\r\n" +  "Host: reqbin.com\r\nContent-Type: application/json; charset=UTF-16\r\nAccept: */*\r\n" + "Content-Length: "+ "{\"suka\":\"ddd\"}".getBytes().length + "\r\n\r\n" + "{\"suka\":\"ddd\"}").getBytes()));
			DefaultHttpRequestParser req_par = new DefaultHttpRequestParser(ses_inp);
			String str ="/ddf";
			InputStream cont_stream= new ByteArrayInputStream("suka blat".getBytes());
			BasicHttpResponse req = new BasicHttpResponse(new ProtocolVersion("HTTP", 1,1), 200, "SSS");
			req.setHeader("Cookie", "PHPSESSID=298zf09hf012fh2; csrftoken=u32t4o3tb3gg43; _gat=1");
			System.out.println(req.toString().length());
			BasicHttpEntity entity = new BasicHttpEntity();
			entity.setContent(cont_stream);
			req.setEntity(entity);
			System.out.println(req.toString().length());
			
			
			serv_fol=new Followers(users_to_upd);
			//loads from the folder User_Data all tags and usernames
			User_Data.load_Tags(tags_in_mem);
			User_Data.load_Usernames(usernames);
			
			sign_ser=new Sign_In(REGPORT, tags_in_mem, usernames);//initialization of the remote object
//			sign_ser.register("oleksiy", "password", "python javascript");
//			sign_ser.register("marco", "password", "python css");
//			sign_ser.register("filippo", "password", "ocaml haske");

			LocateRegistry.createRegistry(REGPORT, csf, ssf);
			reg=LocateRegistry.getRegistry(REGHOST, REGPORT, new SslRMIClientSocketFactory());
			reg.bind(SER_NAME, sign_ser);
			stub= (FollowersInterface)UnicastRemoteObject.exportObject(serv_fol,0);
			reg.bind(SER_NAME_FOLL, stub);
			WinsomeServer serv=new WinsomeServer(TCPPORT, SERVER, BUFF_LIMIT, users_to_upd, tags_in_mem, usernames);//initialization of the server
			Runtime.getRuntime().addShutdownHook(new ShutTheServ(serv));
			serv.initMcastVars(MCASTPORT, GAINPERIOD, m_group, reward_author);
			serv.start_serv(TIMEOUT);
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

}
