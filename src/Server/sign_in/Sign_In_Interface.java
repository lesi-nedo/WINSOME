/**
 * 
 */
package sign_in;

import java.rmi.Remote;
import java.rmi.RemoteException;
/**
 * @author nedo1993
 *
 */
public interface Sign_In_Interface extends Remote {
	/*
	 * Overview: Interface that extends Remote so to implement sign-in process with RMI technology. 
	 */
	public static int MAX_LEN_PASS_BYTES=72;
	public static int CREATED=201;
	public int register(String username, String password, String tags) throws IllegalArgumentException, RemoteException, UsernameAlreadyExistsException, TooManyTagsException;

}
