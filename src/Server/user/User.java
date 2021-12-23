/**
 * 
 */
package user;

import java.util.Iterator;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import sign_in.Tags;

/**
 * @author nedo1993
 *
 */
@JsonPropertyOrder({"user_name", "hashed_password", "salt", "tags"})
public class User implements User_Interface {
	/*
	 * Overview: A class that represents the generic user
	 */
	private Tags tags;//all tags specified by the user
	private String user_name;//user name specified by the user
	@SuppressWarnings("unused")
	private String salt;//random string to be concatenated with the password
	@SuppressWarnings("unused")
	private String hashed_pas;//hashed password
	//@Requires: usern_name != null, tags_str != NULL, id_user >=0
	//@Throws: IllegalArgumentException
	//@Modifies: this
	//@Effects: initializes the user object
	//@param user_name: the user name to be saved
	//@param tags: all tags specified by the user
	public User(String user_name, Tags tags, String salt, String hashed_pas) throws IllegalArgumentException {
		if(user_name == null || tags==null || salt == null || hashed_pas == null)
			throw new IllegalArgumentException();
		this.tags = tags;
		this.user_name=user_name;
		this.salt=salt;
		this.hashed_pas=hashed_pas;
	}
	//@Effects: returns the user name
	@JsonGetter
	public String getUser_name() {
		return this.user_name;
	}
	//@Effects: returns all the tags as a string
	@JsonGetter
	public String getTags() {
		return this.tags.toString();
	}
	//@Effects: returns the salt used to hash the password
	@JsonGetter
	public String getSalt() {
		return this.salt;
	}
	//@Effects: returns the hashe passwords
	@JsonGetter
	public String getHashed_password() {
		return this.hashed_pas;
	}
	//@Effects: returns an iterator of all tags
	@JsonIgnore
	public Iterator<String> getTagsIter(){
		return this.tags.iterator();
	}
}
