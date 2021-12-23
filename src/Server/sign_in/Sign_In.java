/**
 * 
 */
package sign_in;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonParseException;

import user.User;
import utils.User_Data;

/**
 * @author nedo1993
 *
 */
public class Sign_In extends UnicastRemoteObject implements Sign_In_Interface {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Set<String> usernames;
	public Sign_In(int port) throws JsonParseException, IllegalArgumentException, IOException, RemoteException {
		super(port, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, null, true));
		this.usernames= ConcurrentHashMap.newKeySet();
		User_Data.load_Usernames(this.usernames);
	}
	public int register(String username, String password, String tags_arg) throws UsernameAlreadyExistsException, TooManyTagsException {
		String salt;
		StringTokenizer token;
		Tags tags=new Tags();
		User user;
		AtomicInteger status=new AtomicInteger(0);
		
		if(username == null || password == null || tags_arg == null)
			throw new IllegalArgumentException("Argument can not be null");
		if(!this.usernames.add(username))
			throw new UsernameAlreadyExistsException("User name is taken.");
		token= new StringTokenizer(tags_arg, " ");
		try {
			while(token.hasMoreTokens()) {
				tags.add_tag(token.nextToken());
			}
			salt=BCrypt.gensalt();
			user=new User(username, tags, salt, BCrypt.hashpw(password.getBytes(), salt));
		
			User_Data.add_user(user, status);
		} catch (IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TooManyTagsException e) {
			this.usernames.remove(username);
			throw new TooManyTagsException(e.getMessage());
		}
		return Sign_In_Interface.CREATED;
	}
	
}
