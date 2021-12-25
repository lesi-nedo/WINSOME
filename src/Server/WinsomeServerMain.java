import sign_in.TooManyTagsException;
import winServ.WinsomeServer;

/**
 * @author nedo1993
 *
 */
public class WinsomeServerMain {

	/**
	 * @param args
	 * @throws TooManyTagsException 
	 */
	public static void main(String[] args) throws TooManyTagsException {
		// TODO Auto-generated method stub
		int port=9999;
		String ip="192.168.1.24";
		WinsomeServer serv=new WinsomeServer(port, ip);
		serv.start_serv();
	}

}
