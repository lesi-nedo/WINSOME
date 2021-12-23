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
	private StringTokenizer token;
	private Tags tags;
	private User user;
	private String salt;
	private Set<String> usernames;
	public Sign_In(int port) throws JsonParseException, IllegalArgumentException, IOException, RemoteException {
		super(port, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, null, true));
		this.usernames= ConcurrentHashMap.newKeySet();
		this.tags=new Tags();
		User_Data.load_Usernames(this.usernames);
	}
	public int register(String username, String password, String tags) throws UsernameAlreadyExistsException, TooManyTagsException {
		if(username == null || password == null || tags == null)
			throw new IllegalArgumentException("Argument can not be null");
		if(!this.usernames.add(username))
			throw new UsernameAlreadyExistsException("User name is taken.");
		this.token= new StringTokenizer(tags);
		while(this.token.hasMoreTokens())
			this.tags.add_tag(this.token.nextToken());
		this.salt=BCrypt.gensalt();
		this.user=new User(username, this.tags, this.salt, BCrypt.hashpw(password.getBytes(), this.salt));
		try {
			User_Data.add_user(this.user);
		} catch (IllegalArgumentException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Sign_In_Interface.CREATED;
	}
	
}
