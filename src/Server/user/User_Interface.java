package user;

import java.util.Iterator;

public interface User_Interface {
	public String getUser_name();
	public String getTags();
	public Iterator<String> getTagsIter();
	public String getHashed_password();
}
