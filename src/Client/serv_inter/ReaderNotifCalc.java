package serv_inter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;


public class ReaderNotifCalc implements Runnable {
	public static int DGRAM_BUFF_SIZE=8192;
	private int port;
	private InetAddress addr;
	private byte[] leng_bytes;
	private boolean end;
	private int timeout;
	MulticastSocket mcast;
	public ReaderNotifCalc(int port, InetAddress addr, int timeout) {
		this.port=port;
		this.addr=addr;
		this.timeout=timeout;
		this.end=false;
	}
	
	@Override
	public void run() {
		try{
			mcast = new MulticastSocket(this.port);
			mcast.joinGroup(this.addr);
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
//				System.out.println(new String(dp.getData(), dp.getOffset(), dp.getLength()));
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} finally {
			mcast.close();
		}
	}
	
	public void leave() {
		try {
			this.end=true;
			this.mcast.leaveGroup(this.addr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
