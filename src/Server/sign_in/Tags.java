/**
 * 
 */
package sign_in;

import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
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
	public Tags(String tags, String delim) throws TooManyTagsException {
		this.tags=new TreeSet<String>();
		StringTokenizer toks = new StringTokenizer(tags, delim);
		while(toks.hasMoreTokens()) {
			String token=toks.nextToken();
			if(num_tags>5)
				throw new TooManyTagsException("Maxium number of tags allowed is: " + String.valueOf(Tags_Interface.MAX_NUM_OF_TAGS));
			this.tags.add(token);
		}
	}
	/*
	 * @Requires: tag != null
	 * @Throws: IllegalArgumentException, TooManyTagsException
	 * @Modifies: tags set
	 * @Effects: Inserts a tag to the set of tags.
	 * @Returns: true if the tag was added false otherwise
	 * @param tag is a String specified by the user to be associated to his/her profile
	 */
	public boolean add_tag(String tag) throws TooManyTagsException {
		if(tag==null) throw new IllegalArgumentException();
		if(!this.tags.add(tag.toLowerCase()))
			return false;
		this.num_tags++;
		if(this.num_tags>Tags_Interface.MAX_NUM_OF_TAGS) throw new TooManyTagsException("Maxium number of tags allowed is: " + String.valueOf(Tags_Interface.MAX_NUM_OF_TAGS));
		return true;
	}
	
	/*
	 * @Effects: returns all tags as a string
	 * @Returns: the tags in String format
	 */
	@JsonValue
	public String toString() {
		return String.join(" ", this.tags);
	}
	
	/*
	 * @Effects: returns an iterator
	 * @Returns: the iterator of the tags
	 */
	public Iterator<String> iterator(){
		return this.tags.iterator();
	}
}
