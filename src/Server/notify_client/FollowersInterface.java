package notify_client;

import java.rmi.Remote;

import rec_fol.ReceiveUpdatesInterface;

public interface FollowersInterface extends Remote {
	public void registerMe(String username, ReceiveUpdatesInterface c);
	public void deregisterMe(String username, ReceiveUpdatesInterface c);
}
