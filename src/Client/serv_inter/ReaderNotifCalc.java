package serv_inter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
<<<<<<< HEAD
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
=======
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)


public class ReaderNotifCalc implements Runnable {
	/*
	 * Overview: thread that joins the multicast group
	 */
	public static int DGRAM_BUFF_SIZE=8192;
<<<<<<< HEAD
=======
	public static final String NETINTER="wlan1";
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)
	private int port;
	private InetAddress addr;
	private byte[] leng_bytes;
	private boolean end;
	private int timeout;
<<<<<<< HEAD
	MulticastSocket mcast;
	public ReaderNotifCalc(int port, InetAddress addr, int timeout) {
=======
	private MulticastSocket mcast;
	private String NAME_NET_INTER;
	public ReaderNotifCalc(int port, InetAddress addr, int timeout, String NAME_NET_INTER) {
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)
		this.port=port;
		this.addr=addr;
		this.timeout=timeout;
		this.end=false;
<<<<<<< HEAD
=======
		this.NAME_NET_INTER=NAME_NET_INTER;
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)
	}
	
	@Override
	public void run() {
		try{
			mcast = new MulticastSocket(this.port);
<<<<<<< HEAD
			mcast.joinGroup(this.addr);
=======
			NetworkInterface netIf = NetworkInterface
					.getByName(NAME_NET_INTER);
			mcast.joinGroup(new InetSocketAddress(this.addr, this.port), netIf);
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)
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
<<<<<<< HEAD
				System.err.println("Received: " +new String(dp.getData(), dp.getOffset(), dp.getLength()));
=======
//				System.err.println("Received: " +new String(dp.getData(), dp.getOffset(), dp.getLength()));
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)
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
<<<<<<< HEAD
			this.end=true;
			this.mcast.leaveGroup(this.addr);
=======
			NetworkInterface netIf = NetworkInterface
					.getByName(NAME_NET_INTER);
			this.end=true;
			this.mcast.leaveGroup(new InetSocketAddress(this.addr, this.port), netIf);
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
