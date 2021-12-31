package winServ;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import utils.StaticNames;

public class CalcEarningsThread implements Runnable {
	private int  port;
	private InetAddress addr;
	private ConcurrentMap<String, ReadWriteLock> usernames;
	private HashMap<String, Double> users_eatnings;
	private float reward_author;
	
	public CalcEarningsThread(int port, InetAddress addr, ConcurrentMap<String, ReadWriteLock> usernames, float reward_author) {
		this.port=port;
		this.addr=addr;
		this.usernames=usernames;
		this.users_eatnings=new HashMap<String, Double>();
		this.reward_author=reward_author;
	}
	
	@Override
	public void run() {
		File[] all_users = (new File(StaticNames.PATH_TO_PROFILES)).listFiles();//all users as an Array of files
		String user = null;
		Lock lock_write=null;
		Lock lock_read=null;
		ReadWriteLock lock_wr=null;
		
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar = null;
		JsonGenerator jsonGen = null;
		
		
		//for each user present in the folder WINSOME/src/Server/User_Data/Profiles
		for(File us: all_users){
			try {
				File[] all_posts=null; //all posts published/rewinded by the user x
				user=us.getName();
				lock_wr=this.usernames.get(user);
				lock_read=lock_wr.readLock();
				lock_write=lock_wr.writeLock();
				lock_read.lock();
				all_posts=new File(StaticNames.PATH_TO_PROFILES+user+"/Posts").listFiles();
				//for each post published by the user x present in the folder WINSOME/src/Server/User_Data/Profiles/x/Posts
				for(File post: all_posts) {
					if(Files.exists(post.toPath()) && !Files.isSymbolicLink(post.toPath())) {
						String post_name=post.getName();
						this.users_eatnings.put(user, 0.0);
						int num_old_thmb_up=0;//the number of positive reactions to the post y
						int num_old_thmb_down=0; //the number of negative reactions to the post y
						int num_old_iter=0;// the number of iterations done
						long old_last_calc=0;//timestamp of the last iteration
						int num_new_com=0;//number of new + old comments
						int num_new_thmb_up=0; //number of new thumbs_up
						int num_new_thmb_down=0;// number of new + old thumbs_donwn
						int num_new_iter;//old number of iterations +1
						long checked =0; //timestamp that indicates the last calculation
						var wrapper = new Object() { double earnings=0; }; //earnings calculated
						String tok = null;
						File post_file=null;
						var wrapper2 = new Object() { int num_cur=1; }; //the total number of curators
						double first_arg=0;//argument to the first log
						double second_arg=0;//argument to the second log
						
						post_file=new File(StaticNames.PATH_TO_PROFILES+user+"/Posts/" + post_name +"/" + StaticNames.NAME_POSTS_STAT);
						jsonPar = jsonFact.createParser(post_file);
						jsonPar.nextToken();
						//retrieves old data from file stats.json
						while(jsonPar.nextToken() != JsonToken.END_OBJECT) {
							tok = jsonPar.getText();
							if(tok.equals("last_calc")) {
								jsonPar.nextToken();
								tok=jsonPar.getText();
								old_last_calc=Long.valueOf(tok);
							} else if(tok.equals("num_thumbs_up")) {
								jsonPar.nextToken();
								tok=jsonPar.getText();
								num_old_thmb_up=Integer.valueOf(tok);
							} else if(tok.equals("num_thumbs_down")) {
								jsonPar.nextToken();
								tok=jsonPar.getText();
								num_old_thmb_down=Integer.valueOf(tok);
							} else if(tok.equals("num_iter")) {
								jsonPar.nextToken();
								tok=jsonPar.getText();
								num_old_iter=Integer.valueOf(tok);
							}
						}
						jsonPar.close();
						try {
							lock_read.unlock();
							lock_write.lock();
							checked=System.currentTimeMillis();
							File[] all_thumb_up = new File(StaticNames.PATH_TO_PROFILES+user+"/Posts/" + post_name +"/Thumbs_up").listFiles();
							File[] list_com = new File(StaticNames.PATH_TO_PROFILES+user+"/Posts/" + post_name + "/Comments").listFiles();
							File[] all_thumb_down = new File(StaticNames.PATH_TO_PROFILES+user+"/Posts/"+post_name+"/Thumbs_down").listFiles();
							num_new_thmb_down=all_thumb_down.length;
							//each post has a folder called comments each comment is stored in the folder called as the author of the comment
							//so all comments relative to a post 'y' by the user 'x' will be in the folder ../y/Comments/x
							for(File d : list_com){
								//the number of comments published by the user
								int num_coms=d.list().length;
								num_new_com =+ num_coms;
								//checks the last time modify of the folder if greater than the last time the calculation was performed
								//then a user has added a new comment 
								if(d.lastModified() >=old_last_calc) {
									this.users_eatnings.put(d.getName(), 0.0);//we add to hashmap all new commentators
									second_arg=+2/(1+Math.pow(Math.E, -(num_coms-1)));
								}
							}
							for(File th_up: all_thumb_up) {
								if(th_up.lastModified() > old_last_calc) {
									num_new_thmb_up++;
									String name_us=th_up.getName();
									this.users_eatnings.put(name_us.substring(0, name_us.lastIndexOf('.')), 0.0);
								}
							}
						} finally{
							lock_write.unlock();
							lock_read.lock();
						}
						num_new_iter=num_old_iter+1;
						first_arg=Math.max(num_new_thmb_up -(num_new_thmb_down - num_old_thmb_down), 0) +1;
						wrapper.earnings= (Math.log(first_arg)+Math.log(second_arg+1))/num_new_iter;
						wrapper2.num_cur=this.users_eatnings.size();
						wrapper2.num_cur=wrapper2.num_cur == 0? 1 :wrapper2.num_cur;
						this.users_eatnings.replaceAll((u, e)-> {
							if(u.equals(us.getName())) {
								double ear_au=wrapper.earnings*reward_author;
								wrapper.earnings =- ear_au;
								return e+ear_au;
							} else {
								return e+(wrapper.earnings/wrapper2.num_cur);
							}
						});
						
						//updates the stat file
						jsonGen = jsonFact.createGenerator(post_file, StaticNames.ENCODING);
						jsonGen.useDefaultPrettyPrinter();
						jsonGen.writeStartObject();
						jsonGen.writeNumberField("num_comments", num_new_com);
						jsonGen.writeNumberField("num_thumbs_up", num_new_thmb_up+num_old_thmb_up);
						jsonGen.writeNumberField("num_thumbs_down", num_new_thmb_down);
						jsonGen.writeNumberField("num_iter", num_new_iter);
						jsonGen.writeNumberField("last_calc", checked);
						jsonGen.writeEndObject();
						jsonGen.close();
						
					}
				}
				this.users_eatnings.forEach((u,e) ->{
					File temp_file = new File(StaticNames.PATH_TO_PROFILES+u+"temp_wall.json");
					File file = new File(StaticNames.PATH_TO_PROFILES+u+StaticNames.NAME_FILE_WALLET);
					JsonParser jsonPar2=null;
					JsonGenerator jsonGen2=null;
					try {
						jsonPar2 = jsonFact.createParser(file);
						jsonGen2= jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
						jsonPar2.nextToken();
						jsonGen2.copyCurrentEvent(jsonPar2);
						jsonGen2.writeNumberField("value", e);
						jsonGen2.writeNumberField("timestamp", System.currentTimeMillis());
						while(jsonPar2.nextToken()!=JsonToken.END_OBJECT)
							jsonGen2.copyCurrentStructure(jsonPar2);
						jsonGen2.copyCurrentEvent(jsonPar2);
						file.delete();
						temp_file.renameTo(file);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
				int len_msg=StaticNames.MSG_NOTIFY_MULTICAS.length();
				ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES+len_msg);
				buf.putInt(len_msg);
				buf.put(StaticNames.MSG_NOTIFY_MULTICAS.getBytes());
				DatagramSocket sock = new DatagramSocket();
				DatagramPacket dat = new DatagramPacket(buf.array(), buf.position(), this.addr, this.port);
				sock.send(dat);
				sock.close();
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				lock_read.unlock();
			}
			
		}
	}
	
}
