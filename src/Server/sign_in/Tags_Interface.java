/**
 * 
 */
package sign_in;

import java.util.Iterator;

/**
 * @author nedo1993
 *
 */
public interface Tags_Interface {
	public static int MAX_NUM_OF_TAGS=5;// Maximum number of tags allowed per user.
	public boolean add_tag(String tag) throws IllegalArgumentException, TooManyTagsException;
	public String toString();
	public Iterator<String> iterator();
}
