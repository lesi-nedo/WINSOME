package user;

import java.util.Iterator;

public interface User_Interface {
	public String getUsername();
	public String getTags();
	public Iterator<String> getTagsIter();
}
