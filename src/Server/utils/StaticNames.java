package utils;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	public static final Lock PROFILES_LOCK=new ReentrantLock();//lock associated to the json file where all profiles are saved
	public static final String PROFILES="src/Server/User_Data/profiles.json";//json file where all profiles are saved
	public static final String TAGS="tags.json";// json file where all tags are saved and all user name that have indicated
	public static final Lock USERNAMES_LOCK=new ReentrantLock();// lock associated to the json file where all user name are saved
	public static final String ALL_USERNAMES="src/Server/User_Data/all_usernames.json";//the name of the file where all active user name are saved,
	public static final JsonEncoding ENCODING=JsonEncoding.UTF16_BE; // the encoding of all json files
	public static final String PATH_TO_SSL="src/Server/ssl/";
	public static final String KEYSTORE_NAME="KeyStore.jks";
	public static final String TRUSTORE_NAME="truststore.jks";
}
