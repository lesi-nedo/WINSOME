package utils;

import com.fasterxml.jackson.core.JsonEncoding;

public class StaticNames_Client {
	public static final String PATH_TO_SSL="src/Client/ssl/";
	public static final String KEYSTORE_NAME="KeyStore.jks";//the name of the client's keystore
	public static final String TRUSTSTORE_NAME="truststore.jks";//the name of the client's keystore
	public static final String PATH_TO_FILE_FOLL="/WINSOME/src/Client/"; // path to the json file where all followers will be  stored
	public static final String NAME_FILE_FOLL="/followers.json";//name of the json file where all followers of the client x will be stored
	public static final String NAME_CONF_FILE="src/Client/conf_file.txt";
	public static final String PATH_TO_CLIENT="src/Client/Users/";
	public static final JsonEncoding ENCODING=JsonEncoding.UTF16_BE;
}
