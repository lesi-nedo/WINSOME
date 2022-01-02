package notify_client;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.concurrent.ConcurrentMap;

import notify_client.FollowersInterface;
import rec_fol.ReceiveUpdatesInterface;

public class Followers extends RemoteServer implements FollowersInterface {
	/**
	 * Overview: The remote Object that lets clients register for the callback
	 */
	private static final long serialVersionUID = 1L;
	ConcurrentMap<String, ReceiveUpdatesInterface> users;
	
	//@Requires: users != null
	//@Throws: IllegalArgumentException, RemoteException
	//@Effects: initializes the Followers object
	//@param users: the concurrent data structure that holds all client that have signed for the updates
	public Followers(ConcurrentMap<String, ReceiveUpdatesInterface> users) throws RemoteException {
		super();
		if(users==null)
			throw new IllegalArgumentException();
		this.users=users;
	}
	//@Requires: username != null c != null
	//@Throws: IllegalArgumentException RemoteException
	//@Modifies: this.users
	//@Effects: enrolls the client for the callbacks
	//@Returns: void
	//@param username: the name of the client to be enrolled
	//@param c: the client's stub
	public void registerMe(String username, ReceiveUpdatesInterface c) throws RemoteException {
		if(username == null ||c == null )
			throw new IllegalArgumentException();
		this.users.putIfAbsent(username, c);
	}
	//@Requires: username != null c != null
	//@Throws: IllegalArgumentException RemoteException
	//@Modifies: this.users
	//@Effects: delists the client from the callbacks
	//@Returns: void
	//@param username: the name of the client to be enrolled
	//@param c: the client's stub
	public void deregisterMe(String username, ReceiveUpdatesInterface c) throws RemoteException {
		if(username == null ||c == null )
			throw new IllegalArgumentException();
		this.users.remove(username);
	}
	//@Requires: new_user != null who_upd != null
	//@Throws: IllegalArgumentException RemoteException
	//@Effects: updates the client that a new user started to follow 
	public void update(String new_user, String who_upd) throws RemoteException {
		if(new_user == null || who_upd == null)
			throw new IllegalArgumentException();
		this.users.get(who_upd).update(new_user);
	}
	
	//@Requires: new_user != null who_upd != null
	//@Throws: IllegalArgumentException RemoteException
	//@Effects: updates the client that a user unfollowed 
	public void update_unfollowed(String unf_user, String who_upd) throws RemoteException {
		if(unf_user == null || who_upd == null)
			throw new IllegalArgumentException();
		this.users.get(who_upd).update_unf(unf_user);
	}
	
}
