package rec_fol;

import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReceiveUpdatesInterface extends Remote {
	public void update(String username) throws RemoteException;
	public void update_unf(String username) throws RemoteException;
	public void comm_port_idd(int port, InetAddress addr) throws RemoteException;
}
