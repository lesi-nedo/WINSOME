package notify_client;

import java.util.concurrent.ConcurrentMap;

import notify_client.FollowersInterface;
import rec_fol.ReceiveUpdatesInterface;

public class Followers implements FollowersInterface {
	ConcurrentMap<String, ReceiveUpdatesInterface> users;
	public Followers(ConcurrentMap<String, ReceiveUpdatesInterface> users) {
		this.users=users;
	}
	public void registerMe(String username, ReceiveUpdatesInterface c) {
		this.users.putIfAbsent(username, c);
	}
	public void deregisterMe(String username, ReceiveUpdatesInterface c) {
		this.users.remove(username);
	}
}
