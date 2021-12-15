/**
 * 
 */
package sign_in;

import java.rmi.Remote;
import java.util.Set;

/**
 * @author nedo1993
 *
 */
public interface Sign_In_Interface extends Remote {
	/*
	 * Overview: Interface that extends Remote so to implement sign-in process with RMI technology. 
	 */
	String register(String username, String password, Set<String> tags);

}
