package serv_inter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;


public class ReaderNotifCalc implements Runnable {
	/*
	 * Overview: thread that joins the multicast group
	 */
	public static int DGRAM_BUFF_SIZE=8192;
	public static final String NETINTER="wlan1";
	private int port;
	private InetAddress addr;
	private byte[] leng_bytes;
	private boolean end;
	private int timeout;
	private MulticastSocket mcast;
	private String NAME_NET_INTER;
	public ReaderNotifCalc(int port, InetAddress addr, int timeout, String NAME_NET_INTER) {
		this.port=port;
		this.addr=addr;
		this.timeout=timeout;
		this.end=false;
		this.NAME_NET_INTER=NAME_NET_INTER;
	}
	
	@Override
	public void run() {
		try{
			mcast = new MulticastSocket(this.port);
			NetworkInterface netIf = NetworkInterface
					.getByName(NAME_NET_INTER);
			mcast.joinGroup(new InetSocketAddress(this.addr, this.port), netIf);
			while(!this.end) {
				this.leng_bytes = new byte[DGRAM_BUFF_SIZE];
				DatagramPacket dp = new DatagramPacket(leng_bytes, leng_bytes.length);
				mcast.setSoTimeout(timeout);
				try {
					mcast.receive(dp);
				} catch(SocketTimeoutException e) {
					if(Thread.interrupted()) {
						break;
					} else {
						continue;
					}
				}
//				System.err.println("Received: " +new String(dp.getData(), dp.getOffset(), dp.getLength()));
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} finally {
			mcast.close();
		}
	}
	//@Effects: leaves the multicast group
	public void leave() {
		try {
			NetworkInterface netIf = NetworkInterface
					.getByName(NAME_NET_INTER);
			this.end=true;
			this.mcast.leaveGroup(new InetSocketAddress(this.addr, this.port), netIf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
