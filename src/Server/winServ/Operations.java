package winServ;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import org.springframework.security.crypto.bcrypt.BCrypt;

import com.fasterxml.jackson.core.JsonFactory;
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
	
	public static Result login(String username, String password, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, String> logged_users) throws JsonParseException, IOException {
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
	
	
	public static String getSessionId() {
        return UUID.randomUUID().toString().substring(1, 16).replace("-", "");
    }
	
	
	public static Result logout(String username, ConcurrentMap<String, String> logged_users) {
		if(logged_users.remove(username)!=null)
			return new Result(true, "The user have successfully logged out");
		return new Result(false, "The user was not logged in");
	}
	
	
	public static String list_users(String username, ConcurrentMap<String, ReadWriteLock> usernames, ConcurrentMap<String, ReadWriteLock> tags_in_mem) throws IOException, TooManyTagsException {
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
	
}
