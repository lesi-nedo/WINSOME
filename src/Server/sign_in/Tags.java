/**
 * 
 */
package sign_in;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author nedo1993
 *
 */
public class Tags implements Tags_Interface {
	/*
	 * Overview: Class that contains all tags specified  by the user needed to make an object immutable
	 */
	private Set<String> tags; // All tags specified by the user
	private int num_tags; //keeps the number of tags added
	
	public Tags() {
		this.tags=new TreeSet<String>();
		this.num_tags=0;
	}
	
	/*
	 * @Requires: tag != null
	 * @Throws: IllegalArgumentException, TooManyTagsException
	 * @Modifies: tags set
	 * @Effects: Inserts a tag to the set of tags.
	 * @param tag is a String specified by the user to be associated to his/her profile
	 */
	public boolean add_tag(String tag) throws TooManyTagsException {
		if(tag==null) throw new IllegalArgumentException();
		if(this.num_tags>Tags_Interface.MAX_NUM_OF_TAGS) throw new TooManyTagsException("Maxium number of tags allowed is: " + String.valueOf(Tags_Interface.MAX_NUM_OF_TAGS));
		if(!this.tags.add(tag))
			return false;
		this.num_tags++;
		return true;
	}
	
	/*
	 * @Effects: returns all tags as a string
	 */
	@JsonValue
	public String toString() {
		return String.join(" ", this.tags);
	}
	
	/*
	 * @Effects: returns an iterator
	 */
	public Iterator<String> iterator(){
		return this.tags.iterator();
	}
}
