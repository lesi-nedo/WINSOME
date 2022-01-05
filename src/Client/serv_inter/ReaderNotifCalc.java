package serv_inter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;


public class ReaderNotifCalc implements Runnable {

	private int port;
	private InetAddress addr;
	private byte[] leng_bytes;
	private ByteBuffer buff; 
	private int length;
	
	public ReaderNotifCalc(int port, InetAddress addr) {
		this.port=port;
		this.addr=addr;
	}
	
	@Override
	public void run() {
		try(MulticastSocket mcast = new MulticastSocket(this.port);){
			mcast.joinGroup(this.addr);
			this.leng_bytes = new byte[4];
			while(true) {
				DatagramPacket dp = new DatagramPacket(leng_bytes, leng_bytes.length);
				mcast.receive(dp);
				this.buff = ByteBuffer.wrap(this.leng_bytes);
				this.length = buff.getInt();
				this.leng_bytes = new byte[length];
				dp = new DatagramPacket(leng_bytes, leng_bytes.length);
				mcast.receive(dp);
				System.out.println(new String(dp.getData()));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
