package utils;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import user.User;
public class User_Data {
	/*
	 * Overview: Contains useful static methods 
	 */
	
	//@Requires:usernames!=null
	//@Throws: JsonParseException, IOException, IllegalArgumentException
	//@Effects: loads all user name from disc to a Set
	//@Modifies: the parameter usernames
	//@param usernames: the set where all user name will be saved
	public static void load_Usernames(ConcurrentMap<String, ReadWriteLock> usernames) throws JsonParseException, IOException, IllegalArgumentException {
		if(usernames == null)
			throw new IllegalArgumentException();
		Files.walk(Paths.get(StaticNames.PATH_TO_PROFILES)).forEach(path -> {
			if(path.toFile().isDirectory()) {
				usernames.putIfAbsent(path.getFileName().toString(), new ReentrantReadWriteLock());
			}
		});
	}
	
	//@Requires:map_tags!=null
		//@Throws: JsonParseException, IOException, IllegalArgumentException
		//@Effects: loads all tags from disc to a ConcurrentMap<String, ReadWriteLock>
		//@Modifies: the parameter map_tags
		//@param map_tags: the concurrent map where each tag will have its own lock
		public static void load_Tags(ConcurrentMap<String, ReadWriteLock> map_tags) throws JsonParseException, IOException, IllegalArgumentException {
			if(map_tags == null)
				throw new IllegalArgumentException();
			Files.walk(Paths.get(StaticNames.PATH_TO_TAGS)).forEach(path -> {
				if(path.toFile().isDirectory()) {
					map_tags.putIfAbsent(path.getFileName().toString(), new ReentrantReadWriteLock());
				}
			});
		}
		
		
	//@Requires: user !=null
	//@Throws: IOException, IllegalArgumentException, FileAlreadyExistsException
	//@Effects: adds the new user to the file: all_usernames.json, profiles.json. Creates a folder with the name as username and a file which contains information about the user
	//@Modifies: all_usernames.json, profiles.json, directory: Profiles
	//@param user: the user to be registered
	//@param status: 1 if the username was inserted in StaticNames.ALL_USERNAMES
	//				 2 if the username was inserted also in  StaticNames.PROFILES
	public static void add_user(User user, AtomicInteger status, ConcurrentMap<String, ReadWriteLock> tags_in_mem) throws IOException,IllegalArgumentException, FileAlreadyExistsException {
		if(user == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		boolean result;
		String dirName=StaticNames.PATH_TO_PROFILES.concat(user.getUser_name());
		File directory = new File(dirName);
		if(directory.exists())
			throw new FileAlreadyExistsException("This should not have happened, there is a bug in the code.");
		directory.mkdir();
		File file = new File(dirName + "/" + StaticNames.NAME_JSON_USER);
		result =file.createNewFile();
		assert result==true;
		JsonGenerator jsonGen = jsonFact.createGenerator(file, StaticNames.ENCODING);
		
		jsonGen.useDefaultPrettyPrinter();
		jsonGen.writeStartObject();
	    jsonGen.writeStringField("user_name", user.getUser_name());
	    jsonGen.writeStringField("hashed_password", user.getHashed_password());
	    jsonGen.writeStringField("salt", user.getSalt());
	    jsonGen.writeStringField("tags", user.getTags());

		jsonGen.writeEndObject();
		jsonGen.close();
		status.incrementAndGet();
		file=new File(dirName + "/"+ "Followers");
		file.mkdir();
		file=new File(dirName+ "/"+ "Following");
		file.mkdir();
		create_addTags(user.getTagsIter(), user.getUser_name(), tags_in_mem);
	}
	//@Requires: file != null
	//@Modifies: file
	//@Throws: IllegalArgumentException
	//@Effects:  deletes all files inside a directory and the deletes it
	//@param file: the directory to be deleted
	public static void deleteDir(File file) {
		if(file == null)
			throw new IllegalArgumentException();
	    File[] contents = file.listFiles();
	    if (contents != null) {
	        for (File f : contents) {
	            if (! Files.isSymbolicLink(f.toPath())) {
	                deleteDir(f);
	            }
	        }
	    }
	    file.delete();
	}
	//@Throws: IOException
	//@Modifies: tags_in_mem, path: /src/Server/User_Data/Tags
	//@Effects: adds all tags to the concurrent map and creates a folder for each missing tag and if already exists than updates the file users.json to include the new user.
	// so for each specified tag there will be a folder and a file in which will be saved all users that have indicated that tag
	//@param tags_iter: an iterator that holds all tags
	//@param usernam: new username
	//@param tags_in_mem: all tags that have been previously specified
	private static void create_addTags(Iterator<String> tags_iter, String username, ConcurrentMap<String, ReadWriteLock> tags_in_mem) throws IOException{
		while(tags_iter.hasNext()) {
			String tag =tags_iter.next();
			String path=StaticNames.PATH_TO_TAGS+tag;
			File file = new File(path);
			Lock lock=null;
			JsonFactory jsonFact=new JsonFactory();
			File temp_file=null;
			JsonGenerator jsonGen = null;
			
			tags_in_mem.putIfAbsent(tag, new ReentrantReadWriteLock());
			if(!file.exists())
				file.mkdir();
			temp_file=new File(path + "/" +StaticNames.NAME_FILE_TAG_TEMP+ Thread.currentThread().getName()+".json");
			temp_file.createNewFile();
			jsonGen=jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
			jsonGen.useDefaultPrettyPrinter();
			
			lock=tags_in_mem.get(tag).writeLock();
			lock.lock();
			file=new File(path+"/"+StaticNames.NAME_FILE_TAG);
			if(!file.exists()) {
				file.createNewFile();
			}
			JsonParser jsonPar = jsonFact.createParser(file);
			if(jsonPar.nextToken()==null) {
				jsonGen.writeStartArray();
				jsonGen.writeString(username);
				jsonGen.writeEndArray();
			} else {
				jsonGen.copyCurrentEvent(jsonPar);
				while (jsonPar.nextToken()!=JsonToken.END_ARRAY)
					jsonGen.copyCurrentEvent(jsonPar);
				jsonGen.writeString(username);
				jsonGen.copyCurrentEvent(jsonPar);
			}
			jsonGen.flush();
			file.delete();
			temp_file.renameTo(new File(path+"/"+StaticNames.NAME_FILE_TAG));
			lock.unlock();
			
			jsonGen.close();
			jsonPar.close();
		}
	}
	
	public static void removeUserFromTag(String username, String tag, ConcurrentMap<String, ReadWriteLock> tags_in_mem) throws IOException {
		if(username == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		Lock lock=tags_in_mem.get(tag).writeLock();
		File temp_file=new File(StaticNames.PATH_TO_TAGS+tag+"/"+StaticNames.NAME_FILE_TAG_TEMP+ Thread.currentThread().getName()+".json");
		temp_file.createNewFile();
		JsonGenerator jsonGen = jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
		jsonGen.useDefaultPrettyPrinter();
		
		try {
			lock.lock();
			File curr_file=new File(StaticNames.PATH_TO_TAGS+tag+"/"+StaticNames.NAME_FILE_TAG);
			JsonParser jsonPar = jsonFact.createParser(curr_file);
			while (jsonPar.nextToken()!=JsonToken.END_ARRAY) {
				String user=jsonPar.getText();
				if(!user.equals(username)) jsonGen.copyCurrentEvent(jsonPar);
			}
			jsonGen.copyCurrentEvent(jsonPar);
			jsonGen.flush();
			curr_file.delete();
			temp_file.renameTo(new File(StaticNames.PATH_TO_TAGS+tag+"/"+StaticNames.NAME_FILE_TAG));
			jsonPar.close();
		} finally {
			jsonGen.close();
			lock.unlock();
		}
	}
	//@Requires: PATH_TO_SSL != null
	//@Throws: illegalArgumentException
	//@Effects: sets different properties need for the SslRMIClientSocketFactory and SslRMIServerSocketFactory
	//@param PATH_TO_SSL: the path where the certificate is stored
	public static void setSettings_Server(String PATH_TO_SSL) {
		if(PATH_TO_SSL==null)
			throw new IllegalArgumentException();
		System.setProperty("javax.net.ssl.debug", "all");
		System.setProperty("javax.net.ssl.keyStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames.KEYSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.keyStorePassword", StaticNames.PASS_SSL);
		System.setProperty("javax.net.ssl.trustStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames.TRUSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.trustStorePassword", StaticNames.PASS_SSL);
	}
}
