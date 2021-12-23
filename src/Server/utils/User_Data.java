package utils;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	public static void load_Usernames(Set<String> usernames) throws JsonParseException, IOException, IllegalArgumentException {
		if(usernames == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		JsonParser jsonPar=jsonFact.createParser(new File(StaticNames.ALL_USERNAMES));
		JsonToken curr = null;
		while((curr = jsonPar.nextToken()) != null && curr != JsonToken.END_ARRAY) {
			String name_taken= jsonPar.getCurrentName();
			if(name_taken!=null) usernames.add(name_taken);
		}
	}
	//@Requires: user !=null
	//@Throws: IOException, IllegalArgumentException, FileAlreadyExistsException
	//@Effects: adds the new user to the file: all_usernames.json, profiles.json. Creates a folder with the name as username and a file which contains information about the user
	//@Modifies: all_usernames.json, profiles.json, directory: Profiles
	//@param user: the user to be registered
	//@param status: 1 if the username was inserted in StaticNames.ALL_USERNAMES
	//				 2 if the username was inserted also in  StaticNames.PROFILES
	public static void add_user(User user, AtomicInteger status) throws IOException,IllegalArgumentException, FileAlreadyExistsException {
		if(user == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		File temp_file=new File(StaticNames.ALL_USERNAMES_TEMP+ Thread.currentThread().getName()+".json");
		temp_file.createNewFile();
		JsonGenerator jsonGen = jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
		boolean result;
		ObjectMapper mapper = new ObjectMapper();
		
		jsonGen.useDefaultPrettyPrinter();
		String dirName=StaticNames.PATH_TO_PROFILES.concat(user.getUser_name());
		File directory = new File(dirName);
		
		StaticNames.USERNAMES_LOCK.lock();
		File curr_file=new File(StaticNames.ALL_USERNAMES);
		JsonParser jsonPar = jsonFact.createParser(curr_file);
		while (jsonPar.nextToken()!=JsonToken.END_ARRAY)
			jsonGen.copyCurrentEvent(jsonPar);
		jsonGen.writeString(user.getUser_name());
		jsonGen.copyCurrentEvent(jsonPar);
		jsonGen.flush();
		curr_file.delete();
		temp_file.renameTo(new File(StaticNames.ALL_USERNAMES));
		StaticNames.USERNAMES_LOCK.unlock();
		jsonGen.close();
		jsonPar.close();

		status.incrementAndGet();
		
		temp_file=new File(StaticNames.PROFILES_TEMP+ Thread.currentThread().getName()+".json");
		temp_file.createNewFile();
		jsonGen=jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
		jsonGen.useDefaultPrettyPrinter();
		StaticNames.PROFILES_LOCK.lock();
		curr_file=new File(StaticNames.PROFILES);
		jsonPar=jsonFact.createParser(curr_file);
		jsonPar.nextToken();
		jsonGen.copyCurrentEvent(jsonPar);
		while(jsonPar.nextToken()!=JsonToken.END_OBJECT)
			jsonGen.copyCurrentStructure(jsonPar);
	    jsonGen.setCodec(mapper);
		jsonGen.writeObjectField(user.getUser_name(), user);
		jsonGen.copyCurrentEvent(jsonPar);
		curr_file.delete();
		temp_file.renameTo(new File(StaticNames.PROFILES));
		StaticNames.PROFILES_LOCK.unlock();
		jsonGen.close();
		jsonPar.close();
		status.incrementAndGet();

		if(directory.exists())
			throw new FileAlreadyExistsException("This should not have happened, there is a bug in the code.");
		directory.mkdir();
		File file = new File(dirName + "/" + StaticNames.NAME_JSON_USER);
		result =file.createNewFile();
		assert result==true;
		jsonGen= jsonFact.createGenerator(file, StaticNames.ENCODING);
		jsonGen.useDefaultPrettyPrinter();
		jsonGen.writeStartObject();
	    jsonGen.writeStringField("user_name", user.getUser_name());
	    jsonGen.writeStringField("hashed_password", user.getHashed_password());
	    jsonGen.writeStringField("salt", user.getSalt());
	    jsonGen.writeStringField("tags", user.getTags());

		jsonGen.writeEndObject();
		jsonGen.close();
		
	}
	public static void deleteUserFromAll_us(String username) throws IllegalArgumentException, IOException {
		if(username == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		File temp_file=new File(StaticNames.ALL_USERNAMES_TEMP+ Thread.currentThread().getName()+".json");
		temp_file.createNewFile();
		JsonGenerator jsonGen = jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
		jsonGen.useDefaultPrettyPrinter();
		
		StaticNames.USERNAMES_LOCK.lock();
		File curr_file=new File(StaticNames.ALL_USERNAMES);
		JsonParser jsonPar = jsonFact.createParser(curr_file);
		while (jsonPar.nextToken()!=JsonToken.END_ARRAY) {
			String user=jsonPar.getText();
			if(!user.equals(username)) jsonGen.copyCurrentEvent(jsonPar);
		}
		jsonGen.copyCurrentEvent(jsonPar);
		jsonGen.flush();
		curr_file.delete();
		temp_file.renameTo(new File(StaticNames.ALL_USERNAMES));
		StaticNames.USERNAMES_LOCK.unlock();
		jsonGen.close();
		jsonPar.close();
	}
	
	public static void deleteUserFromProf(String username) throws IllegalArgumentException, IOException {
		if(username == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		File temp_file=new File(StaticNames.PROFILES_TEMP+ Thread.currentThread().getName()+".json");
		temp_file.createNewFile();
		JsonGenerator jsonGen = jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
		jsonGen.useDefaultPrettyPrinter();
		StaticNames.PROFILES_LOCK.lock();
		File curr_file=new File(StaticNames.PROFILES);
		JsonParser jsonPar = jsonFact.createParser(curr_file);
		jsonPar.nextToken();
		jsonGen.copyCurrentEvent(jsonPar);
		while(jsonPar.nextToken()!=JsonToken.END_OBJECT) {
			String user=jsonPar.getCurrentName();
			if(!user.equals(username)) {
				jsonGen.copyCurrentStructure(jsonPar);
			} else jsonPar.skipChildren();
		}
		jsonGen.copyCurrentEvent(jsonPar);
		curr_file.delete();
		temp_file.renameTo(new File(StaticNames.PROFILES));
		StaticNames.PROFILES_LOCK.unlock();
		jsonGen.close();
		jsonPar.close();
	}
	public static void deleteDir(File file) {
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
	public static void setSettings_Server(String PATH_TO_SSL) {
		System.setProperty("javax.net.ssl.debug", "all");
		System.setProperty("javax.net.ssl.keyStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames.KEYSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.keyStorePassword", StaticNames.PASS_SSL);
		System.setProperty("javax.net.ssl.trustStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames.TRUSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.trustStorePassword", StaticNames.PASS_SSL);
	}
}
