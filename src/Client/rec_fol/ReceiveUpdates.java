package rec_fol;

import java.util.Set;

import rec_fol.ReceiveUpdatesInterface;

public class ReceiveUpdates implements ReceiveUpdatesInterface {
	/*
	 * Overview: Implementation of the remote object
	 */
	Set<String> all_followers;
	
	//@Effects: Initializes the object
	//@Throws: RemoteException
	//@param all_followers: the followers that are currently present
	public ReceiveUpdates(Set<String> all_followers) {
		this.all_followers=all_followers;
	}
	
	//@Effects: Adds a new user to the Set of followers
	//@Throws: RemoteException
	//@param username: the name of the new follower
	public void update(String username) {
		this.all_followers.add(username);
	}
	//@Effects: Deletes a new user to the Set of followers
	//@Throws: RemoteException
	//@param username: the name of the unfollower
	public void update_unf(String username) {
		this.all_followers.remove(username);
	}
	
}
