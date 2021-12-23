package utils;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Paths;
import java.util.Set;

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
	public static void load_Usernames(Set<String> usernames) throws JsonParseException, IOException, IllegalArgumentException {
		if(usernames == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		JsonParser jsonPar=jsonFact.createParser(new File(StaticNames.ALL_USERNAMES));
		while(jsonPar.nextToken() != JsonToken.END_ARRAY) {
			String name_taken= jsonPar.getCurrentName();
			if(name_taken!=null) usernames.add(name_taken);
		}
	}
	//@Requires: user !=null
	//@Throws: IOException, IllegalArgumentException, FileAlreadyExistsException
	//@Effects: adds the new user to the file: all_usernames.json, profiles.json. Creates a folder with the name as username and a file which contains information about the user
	//@Modifies: all_usernames.json, profiles.json, directory: Profiles
	//@param user: the user to be registered
	public static void add_user(User user) throws IOException,IllegalArgumentException, FileAlreadyExistsException {
		if(user == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		JsonGenerator jsonGen = jsonFact.createGenerator(new File(StaticNames.ALL_USERNAMES), StaticNames.ENCODING);
		JsonGenerator jsonGenProf= jsonFact.createGenerator(new File(StaticNames.PROFILES), StaticNames.ENCODING);
		boolean result;
		
		String dirName=StaticNames.PATH_TO_PROFILES.concat(user.getUsername());
		File directory = new File(dirName);
		StaticNames.USERNAMES_LOCK.lock();
		jsonGen.writeStartArray();
		jsonGen.writeString(user.getUsername());
		jsonGen.writeEndArray();
		jsonGen.flush();
		StaticNames.USERNAMES_LOCK.unlock();
		jsonGen.close();
		
		StaticNames.PROFILES_LOCK.lock();
		jsonGenProf.writeStartObject();
		jsonGenProf.writeFieldName(user.getUsername());
		jsonGenProf.writeObject(user);
		jsonGenProf.writeEndObject();
		jsonGenProf.flush();
		StaticNames.PROFILES_LOCK.unlock();
		jsonGenProf.close();
		
		if(directory.exists())
			throw new FileAlreadyExistsException("This should not have happened, there is a bug in the code.");
		directory.mkdir();
		File file = new File(dirName + "/" + StaticNames.NAME_JSON_USER);
		result =file.createNewFile();
		assert result==true;
		jsonGen= jsonFact.createGenerator(file, StaticNames.ENCODING);
		jsonGen.writeStartObject();
		jsonGen.writeObject(user);
		jsonGen.writeEndObject();
		jsonGen.close();
		
	}
	public static void setSettings_Server(String PATH_TO_SSL) {
		System.setProperty("javax.net.ssl.debug", "all");
		System.setProperty("javax.net.ssl.keyStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames.KEYSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.keyStorePassword", StaticNames.PASS_SSL);
		System.setProperty("javax.net.ssl.trustStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames.TRUSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.trustStorePassword", StaticNames.PASS_SSL);
	}
}
