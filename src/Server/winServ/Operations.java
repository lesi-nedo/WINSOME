package winServ;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Duration;
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
	private static final int LENGTH_OF_POST_ID=12; 
	private static final int LENGTH_OF_COMMENT_ID=12; 
	private static final String URL_TO_RAND="https://www.random.org/decimal-fractions/?num=1&dec=10&col=1&format=plain&rnd=new";
	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//@Requires: username != null password != null usernames != null logged_users != null
	//@Throws: IllegalArgumentException, JsonParseException, IOException
	//@Modifies: logged_users
	//@Effects: checks if the user exists than add to the concurrent data structure to keep truck of who is logged
	//Returns:if  Result.result == 200 than the user was logged, 404, 400 or 500 otherwise and a reason 
	//@param username: the name of the user to be logged
	//@param password: the password of the user
	//@param usernames: the concurrent data structure that holds all username of the social network with relative locks
	//@param logged_users: the concurrent data structure that holds all username and sessionId of the logged users
	public static Result login(String username, String password, ConcurrentMap<String, String> logged_users, ConcurrentMap<String, ReadWriteLock> usernames) throws JsonParseException, IOException {
		if(username == null || password == null || usernames == null || logged_users == null) 
			throw new IllegalArgumentException("Incorrect input");
		File user=new File(StaticNames.PATH_TO_PROFILES+username+"/"+StaticNames.NAME_JSON_USER);
		String hash_pas=null;
		String username_json=null;
		ReadWriteLock rw_lock=null;
		Lock lock=null;
		JsonFactory jsonFact= new JsonFactory();
		
		if((rw_lock=usernames.get(username)) == null)
			return new Result(404, "{\"reason\":\"Username does not exists\"}");
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
							assert username_json.equals(username)==true;
						}
						if(tok!=null && tok.equals("hashed_password")) {
							jsonPar.nextToken();
							hash_pas=jsonPar.getValueAsString();
							if(BCrypt.checkpw(password,hash_pas)) {
								logged_users.putIfAbsent(username, getSessionId());
								return new Result(200, "{\"reason\":\"Correct password\"}");
							} else return new Result(400, "{\"reason\":\"Incorrect Password\"}");
						}
					}
				}finally {
					jsonPar.close();
				}
			} else {
				return new Result(404, "{\"reason\":\"The user has been removed\"}");
			}
		} finally {
			lock.unlock();
		}
		return new Result(500, "{\"reason\":\"Server has bugs\"}");
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
	//@Returns: if Result.result == 202 than the user has been logged out, 400 if the user wasn't logged
	//@param username: the name of the user logged in
	//@param logged_users: all users that are currently logged_in
	public static Result logout(String username, ConcurrentMap<String, String> logged_users) {
		if(logged_users == null || username == null)
			throw new IllegalArgumentException();
		if(logged_users.remove(username)!=null)
			return new Result(202, "{\"reason\":\"The user have successfully logged out\"}");
		return new Result(400, "{\"reason\":\"The user was not logged in\"}");
	}
	
	//@Requires: username != null usernames != null tags_in_mem != null 
	//@Throws: IllegalArgumentEsception, IOException, TooManyTagsException
	//@Effects: lists all users that have at least one tag in common with the user identified by the username parameter
	//@Returns: Result.result a http code ad as Result.reason a json string in format object with field as usernames and values as array of all tags specified by the user
	//@param username: the name of the user who requests a look up of the users
	//@param usernames: the concurrent hashMap that holds all usernames registered and locks
	//@param tags_in_mem: all tags specified by the users
	public static Result list_users(String username, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, ReadWriteLock> tags_in_mem) throws IOException, TooManyTagsException {
		if(username == null || usernames == null || tags_in_mem == null)
			throw new IllegalArgumentException();
		File user=new File(StaticNames.PATH_TO_PROFILES+username+"/"+StaticNames.NAME_JSON_USER);
		ReadWriteLock rw_lock=null;
		TreeSet<String> user_to_ret = new TreeSet<String>();
		String result="{";
		if((rw_lock=usernames.get(username)) == null)
			return new Result(404, "{\"reasone\":\"User not found\"}");
		
		Tags tags=getUserTags(user, rw_lock.readLock());
		Iterator<String> tags_iter=null;

		
		if(tags == null)
			return new Result(404, "{\"reasone\":\"User not found\"}");
		tags_iter=tags.iterator();
		while(tags_iter.hasNext()) {
			String tag = tags_iter.next();
			getAllUsers(new File(StaticNames.PATH_TO_TAGS+tag+"/"+StaticNames.NAME_FILE_TAG), tag, tags_in_mem.get(tag).readLock(), usernames, tags_in_mem, user_to_ret);
		}
		user_to_ret.remove(username);
		tags_iter=user_to_ret.iterator();
		if(tags_iter.hasNext()) {
			String part_user=tags_iter.next();
			Iterator<String> tags_part_user=null;
			
			result=result.concat("\""+part_user+"\":[");
			tags=getUserTags(new File(StaticNames.PATH_TO_PROFILES+part_user+"/"+StaticNames.NAME_JSON_USER), usernames.get(part_user).readLock());
			if(tags!=null) {
				tags_part_user=tags.iterator();
				
				String curr_tag=tags_part_user.next();
				result=result.concat("\""+curr_tag+"\"");
				while(tags_part_user.hasNext()) {
					curr_tag=tags_part_user.next();
					result=result.concat(", \""+curr_tag+"\"");
				}
				result=result.concat("]");
			}
		}
		while(tags_iter.hasNext()) {
			String part_user=tags_iter.next();
			Iterator<String> tags_part_user=null;
			
			result=result.concat(", \""+part_user+"\":[");
			tags=getUserTags(new File(StaticNames.PATH_TO_PROFILES+part_user+"/"+StaticNames.NAME_JSON_USER), usernames.get(part_user).readLock());
			if(tags == null)
				continue;
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
		return new Result(200, result);
	}
	
	
	//@Requires: username != null, usernames !=null
	//@Throws IllegalArgumentException IOException
	//@Effects: lists all the users followed by the user
	//@Returns: Result.result http code and Result.reason  the json string in format array with all users
	//@param username: the name of the user
	//@param usernames: all usernames in the system
	public static Result list_following(String username, ConcurrentMap<String, ReadWriteLock> usernames) throws IOException {
		if(username == null || usernames == null)
			throw new IllegalArgumentException();
		Lock lock=usernames.get(username).readLock();
		var wrapper = new Object() { String result="["; };
		try {
			lock.lock();
			if(!(new File(StaticNames.PATH_TO_PROFILES+username)).exists())
				return new Result(404, "{\"reasone\":\"User not found\"}");
			Stream.of((new File(StaticNames.PATH_TO_PROFILES+username+"/" +"Following")).listFiles()).forEach(path -> {
				try {
					if(path.isDirectory()) {
						wrapper.result=wrapper.result.concat(", \""+path.getName()+"\"");
					}
				} catch(NullPointerException e) {
					return;
				}
			});
		} finally {
			lock.unlock();
		}
		wrapper.result=wrapper.result.replaceFirst(", ", "");
		wrapper.result=wrapper.result.concat("]");
		return new Result(200, wrapper.result);
	}
	
	//@Requires: username != null follow_user != null usernames != null
	//@Throws: IllegalArgumentException IOException
	//@Modifies: the directory some/path/username/Following and some/path/follow_user/Followers
	//@Effects: creates a symbolic link of the user followed in the folder some/path/username/Following
	//			and symbolic link of the user's foleder in some/path/follow_user/Followers
	//@Returns: if Result.result == 200 than the username started to follow follow_user, otherwise 404 or 400
	//@param username: the name of the user who wants to follow
	//@param follow_user: the name of the user who will be followed
	//@param usernames: all the name of the users of winsome
	public static Result follow_user(String username, String follow_user, ConcurrentMap<String, ReadWriteLock> usernames) throws IOException {
		if(username == null || follow_user==null || usernames == null)
			throw new IllegalArgumentException();
		Lock user_lock = usernames.get(username).readLock();
		Lock user_to_follow_lock=usernames.get(follow_user).readLock();
		try {
			user_lock.lock();
			user_to_follow_lock.lock();
			Path user_to_follow=Paths.get(StaticNames.PATH_TO_PROFILES+follow_user);
			if(!user_to_follow.toFile().exists())
				return new Result(404, "{\"reason\":\"User to follow has been deleted"+"\"}");
			user_to_follow=user_to_follow.toAbsolutePath();
			Path user=Paths.get(StaticNames.PATH_TO_PROFILES+username+"/"+"Following"+"/"+follow_user);
			if(!(new File(StaticNames.PATH_TO_PROFILES+username)).exists())
				return new Result(404, "{\"reason\":\"The user has been deleted"+"\"}");
			Files.createSymbolicLink(user, user_to_follow);
			user=Paths.get(StaticNames.PATH_TO_PROFILES+username);
			user=user.toAbsolutePath();
			user_to_follow=Paths.get(StaticNames.PATH_TO_PROFILES+follow_user+"/"+"Followers"+"/"+username);
			Files.createSymbolicLink(user_to_follow, user);
			return new Result(200, "{\"reason\":\"The user: " + username + " now follows " + follow_user+"\"}");
		}catch (FileAlreadyExistsException e) {
			return new Result(400, "{\"reason\":\"The user: " + username + " already follows " + follow_user+"\"}");
		} finally {
			user_lock.unlock();
			user_to_follow_lock.unlock();
		}
	}
	
	
	//@Requires: username != null unfollow_user != null usernames != null
		//@Throws: IllegalArgumentException IOException
		//@Modifies: the directory some/path/username/Following and some/path/follow_user/Followers
		//@Effects: removes the symbolic link of the user followed in the folder some/path/username/Following
		//			and symbolic link of the user's foleder in some/path/follow_user/Followers
		//@Returns: if Result.result == 200  than the username unfollowed follow_user, 400 otherwise
		//@param username: the name of the user who wants to unfollow
		//@param unfollow_user: the name of the user who will be unfollowed
		//@param usernames: all the name of the users of winsome
		public static Result unfollow_user(String username, String unfollow_user, ConcurrentMap<String, ReadWriteLock> usernames) throws IOException {
			if(username == null || unfollow_user==null || usernames == null)
				throw new IllegalArgumentException();
			Lock user_lock = usernames.get(username).readLock();
			Lock user_to_unfollow_lock=usernames.get(unfollow_user).readLock();
			try {
				user_lock.lock();
				user_to_unfollow_lock.lock();
				File user_to_unfollow=new File(StaticNames.PATH_TO_PROFILES+unfollow_user+"/"+"Followers/"+username);
				File user=new File(StaticNames.PATH_TO_PROFILES+username+"/"+"Following/"+unfollow_user);
				if(!user.exists())
					return new Result(400, "{\"reason\":\"The user was not a follower of " + unfollow_user+"\"}");
				user.delete();
				user_to_unfollow.delete();
				return new Result(200, "{\"reason\":\"The user: " + username + " unfollowed " + unfollow_user+"\"}");
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
	public static Tags getUserTags(File user, Lock lock_user) throws JsonParseException, IOException, TooManyTagsException {
		JsonFactory jsonFact= new JsonFactory();
		try {
			lock_user.lock();
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
			lock_user.unlock();
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
	private static void getAllUsers(File tag_file_json, String tag, Lock lock_tag, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, ReadWriteLock> tags_in_mem, TreeSet<String> users_to_ret) throws JsonParseException, IOException {
		JsonFactory jsonFact= new JsonFactory();
		int locked=0;
		try {
			lock_tag.lock();
			locked=1;
			if(tag_file_json.exists()) {
				JsonParser jsonPar=jsonFact.createParser(tag_file_json);
				try{
					jsonPar.nextToken();
					while(jsonPar.nextToken() != JsonToken.END_ARRAY) {
						String tok=jsonPar.getValueAsString();
						if(tok!=null) {
							if(!(usernames.containsKey(tok))) {
								lock_tag.unlock();
								locked=0;
								User_Data.removeUserFromTag(tok, tag, tags_in_mem);
								lock_tag.lock();
								locked=1;
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
			if(locked ==1) lock_tag.unlock();
		}
	}
	//@Requires: username != null usernames != null
	//@Throws: IllegalArgumentException IOException
	//@Effects: retrieves all the post published by the user
	//@Returns: http code and  all post as json string in format object, null if the user was delete in meantime
	//@param username: the name of the user who has published the posts
	//@param usernames: all name of the users
	public static Result view_blog(String username, ConcurrentMap<String, ReadWriteLock> usernames) throws IOException {
		if(username == null || usernames == null)
			throw new IllegalArgumentException();
		Lock lock = usernames.get(username).readLock();
		var wrapper = new Object() { String res="{"; };
		String dir_name=StaticNames.PATH_TO_PROFILES+username;
		try {
			lock.lock();
			if(!(new File(dir_name).exists())){
				return new Result(404, "{\"reasone\":\"User not found\"}");
			}
			dir_name=dir_name+"/"+"Posts";
			Stream.of((new File(dir_name)).listFiles()).forEach(path -> {
				try {
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
								if(curr==null)
									break;
								if(curr != null && (curr.equals("author") || curr.equals("title"))) {
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
							jsonPar.close();
						} catch (JsonParseException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					} else if(Files.isSymbolicLink(path.toPath()) && !path.exists()) {
						path.delete();
					}
				} catch(NullPointerException e) {
					return;
				}
			});
			
		} finally {
			lock.unlock();
		}
		wrapper.res=wrapper.res.replaceFirst(", ", "");
		return new Result(200,  wrapper.res=wrapper.res.concat("}"));
		
	}
	
	//@Requires: username != null title != null && 1 <= title.length <=20 content != null && 1<=content.length<=500 usernames != null
	//@Throws: IllegalArgumentException IOException
	//@Modifes: creates a new directory in some/path/username/Posts/id_post and a json file
	//@Effects: creates a new post
	//@Returns: if Result: result == 201 than the post was created, 400 or 404 otherwise with a reason
	//@param username: the name of user who wants to create the post
	//@param title: the title of the post
	//@para content: the content of the post
	//@param usernames: the names of all users with locks
	public static Result create_post(String username, String title, String content, ConcurrentMap<String, ReadWriteLock> usernames) throws IOException {
		if(username == null || title == null || content ==null || usernames== null)
			throw new IllegalArgumentException();
		Lock lock=usernames.get(username).writeLock();
		String id_post=User_Data.generateString(LENGTH_OF_POST_ID);
		String dir_name=StaticNames.PATH_TO_PROFILES+username;
		JsonFactory jsonFact=new JsonFactory();
		JsonGenerator jsonGen = null;
		Path sym_post = Paths.get(StaticNames.PATH_TO_POSTS+id_post);
		title=title.trim();
		content=content.trim();
		if(title.length()>20 || content.length() > 500 || title.length()==0 || content.length() == 0)
			return new Result(400, "{\"reason\":\"At least one passed argument is invalid\"}");
		try {
			lock.lock();
			File file = new File(dir_name);
			if(!file.exists())
				return new Result(404, "{\"reason\":\"The user has been removed\"}");
			while(true) {
				dir_name=dir_name+"/"+"Posts/"+id_post;
				file=new File(dir_name);
				if(!file.exists()) {
					file.mkdir();
					break;
				}
				id_post=User_Data.generateString(LENGTH_OF_POST_ID);	
			}
			Files.createSymbolicLink(sym_post, Paths.get(dir_name).toAbsolutePath());
			file=new File(dir_name+"/stats.json");
			file.createNewFile();
			jsonGen = jsonFact.createGenerator(file, StaticNames.ENCODING);
			jsonGen.useDefaultPrettyPrinter();
			jsonGen.writeStartObject();
			jsonGen.writeNumberField("num_comments", 0);
			jsonGen.writeNumberField("num_thumbs_up", 0);
			jsonGen.writeNumberField("num_thumbs_down", 0);
			jsonGen.writeNumberField("num_iter", 0);
			jsonGen.writeNumberField("last_calc", 0);
			jsonGen.writeEndObject();
			jsonGen.close();
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
			jsonGen.writeEndObject();
			jsonGen.flush();
			jsonGen.close();
			return new Result(201, "{\"reason\":\"Post has been succesfully added\"}");
		} finally {
			lock.unlock();
		}
	}
	//@Requires: username != null usernames != null
	//@Throws: IllegalArgumentException, IOException, JsonParseException
	//@Effects: retrieves all posts in the own feed
	//@Returns: a http code and a json string in format object with fielnames: id_post, author, content
	//@param username: name of the user who's feed will be build
	//@param usernames: all name of the users in the system
	public static Result show_feed(String username, ConcurrentMap<String, ReadWriteLock> usernames) throws JsonParseException, IOException {
		if(username == null || usernames == null)
			throw new IllegalArgumentException();
		String dir_name=StaticNames.PATH_TO_PROFILES+username;
		String author = null;
		String res="{";
		ReadWriteLock lock_r =usernames.get(username);
		if(lock_r == null)
			return new Result(404, "{\"reasone\":\"User not found\"}");
		Lock lock = lock_r.readLock();
		File dir=null;
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar=null;	
		
		
		try {
			lock.lock();
			dir=new File(dir_name);
			if(!dir.exists())
				return new Result(404, "{\"reasone\":\"User not found\"}");
			dir_name=dir_name+"/"+"Posts";
			dir=new File(dir_name);
			File[] files = dir.listFiles();
			for(File f: files) {
				int times_f=0;
				Lock lock_author=null;

				try {
					if(Files.isSymbolicLink(f.toPath())) {
						author =get_author(f.getName());
						lock_author=usernames.get(author).readLock();
						lock_author.lock();
					}
					if(f.isDirectory()) {
						res=res.concat(", \""+f.getName()+"\":{");
						String temp_res="";
						dir = new File(dir_name +"/"+f.getName()+"/"+StaticNames.NAME_FILE_POST);
						jsonPar=jsonFact.createParser(dir);
						jsonPar.nextToken();
						while(jsonPar.nextToken()!=JsonToken.END_OBJECT) {
							String tok=jsonPar.getText();
							if(tok == null)
								break;
							if(tok.equals("author") || tok.equals("title")) {
								times_f++;
								temp_res=temp_res.concat(", \""+tok+"\":");
								jsonPar.nextToken();
								tok=jsonPar.getText();
								temp_res=temp_res.concat("\""+tok+"\"");
							}
							if(times_f==2) break;
						}
						jsonPar.close();
						temp_res=temp_res.replaceFirst(", ", "");
						temp_res=temp_res.concat("}");
						res=res.concat(temp_res);
					} else if(Files.isSymbolicLink(f.toPath()) && !f.exists()) {
						f.delete();
					}
				} catch (NullPointerException | FileNotFoundException e) {
					continue;
				} catch (NoSuchFileException e) {
					continue;
				} finally {
					if(lock_author != null)
						lock_author.unlock();
				}
			}
			res=res.replaceFirst(", ", "");
			res=res.concat("}");
		} finally {
			lock.unlock();
		}
		return new Result(200, res);
	}
	
	//@Requires: usrname != null id_post != null usernames !=null
	//@Throws IllegalArgumentException JsonParseException IOException
	//@Effects: retrieves the post by its id with relative information like title content thumbs up thumbs down, comments 
	//@Returns: http code and a string in format json
	//@param username: the name of the user who has requested the functionality
	//@param id_post: the identifier of the post
	//@param usernames: all usernames in the system
	public static Result show_post(String username, String id_post, ConcurrentMap<String, ReadWriteLock> usernames) throws JsonParseException, IOException {
		if(username == null || id_post == null || usernames == null)
			throw new IllegalArgumentException();
		try {
			username=get_author(id_post);
		} catch(NoSuchFileException e) {
			return new Result(404, "{\"reasone\":\"Post not found\"}");
		}
		Lock lock= usernames.get(username).readLock();
		String res="{";
		int times_f=0;
		JsonFactory jsonFact = new JsonFactory();
		JsonParser jsonPar = null;
		File dir=new File(StaticNames.PATH_TO_PROFILES+username);
		File post=new File(StaticNames.PATH_TO_POSTS+id_post+"/"+StaticNames.NAME_FILE_POST);
		File comments = new File(StaticNames.PATH_TO_POSTS+id_post+"/Comments");
		File thumbs_up = new File(StaticNames.PATH_TO_POSTS+id_post+"/Thumbs_up");
		File thumbs_down = new File(StaticNames.PATH_TO_POSTS+id_post+"/Thumbs_down");
		try {
			lock.lock();
			if(!dir.exists()) {
				return new Result(404, "{\"reasone\":\"User not found\"}");
			}
			if(!post.exists()) {
				return new Result(404, "{\"reasone\":\"Post not found\"}");
			}
			jsonPar=jsonFact.createParser(post);
			jsonPar.nextToken();
			while(jsonPar.nextToken() != JsonToken.END_OBJECT) {
				String curr=jsonPar.getText();
				if(curr == null)
					return new Result(204, "{\"reasone\":\"The post is not pubblished yet\"}");
				if(curr.equals("content") || curr.equals("title")) {
					times_f++;
					res=res.concat(", \""+curr+"\":");
					jsonPar.nextToken();
					curr=jsonPar.getText();
					res=res.concat("\""+curr+"\"");
					if(times_f==2) break;
				}
			}
			jsonPar.close();
			res=res.concat(", \"thumbs_up\":"+thumbs_up.list().length);
			res=res.concat(", \"thumbs_down\":"+thumbs_down.list().length);
			res=res.concat(", \"comments\":{");
			String temp_res_com = "";
			for(File d: comments.listFiles()) {
				for(File f: d.listFiles()) {
					jsonPar = jsonFact.createParser(f);
					String name_f=f.getName();
					temp_res_com =temp_res_com.concat(", \""+name_f.substring(0, name_f.lastIndexOf('.'))+"\":{");
					String temp_res="";
					jsonPar.nextToken();
					while(jsonPar.nextToken() != JsonToken.END_OBJECT) {
						String curr=jsonPar.getText();
						if(curr==null)
							break;
						temp_res=temp_res.concat(", \""+curr+"\":");
						jsonPar.nextToken();
						curr=jsonPar.getText();
						temp_res=temp_res.concat("\""+curr+"\"");
					}
					temp_res=temp_res.replaceFirst(", ", "");
					temp_res=temp_res.concat("}");
					temp_res_com=temp_res_com.concat(temp_res);
				}
			}
			temp_res_com = temp_res_com.replaceFirst(", ", "");
			temp_res_com = temp_res_com.concat("}");
			res=res.concat(temp_res_com);
			res=res.replaceFirst(", ", "");
			res=res.concat("}");
			return new Result(200, res);
		} catch(FileNotFoundException e) {
			return new Result(204, "{\"reasone\":\"The post was just deleted\"}");
		} finally {
			lock.unlock();
		}
	}
	
	//@Requires: username != null id_post != null usernames != null
	//@Throws: IllegalArgumentException IOException
	//@Modifies: the directory some/path/username/Posts
	//@Effects: deletes the post and all comments/votes associated to the post
	//Returns: if Result.result == 202 the post was deleted, 404, 400, 401 otherwise
	//@param username: the name of the user
	//@param id_post: the identifier of the post
	//@param usernames: the names of all users
	public static Result delete_post(String username, String id_post, ConcurrentMap<String, ReadWriteLock> usernames) {
		if(username == null || id_post == null || usernames == null)
			throw new IllegalArgumentException();
		Lock lock = usernames.get(username).writeLock();
		File dir = new File(StaticNames.PATH_TO_PROFILES+username);
		File post = new File(StaticNames.PATH_TO_PROFILES+username+"/Posts/"+id_post);
		try {
			lock.lock();
			if(!dir.exists())
				return new Result(404, "{\"reason\":\"The user has been deleted\"}");
			if(!post.exists())
				return new Result(404, "{\"reason\":\"Post not found\"}");
			if(Files.isSymbolicLink(post.toPath()))
				return new Result(401, "{\"reason\":\"The post belongs to another user\"}");
			User_Data.deleteDir(post);
			try {
				Files.delete(Paths.get(StaticNames.PATH_TO_POSTS+id_post));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return new Result(202, "{\"reason\":\"The post has been deleted succesfully\"}");
		} finally {
			lock.unlock();
		}
	}
	
	//@Requires: username != null id_post != null usernames != null 
	//@Throws: IllegalArgumentException
	//@Mdofies: some/path/username/Posts
	//@Effects: creates a symbolic link of the post to be rewinded
	//@Returns: http code and a string in format json
	//@param username: the identifier of the user
	//@param id_post: the identifier of the post
	//@param usernames: the names of all users
	public static Result rewin_post(String username, String id_post, ConcurrentMap<String, ReadWriteLock> usernames) {
		if(username == null || id_post == null || usernames == null)
			throw new IllegalArgumentException();
		Lock lock=usernames.get(username).readLock();
		String dir=StaticNames.PATH_TO_PROFILES+username+"/Posts";
		Path path_to_posts = Paths.get(dir);
		Path post_to_rw = Paths.get(StaticNames.PATH_TO_POSTS+id_post);
		try {
			lock.lock();
			if(!Files.exists(path_to_posts))
				return new Result(404, "{\"reason\":\"The usere was deleted\"}");
			if(!Files.exists(post_to_rw))
				return new Result(404, "{\"reason\":\"Post not found\"}");
			if(Files.exists(Paths.get(dir+"/"+id_post)))
				return new Result(400, "{\"reason\":\"The post has been rewinded already\"}");
			Files.createSymbolicLink(Paths.get(dir+"/"+id_post), post_to_rw.toRealPath());
			return new Result(200, "{\"reason\":\"Post got rewinded\"}");
		} catch (FileAlreadyExistsException e) { 
			e.printStackTrace();
			return new Result(500, "{\"reason\":\"Server has bugs\"}");
		}catch (IOException e) {
			return new Result(410, "{\"reason\":\"The post was delete\"}");
		} finally {
			lock.unlock();
		}
	}
	//@Requires: username != null id_post != null reaction == -1 || reaction == 1 usernames != null
	//@Throws: IllegalArgumentException IOException
	//@Modifies: some/path/author_of_the_post/id_post/thumbs_up or thumbs_down
	//@Effects: enables a user to rate the post of someone else
	//@Returns: http code with the reason
	//@param username: the identifier of the user
	//@param id_post: the identifier of the post to be rated
	//@param reaction: the feedback, it can be 1 (positive) or -1 (negative)
	//@param usernames: all names of the users with relative locks
	public static Result rate_post(String username, String id_post, int reac, ConcurrentMap<String, ReadWriteLock> usernames) throws IOException {
		if(username == null || id_post == null || usernames == null)
			throw new IllegalArgumentException();
		if(reac != -1 && reac != 1) {
			return new Result(400, "{\"reason\":\"Reaction can be 1 or -1\"}");
		}
		Lock lock=usernames.get(username).readLock();
		Lock lock_rw=null;
		String path_post=null;
		String author_post=null;
		String dir_reac=reac == 1 ? "Thumbs_up/": "Thumbs_down/";
		JsonFactory jsonFact = new JsonFactory();
		JsonGenerator jsonGen = null;
		
		try {
			lock.lock();
			Path post= (new File(StaticNames.PATH_TO_PROFILES+username +"/Posts/"+id_post)).toPath();
			if(Files.exists(post) && !Files.isSymbolicLink(post)) {
				return new Result(400, "{\"reason\":\"Can not rate own post\"}");
			}
			if(!Files.exists(post))
				return new Result(404, "{\"reason\":\"Post not found\"}");
			path_post = post.toRealPath().toString();
			author_post=path_post.substring(path_post.indexOf("Profiles/")+9, path_post.indexOf("/Posts"));
		} catch(IOException e) {
			return new Result(404, "{\"reason\":\"The post has been deleted\"}");
		} finally {
			lock.unlock();
		}
		if(path_post == null || author_post == null)
			return new Result(500, "{\"reason\":\"Server has bucks\"}");
		lock_rw=usernames.get(author_post).readLock();
		
		try {
			lock_rw.lock();
			File reac_file = new File(path_post+"/"+dir_reac+username+".json");
			File exist_th_up= new File(path_post+"/Thumbs_up/"+username+".json");
			File exist_th_down= new File(path_post+"/Thumbs_down/"+username+".json");

			if(exist_th_up.exists() || exist_th_down.exists()) {
				return new Result(400, "{\"reason\":\"The post has been already rated\"}");
			}
			reac_file.createNewFile();
			jsonGen = jsonFact.createGenerator(reac_file, StaticNames.ENCODING);
			jsonGen.useDefaultPrettyPrinter();
			jsonGen.writeStartObject();
			jsonGen.writeStringField("author", username);
			jsonGen.writeStringField("date", FORMATTER.format(Calendar.getInstance().getTime()));
			jsonGen.writeEndObject();
			jsonGen.close();
			return new Result(200, "\"reason\":\"The post was rated\"}");
		} finally {
			lock_rw.unlock();
		}
	}
	
	//@Requires: username != null id_post != null comment.length >=1 usernames != null
	//@Throws: IllegalArgumentException IOException
	//@Modifies: some/path/author_of_the_post/id_post/thumbs_up or thumbs_down
	//@Effects: enables a user to rate the post of someone else
	//@Returns: http code with the reason
	//@param username: the identifier of the user
	//@param id_post: the identifier of the post to be rated
	//@param reaction: the feedback, it can be 1 (positive) or -1 (negative)
	//@param usernames: all names of the users with relative locks
	public static Result add_comment(String username, String id_post, String comment, ConcurrentMap<String, ReadWriteLock> usernames) throws IOException {
		if(username == null || id_post == null || usernames == null)
			throw new IllegalArgumentException();
		if((comment =comment.trim()).length() == 0)
			return new Result(400, "{\"reason\":\"Empty comment\"}");
		Lock lock=usernames.get(username).readLock();
		Lock lock_rw=null;
		String path_post=null;
		String author_post=null;
		JsonFactory jsonFact = new JsonFactory();
		JsonGenerator jsonGen = null;
		String id_comment=User_Data.generateString(LENGTH_OF_COMMENT_ID);
		
		try {
			lock.lock();
			Path post= (new File(StaticNames.PATH_TO_PROFILES+username +"/Posts/"+id_post)).toPath();
			if(Files.exists(post) && !Files.isSymbolicLink(post))
				return new Result(400, "{\"reason\":\"Can not comment own post\"}");
			if(!Files.exists(post))
				return new Result(404, "{\"reason\":\"Post not found\"}");
			path_post = post.toRealPath().toString();
			author_post=path_post.substring(path_post.indexOf("Profiles/")+9, path_post.indexOf("/Posts"));
		} catch(IOException e) {
			return new Result(404, "{\"reason\":\"The post has been deleted\"}");
		} finally {
			lock.unlock();
		}
		if(path_post == null || author_post == null)
				return new Result(500, "{\"reason\":\"Server has bucks\"}");
		lock_rw=usernames.get(author_post).readLock();
			
		try {
			lock_rw.lock();
			if(!Files.exists(Paths.get(path_post)))
				return new Result(404, "{\"reason\":\"Post was deleted\"}");
			File reac_file = new File(path_post+"/Comments/" +username);
			if(!reac_file.exists())
				reac_file.mkdir();
			reac_file = new File(path_post+"/Comments/" +username+ "/"+id_comment+".json");
			while(true) {
				if(!reac_file.exists()) {
					reac_file.createNewFile();
					break;
				}
				id_comment=User_Data.generateString(LENGTH_OF_COMMENT_ID);
			}
			jsonGen = jsonFact.createGenerator(reac_file, StaticNames.ENCODING);
			jsonGen.useDefaultPrettyPrinter();
			jsonGen.writeStartObject();
			jsonGen.writeStringField("author", username);
			jsonGen.writeStringField("content", comment);
			jsonGen.writeStringField("date", FORMATTER.format(Calendar.getInstance().getTime()));
			jsonGen.writeEndObject();
			jsonGen.close();
			return new Result(200, "{\"reason\":\"The post was commented\"}");
		} finally {
			lock_rw.unlock();
		}
	}
	//@Requires: username != null usernames != null
	//@Throws: IllegalArgumentException JsonParseEscepion IOException
	//@Effects: permits to retrive the value of the wallet
	//@Returns: http code with a string in format json
	//@param username: the name of the user
	//@param usernames: the names of users
	public static Result get_wallet(String username, ConcurrentMap<String, ReadWriteLock> usernames) throws JsonParseException, IOException {
		if(username == null || usernames == null) 
			throw new IllegalArgumentException();
		String file_w=StaticNames.PATH_TO_PROFILES+username+"/"+StaticNames.NAME_FILE_WALLET;
		File file = new File(file_w);
		Lock lock=usernames.get(username).readLock();
		String res="{";
		JsonFactory jsonFact=new JsonFactory();
		JsonParser jsonPar=null;
		
		try {
			lock.lock();
			if(!file.exists())
				return new Result(404, "{\"reason\":\"The user has been removed\"}");
			jsonPar=jsonFact.createParser(file);
			jsonPar.nextToken();
			while(jsonPar.nextToken()!= JsonToken.END_OBJECT) {
				String tok = jsonPar.getText();
				if(tok == null)
					break;
				res=res.concat(", \""+tok+"\":");
				jsonPar.nextToken();
				tok = jsonPar.getText();
				res=res.concat("\""+tok+"\"");
			}
			res=res.replaceFirst(", ", "");
			res=res.concat("}");
			return new Result(200, "{\"reason\": \"Success\"}");
		} finally {
			lock.unlock();
		}
			
	}
		
	//@Requires: username != null usernames != null
	//@Throws: IllegalArgumentException JsonParseEscepion IOException
	//@Effects: permits to retrive the value of the wallet in bitcoins
	//@Returns: http code with a string in format json
	//@param username: the name of the user
	//@param usernames: all names of the user
	public static Result get_wallet_in_bitcoin(String username, ConcurrentMap<String, ReadWriteLock> usernames) throws JsonParseException, IOException {
		if(username == null || usernames == null) 
			throw new IllegalArgumentException();
		String file_w=StaticNames.PATH_TO_PROFILES+username+"/"+StaticNames.NAME_FILE_WALLET;
		File file = new File(file_w);
		Lock lock=usernames.get(username).readLock();
		String res="{";
		JsonFactory jsonFact=new JsonFactory();
		JsonParser jsonPar=null;
		double value=0f;
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(URL_TO_RAND))
				.build();
		HttpClient client = HttpClient.newBuilder()
				.version(Version.HTTP_1_1)
				.followRedirects(Redirect.NORMAL)
				.connectTimeout(Duration.ofSeconds(10))
				.build();
		try {
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			System.out.println(response.statusCode());
			value=Double.valueOf(response.body());
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(value);
		try {
			lock.lock();
			if(!file.exists())
				return new Result(404, "{\"reason\":\"The user has been removed\"}");
			jsonPar=jsonFact.createParser(file);
			jsonPar.nextToken();
			String tok = null;
			if(jsonPar.nextToken() != JsonToken.END_OBJECT) {
				tok = jsonPar.getText();
				if(tok != null) {	
					res=res.concat(", \""+tok+"_in_bitcoins\":");
					jsonPar.nextToken();
					tok = jsonPar.getText();
					res=res.concat(""+Float.valueOf(tok)*value);
				}
			}
			while(tok != null && jsonPar.nextToken()!= JsonToken.END_OBJECT) {
				tok = jsonPar.getText();
				res=res.concat(", \""+tok+"\":");
				jsonPar.nextToken();
				tok = jsonPar.getText();
				res=res.concat("\""+tok+"\"");
			}
			res=res.replaceFirst(", ", "");
			res=res.concat("}");
			return new Result(200, res);
		} finally {
			lock.unlock();
//			in.close();
		}
				
	}
	
	//@Request: id_post != null
	//@Throws: IllegalArgumentException
	//@Effects: helps to retrieve the author of the post
	//@Returns: the username of the author
	//@param id_post: the idnetifier of the post
	public static String get_author(String id_post) throws IOException {
		if(id_post == null)
			throw new IllegalArgumentException();
		String path_post=null;
		String author_post=null;
		Path post = Paths.get(StaticNames.PATH_TO_POSTS+id_post);
		path_post = post.toRealPath().toString();
		author_post=path_post.substring(path_post.indexOf("Profiles/")+9, path_post.indexOf("/Posts"));
		return author_post;
	}
}


















