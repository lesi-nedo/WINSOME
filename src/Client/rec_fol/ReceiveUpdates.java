package rec_fol;

import java.util.Set;

import rec_fol.ReceiveUpdatesInterface;

public class ReceiveUpdates implements ReceiveUpdatesInterface {
	Set<String> all_followers;
	
	public ReceiveUpdates(Set<String> all_followers) {
		this.all_followers=all_followers;
	}
	
	public void update(String username) {
		this.all_followers.add(username);
	}
	
	public void update_unf(String username) {
		this.all_followers.remove(username);
	}
	
}
