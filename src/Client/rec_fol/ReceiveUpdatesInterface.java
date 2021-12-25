package rec_fol;

import java.rmi.Remote;

public interface ReceiveUpdatesInterface extends Remote {
	public void update(String username);
}
