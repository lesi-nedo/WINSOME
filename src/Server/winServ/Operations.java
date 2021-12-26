package winServ;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import sign_in.Tags;
import sign_in.TooManyTagsException;
import utils.StaticNames;
import utils.User_Data;

public class Operations {
	/*
	 * Overview: It holds all the methods useful to the interaction client-server
	*/
	private static final int LENGTH_OF_POST_ID=10; 
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//@Requires: username != null password != null usernames != null logged_users != null
	//@Throws: IllegalArgumentException, JsonParseException, IOException
	//@Modifies: logged_users
	//@Effects: checks if the user exists than add to the concurrent data structure to keep truck of who is logged
	//Returns: Result holds a boolean if true than the user was logged false otherwise and a reason 
	//@param username: the name of the user to be logged
	//@param password: the password of the user
	//@param usernames: the concurrent data structure that holds all username of the social network with relative locks
	//@param logged_users: the concurrent data structure that holds all username and sessionId of the logged users
	public static Result login(String username, String password, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, String> logged_users) throws JsonParseException, IOException {
		if(username == null || password == null || usernames == null || logged_users == null) 
			throw new IllegalArgumentException();
		File user=new File(StaticNames.PATH_TO_PROFILES+username+"/"+StaticNames.NAME_JSON_USER);
		String hash_pas=null;
		String username_json=null;
		ReadWriteLock rw_lock=null;
		Lock lock=null;
		JsonFactory jsonFact= new JsonFactory();
		
		if((rw_lock=usernames.get(username)) == null)
			return new Result(false, "Username does not exists");
		lock=rw_lock.readLock();
		try {
			lock.lock();
			if(user.exists()) {
				JsonParser jsonPar=jsonFact.createParser(user);
				try{
					while(jsonPar.nextToken() != JsonToken.END_OBJECT) {
						String tok=jsonPar.getCurrentName();
						if(tok!=null && tok.equals("user_name")) {
							jsonPar.nextToken();
							username_json=jsonPar.getText();
							assert username_json == username;
						}
						if(tok!=null && tok.equals("hashed_password")) {
							jsonPar.nextToken();
							hash_pas=jsonPar.getValueAsString();
							if(BCrypt.checkpw(password,hash_pas)) {
								logged_users.putIfAbsent(username, getSessionId());
								return new Result(true, "Correct password");
							} else return new Result(false, "Incorrect Password");
						}
					}
				}finally {
					jsonPar.close();
				}
			} else {
				return new Result(false, "The user has been removed");
			}
		} finally {
			lock.unlock();
		}
		return new Result(false, "Server has bugs");
	}
	
	//@Effects: generates pseudo-random string
	//@Returns: string of length 16
	public static String getSessionId() {
        return UUID.randomUUID().toString().substring(1, 16).replace("-", "");
    }
	
	//@Requires: username != null logged_users != null
	//@Effects: logs out the user
	//@Throws: IllegalArgumentException
	//@Modifies: logged_users
	//@Returns: Result holds a boolean, if true than the user has been logged out false if the user wasn't logged
	//@param username: the name of the user logged in
	//@param logged_users: all users that are currently logged_in
	public static Result logout(String username, ConcurrentMap<String, String> logged_users) {
		if(logged_users == null || username == null)
			throw new IllegalArgumentException();
		if(logged_users.remove(username)!=null)
			return new Result(true, "The user have successfully logged out");
		return new Result(false, "The user was not logged in");
	}
	
	//@Requires: username != null usernames != null tags_in_mem != null 
	//@Throws: IllegalArgumentEsception, IOException, TooManyTagsException
	//@Effects: lists all users that have at least one tag in common with the user identified by the username parameter
	//@Returns: a json string in format object with field as usernames and values as array of all tags specified by the user, null if the user was deleted in meantime
	//@param username: the name of the user who requests a look up of the users
	//@param usernames: the concurrent hashMap that holds all usernames registered and locks
	//@param tags_in_mem: all tags specified by the users
	public static String list_users(String username, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, ReadWriteLock> tags_in_mem) throws IOException, TooManyTagsException {
		if(username == null || usernames == null || tags_in_mem == null)
			throw new IllegalArgumentException();
		File user=new File(StaticNames.PATH_TO_PROFILES+username+"/"+StaticNames.NAME_JSON_USER);
		ReadWriteLock rw_lock=null;
		TreeSet<String> user_to_ret = new TreeSet<String>();
		String result="{";
		if((rw_lock=usernames.get(username)) == null)
			return null;
		
		Tags tags=getUserTags(user, rw_lock.readLock());
		Iterator<String> tags_iter=null;

		
		if(tags == null)
			return null;
		tags_iter=tags.iterator();
		while(tags_iter.hasNext()) {
			String tag = tags_iter.next();
			getAllUsers(new File(StaticNames.PATH_TO_TAGS+tag+"/"+StaticNames.NAME_FILE_TAG), tags_in_mem.get(tag).readLock(), usernames, tags_in_mem, user_to_ret);
		}
		user_to_ret.remove(username);
		System.out.println(user_to_ret.size());
		tags_iter=user_to_ret.iterator();
		if(tags_iter.hasNext()) {
			String part_user=tags_iter.next();
			Iterator<String> tags_part_user=null;
			
			result=result.concat("\""+part_user+"\":[");
			tags=getUserTags(new File(StaticNames.PATH_TO_PROFILES+part_user+"/"+StaticNames.NAME_JSON_USER), usernames.get(part_user).readLock());
			if(tags==null)
				return null;
			tags_part_user=tags.iterator();
			
			String curr_tag=tags_part_user.next();
			result=result.concat("\""+curr_tag+"\"");
			while(tags_part_user.hasNext()) {
				curr_tag=tags_part_user.next();
				result=result.concat(", \""+curr_tag+"\"");
			}
			result=result.concat("]");
		}
		while(tags_iter.hasNext()) {
			String part_user=tags_iter.next();
			Iterator<String> tags_part_user=null;
			
			result=result.concat(", \""+part_user+"\":[");
			tags=getUserTags(new File(StaticNames.PATH_TO_PROFILES+part_user+"/"+StaticNames.NAME_JSON_USER), usernames.get(part_user).readLock());
			if(tags == null)
				return null;
			tags_part_user=tags.iterator();
			
			String curr_tag=tags_part_user.next();
			result=result.concat("\""+curr_tag+"\"");
			while(tags_part_user.hasNext()) {
				curr_tag=tags_part_user.next();
				result=result.concat(", \""+curr_tag+"\"");
			}
			result=result.concat("]");
		}
		result=result.concat("}");
		return result;
	}
	
	
	//@Requires: username != null, lock !=null
	//@Throws IllegalArgumentException IOException
	//@Effects: lists all the users followed by the user
	//@Returns: the json string in format array with all users, null if the user was deleted in meantime
	//@param username: the name of the user
	public static String list_following(String username, ReadWriteLock lock_r) throws IOException {
		if(username == null || lock_r == null)
			throw new IllegalArgumentException();
		Lock lock=lock_r.readLock();
		var wrapper = new Object() { String result="["; };
		try {
			lock.lock();
			if(!(new File(StaticNames.PATH_TO_PROFILES+username)).exists())
				return null;
			Stream.of((new File(StaticNames.PATH_TO_PROFILES+username+"/" +"Following")).listFiles()).forEach(path -> {
				if(path.isDirectory()) {
					wrapper.result=wrapper.result.concat(", \""+path.getName()+"\"");
				}
			});
		} finally {
			lock.unlock();
		}
		wrapper.result=wrapper.result.replaceFirst(", ", "");
		wrapper.result=wrapper.result.concat("]");
		return wrapper.result;
	}
	
	//@Requires: username != null follow_user != null lock_user_r != null lock_user_to_follow_r != null
	//@Throws: IllegalArgumentException IOException
	//@Modifies: the directory some/path/username/Following and some/path/follow_user/Followers
	//@Effects: creates a symbolic link of the user followed in the folder some/path/username/Following
	//			and symbolic link of the user's foleder in some/path/follow_user/Followers
	//@Returns: Result that holds a boolean, true if the username started to follow follow_user, false otherwise
	//@param username: the name of the user who wants to follow
	//@param follow_user: the name of the user who will be followed
	//@param lock_user_r: the Read lock associated to the user's folder
	//@param lock_usere_to_follow_r: the Read lock associated to the followee folder
	public static Result follow_user(String username, String follow_user, ReadWriteLock lock_user_r, ReadWriteLock lock_user_to_follow_r) throws IOException {
		if(username == null || follow_user==null || lock_user_r == null || lock_user_to_follow_r==null)
			throw new IllegalArgumentException();
		Lock user_lock = lock_user_r.readLock();
		Lock user_to_follow_lock=lock_user_to_follow_r.readLock();
		try {
			user_lock.lock();
			user_to_follow_lock.lock();
			Path user_to_follow=Paths.get(StaticNames.PATH_TO_PROFILES+follow_user);
			if(!user_to_follow.toFile().exists())
				return new Result(false, "User to follow has been deleted");
			user_to_follow=user_to_follow.toAbsolutePath();
			Path user=Paths.get(StaticNames.PATH_TO_PROFILES+username+"/"+"Following"+"/"+follow_user);
			if(!(new File(StaticNames.PATH_TO_PROFILES+username)).exists())
				return new Result(false, "The user has been deleted");
			Files.createSymbolicLink(user, user_to_follow);
			user=Paths.get(StaticNames.PATH_TO_PROFILES+username);
			user=user.toAbsolutePath();
			user_to_follow=Paths.get(StaticNames.PATH_TO_PROFILES+follow_user+"/"+"Followers"+"/"+username);
			Files.createSymbolicLink(user_to_follow, user);
			return new Result(true, "The user: " + username + " now follows " + follow_user);
		}catch (FileAlreadyExistsException e) {
			return new Result(false, "The user: " + username + " already follows " + follow_user);
		} finally {
			user_lock.unlock();
			user_to_follow_lock.unlock();
		}
	}
	
	
	//@Requires: username != null unfollow_user != null lock_user_r != null lock_user_to_unfollow_r != null
		//@Throws: IllegalArgumentException IOException
		//@Modifies: the directory some/path/username/Following and some/path/follow_user/Followers
		//@Effects: removes the symbolic link of the user followed in the folder some/path/username/Following
		//			and symbolic link of the user's foleder in some/path/follow_user/Followers
		//@Returns: Result that holds a boolean, true if the username unfollowed follow_user, false otherwise
		//@param username: the name of the user who wants to unfollow
		//@param unfollow_user: the name of the user who will be unfollowed
		//@param lock_user_r: the Read lock associated to the user's folder
		//@param lock_usere_to_unfollow_r: the Read lock associated to the followee folder
		public static Result unfollow_user(String username, String unfollow_user, ReadWriteLock lock_user_r, ReadWriteLock lock_user_to_unfollow_r) throws IOException {
			if(username == null || unfollow_user==null || lock_user_r == null || lock_user_to_unfollow_r==null)
				throw new IllegalArgumentException();
			Lock user_lock = lock_user_r.readLock();
			Lock user_to_unfollow_lock=lock_user_to_unfollow_r.readLock();
			try {
				user_lock.lock();
				user_to_unfollow_lock.lock();
				File user_to_unfollow=new File(StaticNames.PATH_TO_PROFILES+unfollow_user+"/"+"Followers/"+username);
				File user=new File(StaticNames.PATH_TO_PROFILES+username+"/"+"Following/"+unfollow_user);
				if(!user.exists())
					return new Result(false, "The user was not a follower of " + unfollow_user);
				user.delete();
				user_to_unfollow.delete();
				return new Result(true, "The user: " + username + " unfollowed " + unfollow_user);
			} finally {
				user_lock.unlock();
				user_to_unfollow_lock.unlock();
			}
		}
		
	
	//@Throws: JsonParseException IOException TooManyTagsEsception
	//@Effects: recovers all tags specified by the user 
	//@Returns: all the tags 
	//@param user: the directory associated to the user
	//@param lock: lock associated to the user's folder
	private static Tags getUserTags(File user, Lock lock) throws JsonParseException, IOException, TooManyTagsException {
		JsonFactory jsonFact= new JsonFactory();
		try {
			lock.lock();
			if(user.exists()) {
				JsonParser jsonPar=jsonFact.createParser(user);
				try{
					while(jsonPar.nextToken() != JsonToken.END_OBJECT) {
						String tok=jsonPar.getCurrentName();
						if(tok!=null && tok.equals("tags")) {
							jsonPar.nextToken();
							return new Tags(jsonPar.getText(), " ");
						}
					}
				}finally {
					jsonPar.close();
				}
			} else {
				return null;
			}
		} finally {
			lock.unlock();
		}
		assert true;
		System.out.println("ERROR in getUserTags: this message should not have been printed, there are bugs");
		System.exit(0);
		return null;
	}
	
	
	//@Throws: IOException JsonParseException
	//@Effects: recovers all names of the users who have specified the tag
	//@param tag_file_json: the json file that sits in the tag folder
	//@param lock: lock associated to the tag's directory
	//@param useranames: all usernames of the social network 
	//@param tags_in_mem: all tags present
	//@param users_to_ret: holds all users that have specified the tag
	private static void getAllUsers(File tag_file_json, Lock lock, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, ReadWriteLock> tags_in_mem, TreeSet<String> users_to_ret) throws JsonParseException, IOException {
		JsonFactory jsonFact= new JsonFactory();
		try {
			lock.lock();
			if(tag_file_json.exists()) {
				JsonParser jsonPar=jsonFact.createParser(tag_file_json);
				try{
					jsonPar.nextToken();
					while(jsonPar.nextToken() != JsonToken.END_ARRAY) {
						String tok=jsonPar.getValueAsString();
						if(tok!=null) {
							if(!(usernames.containsKey(tok))) {
								System.out.println(tag_file_json.getName());
								System.out.println(tok);
								lock.unlock();
								User_Data.removeUserFromTag(tok, tag_file_json.getName(), tags_in_mem);
								lock.lock();
							} else {
								users_to_ret.add(tok);
							}
						}
					}
				}finally {
					jsonPar.close();
				}
			} else {
				System.out.println("ERROR in getAllUsers: this message should not have been printed, there are bugs");
				System.exit(0);
			}
		} finally {
			lock.unlock();
		}
	}
	//@Requires: username != null lock != null
	//@Throws: IllegalArgumentException IOException
	//@Effects: retrieves all the post published by the user
	//@Returns: all post as json string in format object, null if the user was delete in meantime
	//@param username: the name of the user who has published the posts
	//@param lock: the lock associated to the user's directory
	public static String view_blog(String username, ReadWriteLock lock_r) throws IOException {
		if(username == null || lock_r == null)
			throw new IllegalArgumentException();
		Lock lock = lock_r.readLock();
		var wrapper = new Object() { String res="{"; };
		String dir_name=StaticNames.PATH_TO_PROFILES+username;
		try {
			lock.lock();
			if(!(new File(dir_name).exists())){
				return null;
			}
			dir_name=dir_name+"/"+"Posts";
			Stream.of((new File(dir_name)).listFiles()).forEach(path -> {
				if(path.isDirectory() && !Files.isSymbolicLink(path.toPath())) {
					String temp_res="";
					int times_f=0;

					wrapper.res=wrapper.res.concat(", \""+path.getName()+"\":{");
					JsonFactory jsonFact=new JsonFactory();
					try {
						JsonParser jsonPar=jsonFact.createParser(new File(StaticNames.PATH_TO_PROFILES+username+"/Posts/"+path.getName()+"/"+StaticNames.NAME_FILE_POST));
						jsonPar.nextToken();
						while(jsonPar.nextToken() != JsonToken.END_OBJECT) {
							String curr=jsonPar.getText();
							if(curr.equals("author") || curr.equals("title")) {
								times_f++;
								temp_res=temp_res.concat(", \""+curr+"\":");
								jsonPar.nextToken();
								curr=jsonPar.getText();
								temp_res=temp_res.concat("\""+curr+"\"");
								if(times_f==2) break;
							}
						}
						temp_res=temp_res.replaceFirst(", ", "");
						temp_res=temp_res.concat("}");
						wrapper.res=wrapper.res.concat(temp_res);
					} catch (JsonParseException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			
		} finally {
			lock.unlock();
		}
		wrapper.res=wrapper.res.replaceFirst(", ", "");
		return wrapper.res=wrapper.res.concat("}");
		
	}
	
	//@Requires: username != null title != null && 1 <= title.length <=20 content != null && 1<=content.length<=500 lock_r != null
	//@Throws: IllegalArgumentException IOException
	//@Modifes: creates a new directory in some/path/username/Posts/id_post and a json file
	//@Effects: creates a new post
	//@Returns: Result: result == true if was created false otherwise with a reason
	//@param username: the name of user who wants to create the post
	//@param title: the title of the post
	//@para content: the content of the post
	//@param lock_r: the ReadWrite lock associated to the user's folder
	public static Result createPost(String username, String title, String content, ReadWriteLock lock_r) throws IOException {
		if(username == null || title == null || content ==null || lock_r== null)
			throw new IllegalArgumentException();
		Lock lock=lock_r.readLock();
		String id_post=User_Data.generateString(LENGTH_OF_POST_ID);
		String dir_name=StaticNames.PATH_TO_PROFILES+username;
		JsonFactory jsonFact=new JsonFactory();
		JsonGenerator jsonGen = null;
		
		title=title.trim();
		content=content.trim();
		if(title.length()>20 || content.length() > 500 || title.length()==0 || content.length() == 0)
			return new Result(false, "At least one passed argument is invalid");
		try {
			lock.lock();
			File file = new File(dir_name);
			if(!file.exists())
				return new Result(false, "The user has been removed");
			while(true) {
				dir_name=dir_name+"/"+"Posts/"+id_post;
				file=new File(dir_name);
				if(!file.exists()) {
					file.mkdir();
					break;
				}
				id_post=User_Data.generateString(LENGTH_OF_POST_ID);	
			}
			file=new File(dir_name+"/"+"Thumbs_up");
			file.mkdir();
			file=new File(dir_name+"/"+"Thumbs_down");
			file.mkdir();
			file=new File(dir_name+"/" + "Comments");
			file.mkdir();
			file=new File(dir_name+"/"+StaticNames.NAME_FILE_POST);
			file.createNewFile();
			jsonGen = jsonFact.createGenerator(file, StaticNames.ENCODING);
			jsonGen.useDefaultPrettyPrinter();
			jsonGen.writeStartObject();
			jsonGen.writeStringField("id_post", id_post);
			jsonGen.writeStringField("author", username);
			jsonGen.writeStringField("title", title);
			jsonGen.writeStringField("date", FORMATTER.format(Calendar.getInstance().getTime()));
			jsonGen.writeStringField("content", content);
			jsonGen.writeNumberField("thumbs_up", 0);
			jsonGen.writeNumberField("thumbs_down", 0);
			jsonGen.writeEndObject();
			jsonGen.close();
			return new Result(true, "Post has been succesfully added");
		} finally {
			lock.unlock();
		}
	}
	//@Requires: username != null lock_r != null
	//@Throws: IllegalArgumentException, IOException, JsonParseException
	//@Effects: retrieves all posts in the own feed
	//@Returns: a json string in format object with fielnames: id_post, author, content
	//@param username: name of the user who's feed will be build
	//@param lock_r: ReadWrite lock associated to each user
	public static String show_feed(String username, ReadWriteLock lock_r) throws JsonParseException, IOException {
		if(username == null || lock_r == null)
			throw new IllegalArgumentException();
		String dir_name=StaticNames.PATH_TO_PROFILES+username;
		String res="{";
		Lock lock = lock_r.readLock();
		File dir=null;
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar=null;		
		try {
			lock.lock();
			dir=new File(dir_name);
			if(!dir.exists())
				return null;
			dir_name=dir_name+"/"+"Posts";
			dir=new File(dir_name);
			File[] files = dir.listFiles();
			for(File f: files) {
				int times_f=0;
				if(f.isDirectory()) {
					res=res.concat(", \""+f.getName()+"\":{");
					String temp_res="";
					dir = new File(dir_name +"/"+f.getName()+"/"+StaticNames.NAME_FILE_POST);
					jsonPar=jsonFact.createParser(dir);
					jsonPar.nextToken();
					while(jsonPar.nextToken()!=JsonToken.END_OBJECT) {
						String tok=jsonPar.getText();
						if(tok.equals("author") || tok.equals("title")) {
							times_f++;
							temp_res=temp_res.concat(", \""+tok+"\":");
							jsonPar.nextToken();
							tok=jsonPar.getText();
							temp_res=temp_res.concat("\""+tok+"\"");
						}
						if(times_f==2) break;
					}
					temp_res=temp_res.replaceFirst(", ", "");
					temp_res=temp_res.concat("}");
					res=res.concat(temp_res);
				}
			}
			res=res.replaceFirst(", ", "");
			res=res.concat("}");
		} finally {
			lock.unlock();
		}
		return res;
	}
}


















