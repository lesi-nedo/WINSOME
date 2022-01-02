package winServ;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.ChunkedOutputStream;
import org.apache.http.impl.io.ContentLengthOutputStream;
import org.apache.http.impl.io.DefaultHttpResponseWriter;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.impl.io.IdentityOutputStream;
import org.apache.http.impl.io.SessionOutputBufferImpl;

public class WriterMessagesToClient implements Runnable {
	private Selector sel;
	private SelectionKey key;
	
	public WriterMessagesToClient (Selector sel, SelectionKey key) {
		this.sel=sel;
		this.key=key;
	}
	
	public void run () {
		HttpTransportMetricsImpl metrics = new HttpTransportMetricsImpl();
		SocketChannel c_sk = (SocketChannel) key.channel();
		HttpWrapper resp_wrp = (HttpWrapper) key.attachment();
		if(resp_wrp != null) {
			HttpResponse resp = resp_wrp.getResp();
			SessionOutputBufferImpl out = new SessionOutputBufferImpl(metrics, (int) resp_wrp.getLength());
			ByteArrayOutputStream out_stream=new ByteArrayOutputStream(resp_wrp.getLength());
			out.bind(out_stream);
			DefaultHttpResponseWriter to_w = new DefaultHttpResponseWriter(out);
			HttpEntity ent =resp.getEntity();
			resp.addHeader(ent.getContentType());
			resp.addHeader(ent.getContentEncoding());
			resp.addHeader("Content-Length", ent.getContentLength()+"");
			
			
			try {
				to_w.write(resp);
				StrictContentLengthStrategy con_len_stra =new StrictContentLengthStrategy();
				long len = con_len_stra.determineLength(resp);
				OutputStream out_entity=null;
				if(len == ContentLengthStrategy.CHUNKED) {
					out_entity= new ChunkedOutputStream(2048, out);
				} else if(len == ContentLengthStrategy.IDENTITY) {
					out_entity = new IdentityOutputStream(out);
				} else {
					out_entity = new ContentLengthOutputStream(out, len);
				}
				if(resp.getEntity() != null) {
					resp.getEntity().writeTo(out_entity);
				}
				out_entity.close();
				out.flush();
				ByteBuffer buf = ByteBuffer.wrap(out_stream.toByteArray());
				c_sk.write(buf);
				c_sk.close();
				System.out.println(new String(buf.array()) );
				if(!buf.hasRemaining()) {
					if(!resp_wrp.getStatus() && c_sk.isConnected()) {
						buf.clear();
						c_sk.register(sel, SelectionKey.OP_READ, null);
					} else {
						c_sk.close();
					}
				}
			} catch (IOException | HttpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(0);
				try {
					key.channel().close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} finally {
				try {
					out_stream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
}
