package utils;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class User_Data {
	/*
	 * Overview: Contains useful static methods 
	 */
	
	//@Requires:usernames!=null
	//@Throws: JsonParseException, IOException, IllegalArgumentException
	//@Effects: loads all user name from disc to a Set
	//@param usernames: the set where all user name will be saved
	public static void load_Usernames(Set<String> usernames) throws JsonParseException, IOException, IllegalArgumentException {
		if(usernames == null)
			throw new IllegalArgumentException();
		JsonFactory jsonFact=new JsonFactory();
		JsonParser jsonPar=jsonFact.createParser(new File(FilesNames.ALL_USERNAMES));
		while(jsonPar.nextToken() != JsonToken.END_ARRAY) {
			String name_taken= jsonPar.getCurrentName();
			usernames.add(name_taken);
		}
	}
	
}
