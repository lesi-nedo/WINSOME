/**
 * 
 */
package sign_in;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.springframework.security.crypto.bcrypt.BCrypt;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import user.User;
import utils.StaticNames;
import utils.User_Data;

/**
 * @author nedo1993
 *
 */
public class Sign_In extends UnicastRemoteObject implements Sign_In_Interface {
	/**
	 * Overview: the remote Objects that allows a client to register
	 */
	private static final long serialVersionUID = 1L;
	private ConcurrentMap<String, ReadWriteLock> usernames;//all previously specified usernames
	private ConcurrentMap<String, ReadWriteLock> tags_in_mem;//all tags that have been inserted
	
	//@Requires: tags_in_mem != null usernames != null
	//@Throws: IllegalaArgumentException RemoteException
	//@Effects: initializes the Sign_In object
	//@param port: the port number
	//@param tags_in_mem: all tags that have been specified
	//@param usernames: all used names
	public Sign_In(int port, ConcurrentMap<String, ReadWriteLock> tags_in_mem, ConcurrentMap<String, ReadWriteLock> usernames) throws RemoteException {
		super(port, new SslRMIClientSocketFactory(), new SslRMIServerSocketFactory(null, null, true));
		if(tags_in_mem == null || usernames == null)
			throw new IllegalArgumentException();
		this.usernames= usernames;
		this.tags_in_mem=tags_in_mem;
	}
	//@Requires: username != null password != null tags_args != null
	//			username, passwords, tags_args != " " password >= 5 username != " some_text"
	//@Throws: IllegalArgumentExcption UsernameAlreadyExistsException TooManyTagsExcption RemoteException
	//@Modifies: this.usernames this.tags_in_mem
	//@Effects: registers the new user 
	//@Returns: 201 that indicates created
	//@param username: the name of the new user
	//@param password: the password of the new user
	//@param tags_arg: the tags
	public int register(String username, String password, String tags_arg) throws UsernameAlreadyExistsException, TooManyTagsException, RemoteException {
		String salt;
		StringTokenizer token;
		Tags tags=new Tags();
		User user;
		ReentrantReadWriteLock lock_us=new ReentrantReadWriteLock();
		Lock lock = lock_us.writeLock();
		AtomicInteger status=new AtomicInteger(0);
		
		if(username == null || password == null || tags_arg == null || 
			username.startsWith(" ") || password.startsWith(" ") || password.length() < 5 || (tags_arg =tags_arg.trim()).length()==0)
			throw new IllegalArgumentException("Incorrect argument.");
		try {
			lock.lock();
			if(this.usernames.putIfAbsent(username, lock_us)!=null)
				throw new UsernameAlreadyExistsException("User name is taken.");
			token= new StringTokenizer(tags_arg, " ");
			try {
				while(token.hasMoreTokens()) {
					tags.add_tag(token.nextToken());
				}
				salt=BCrypt.gensalt();
				user=new User(username, tags, salt, BCrypt.hashpw(password.getBytes(), salt));
			
				User_Data.add_user(user, status, this.tags_in_mem);
			} catch (IllegalArgumentException | IOException e) {
				this.usernames.remove(username);
				User_Data.deleteDir(new File(StaticNames.PATH_TO_PROFILES+username));
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TooManyTagsException e) {
				this.usernames.remove(username);
				throw new TooManyTagsException(e.getMessage());
			}
		} finally {
			lock.unlock();
		}
		return Sign_In_Interface.CREATED;
	}
	
}
