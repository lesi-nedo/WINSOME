package utils;

import com.fasterxml.jackson.core.JsonEncoding;

public class StaticNames {
	/*
	 * This is a class for storing static names for different purpose, e.g names of json files
	 */
	public static final String POSTS_REWIN="post_rewin.json";
	public static final String PATH_TO_PROFILES="src/Server/User_Data/Profiles/";//path to the folder where each user will have own directory 
	public static final String NAME_JSON_USER="about_user.json";//the json file name containing all information on the user 
	public static final String PASS_SSL="password";//password used for keytool command and openssl 
	public static final String POSTS="posts.json";// json file where all posts are saved written by x
	public static final String PATH_TO_TAGS="src/Server/User_Data/Tags/";// json file where all tags are saved and all user name that have indicated
	public static final JsonEncoding ENCODING=JsonEncoding.UTF16_BE; // the encoding of all json files
	public static final String PATH_TO_SSL="src/Server/ssl/";//path to where certificates are stored
	public static final String KEYSTORE_NAME="KeyStore.jks";
	public static final String TRUSTORE_NAME="truststore.jks";
	public static final String NAME_FILE_TAG="users.json";
	public static final String NAME_FILE_TAG_TEMP="users_temp";
	public static final String NAME_FILE_FOL_UPD="not_notified.json";
	public static final String NAME_FILE_FOL_UPD_TEMP="not_notified";

}
