/**
 * 
 */
package sign_in;

/**
 * @author nedo1993
 *
 */
public class TooManyTagsException extends Exception {

	/**
	 * Overview: Exception to let know the user has inserted too many tags.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param: string to be printed.
	 */
	public TooManyTagsException(String msg) {
		// TODO Auto-generated constructor stub
		super(msg);
	}
}
