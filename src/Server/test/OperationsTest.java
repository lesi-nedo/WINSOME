package test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonParseException;

import sign_in.Sign_In;
import sign_in.Sign_In_Interface;
import sign_in.Tags;
import sign_in.TooManyTagsException;
import sign_in.UsernameAlreadyExistsException;
import utils.StaticNames;
import utils.User_Data;
import winServ.Operations;
import winServ.Result;

@Execution(ExecutionMode.CONCURRENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OperationsTest {
	private static final int NUM_USERS_TO_TEST = 40;//the number of users to be inserted before the test
	private static final int NUM_USERS_SAME_COLON=4; //number of users that will have the same tag to test list_users
	private static final int NUM_TEST_FOLLOWERS=20;//the number of user that will follow someone
	private static final int NUM_USERS_SAME_ROW=3; //is fixed, the number of users groups with same tags
	private static final int NUM_OF_POSTS=100;//number of created posts, of posts rewinded, of blogs shown, of feed returned, and posts
	private static final int NUM_DEL_POSTS=30; //this how many posts will be deleted
	private ConcurrentMap<String, ReadWriteLock> usernames=new ConcurrentHashMap<String, ReadWriteLock>();//all previously specified usernames
	private String[] all_followers = new String[NUM_TEST_FOLLOWERS];//will contain all users that are followers
	private ArrayList<String> all_fol_posted=new ArrayList<String>();//all users who posted at least one post
	private ConcurrentMap<String, ReadWriteLock> tags_in_mem=new ConcurrentHashMap<String, ReadWriteLock>();
	private HashMap<String, String> user_pas = new HashMap<String, String>();
	private ConcurrentMap<String, String> logged_users=new ConcurrentHashMap<String, String>();//all logged users
	private String[][] users_same_tag = new String[NUM_USERS_SAME_ROW][NUM_USERS_SAME_COLON];//each column will contain users who has at least one tag in common 
	private final int PORT=6969;

	@BeforeAll
	void ini() {
		System.out.println("Preparation has started");
		for(int i=0; i < NUM_USERS_TO_TEST; i++) {
			insertUser("&test_user&" + i + User_Data.generateString(6), User_Data.generateString(10), "&test_tag&" + User_Data.generateString(4) + " " + "&test_tag&" + User_Data.generateString(10) + " " + "&test_tag&" + User_Data.generateString(20));
		}
		System.out.println("Preparation has ended");
	}
	void insertUser(String username, String password, String tags) {
		try {
			Sign_In si =new Sign_In(PORT,  this.tags_in_mem, this.usernames);
			this.user_pas.putIfAbsent(username, password);
			assertEquals(Sign_In_Interface.CREATED, si.register(username, password, tags));
		} catch (IllegalArgumentException | RemoteException | UsernameAlreadyExistsException | TooManyTagsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@DisplayName("Test the login method")
	@ParameterizedTest()
	@MethodSource("users_and_pas")
	void test_login(String username, String password) {
		try {
			assertEquals(200, Operations.login(username, password, usernames, this.logged_users).getResult());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@DisplayName("Test the login method with incorrect password")
	@ParameterizedTest()
	@MethodSource("users_and_pas")
	void test_login_inc_pas(String username, String password) {
		try {
			assertEquals(400, Operations.login(username, password+"TEST", usernames, this.logged_users).getResult());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@DisplayName("Test the login method with username not present")
	@ParameterizedTest()
	@MethodSource("users_and_pas")
	void test_login_inc_user(String username, String password) {
		try {
			Result res =Operations.login(username+"TEST", password+"TEST", usernames, this.logged_users);
			assertEquals(404, res.getResult());
			assertEquals("{\"reason\":\"Username does not exists\"}", res.getReason());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@DisplayName("Test the login method with incorrect input")
	@ParameterizedTest()
	@CsvSource({","})
	void test_login_inc_val(String username, String password) {
		Exception e = assertThrows(IllegalArgumentException.class, ()-> {
			Operations.login(username, password, usernames, logged_users);
		});
		assertEquals("Incorrect input", e.getMessage());

	}
	
	@DisplayName("Test the logout method")
	@ParameterizedTest()
	@MethodSource("users_and_pas")
	void test_logout(String username, String password) {
		if(this.logged_users.containsKey(username)) {
			Result res =Operations.logout(username, this.logged_users);
			assertEquals(202, res.getResult());
			assertEquals("{\"reason\":\"The user have successfully logged out\"}", res.getReason());
		}
		assertFalse(this.logged_users.containsKey(username));
	}
	
	@DisplayName("Test the logout method with use logged out")
	@ParameterizedTest()
	@MethodSource("users_and_pas")
	void test_logout_user_out(String username, String password) {
		if(!this.logged_users.containsKey(username)) {
			Result res =Operations.logout(username, this.logged_users);
			assertEquals(400, res.getResult());
			assertEquals("{\"reason\":\"The user was not logged in\"}", res.getReason());
		}
	}
	
	@DisplayName("Test list users with at least one tag in common")
	@ParameterizedTest()
	@MethodSource("create_users_with_tags_in_common")
	void test_list_users(String username) {
		try {
			int total=0;//if the users have one tag in common than this variable should be >0
			Result res = Operations.list_users(username, this.usernames, this.tags_in_mem);
			assertEquals(200, res.getResult());
			Tags tags = Operations.getUserTags(new File(StaticNames.PATH_TO_PROFILES+username+"/"+StaticNames.NAME_JSON_USER), usernames.get(username).readLock());
			assertNotNull(tags);
			Iterator<String> iter = tags.iterator();
			while(iter.hasNext())
				if(res.getReason().contains(iter.next()))
					total++;
			assertTrue(total>0);
		} catch (IOException | TooManyTagsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@DisplayName("Test list users with at least one tag in common but with wrong argument")
	@ParameterizedTest()
	@MethodSource("create_users_with_tags_in_common")
	void test_list_users_not_correct(String username) {
		try {
			Result res = Operations.list_users(username+"BELLO", this.usernames, this.tags_in_mem);
			assertEquals(404, res.getResult());
		} catch (IOException | TooManyTagsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	@DisplayName("Test follow_user")
	void test_follow() {
		Random rand = new Random();
		int upper = 100;//the upper limit of users that will be followed 
		int lower=10;//le lower limit
		int num_to_follow=rand.nextInt(upper)+lower;
		String follow=null;
		String us_to_test=null;
		ArrayList<String> users = new ArrayList<String>(usernames.keySet());
		for(int i =0; i < NUM_TEST_FOLLOWERS; i++) {
			us_to_test = users.get(rand.nextInt(users.size()));
			all_followers[i]=us_to_test;
			for(int j=0; j<num_to_follow; j++) {
				follow = users.get(rand.nextInt(users.size()));
				int already_a_fol=0;
				if(!follow.equals(us_to_test)) {
					try {
						if(Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+us_to_test+"/Following/"+follow))){
							already_a_fol=1;
						}
						Result res =Operations.follow_user(us_to_test, follow, usernames.get(us_to_test), usernames.get(follow));
						if(already_a_fol==0) {
							assertEquals(200, res.getResult());
						} else {
							assertEquals(400, res.getResult());
						}
						assertTrue(Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+follow+"/Followers/"+us_to_test)));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		try {
			if(us_to_test != null && follow != null && us_to_test != follow) {
				Result res =Operations.follow_user(us_to_test, follow, usernames.get(us_to_test), usernames.get(follow));
				assertEquals(400, res.getResult());
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	@DisplayName("Test list_following")
	void test_following() {
		try {
			Thread.sleep(400);//this needed if the test is run with ExecutionMode.CONCURRENT
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(String f: all_followers) {
			if(f!=null) {
				try {
					Result re = Operations.list_following(f, usernames.get(f));
					assertEquals(200, re.getResult());
					assertTrue(re.getReason().length()>2);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		try {
			assertEquals(404, Operations.list_following("ddddd", new ReentrantReadWriteLock()).getResult());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	@DisplayName("Test unfollow_user")
	void test_unfollow() {
		Random rand = new Random();
		String us=null;
		String us_unf=null;
		try {
			Thread.sleep(400);//this needed if the test is run with ExecutionMode.CONCURRENT
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(String f: all_followers) {
			if(f!=null) {
				us=f;
				File[] folls= (new File(StaticNames.PATH_TO_PROFILES+f+"/Following")).listFiles();
				if(folls != null) {
					if(folls.length == 0)
						continue;
					us_unf=folls[rand.nextInt(folls.length)].getName();
					try {
						Result res = Operations.unfollow_user(f, us_unf, usernames.get(f), usernames.get(us_unf));
						assertEquals(200, res.getResult());
						assertTrue(!Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+f+"/Following/"+us_unf)));
						assertTrue(!Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+us_unf+"/Followers/"+f)));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		try {
			if(us_unf != null && us !=null)
				assertEquals(400, Operations.unfollow_user(us, us_unf, usernames.get(us), usernames.get(us_unf)).getResult());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	@DisplayName("Test create_post")
	void test_create_post() {
		String rand_usr=null;
		Random rand=new Random();
		ArrayList<String> users=new ArrayList<String>(usernames.keySet());
		for(int i=0; i<NUM_OF_POSTS; i++) {
			rand_usr=users.get(rand.nextInt(users.size()));
			all_fol_posted.add(rand_usr);
			String title=User_Data.generateString(7);
			String content=User_Data.generateString(20);
			try {
				Result res = Operations.createPost(rand_usr, title, content, usernames.get(rand_usr));
				assertEquals(201, res.getResult());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		try {
			if(rand_usr !=null) assertEquals(400, Operations.createPost(rand_usr, "GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG", "hey baby", usernames.get(rand_usr)).getResult());
			assertEquals(404, Operations.createPost("DOENOTEXISTS", "oxxxymiron", "rapper", new ReentrantReadWriteLock()).getResult());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	@DisplayName("Test rewin_post")
	void test_rewin_post() {
		String rand_usr=null;
		Random rand=new Random();
		String rand_author=null;
		String rand_id=null;
		try {
			while(all_fol_posted.size() == 0)Thread.sleep(400);//this needed if the test is run with ExecutionMode.CONCURRENT
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ArrayList<String> users=new ArrayList<String>(usernames.keySet());
		for(int i=0; i<NUM_OF_POSTS; i++) {
			rand_usr=users.get(rand.nextInt(users.size()));
			rand_author=all_fol_posted.get(rand.nextInt(all_fol_posted.size()));
			File[] file =new File(StaticNames.PATH_TO_PROFILES+rand_author+"/Posts").listFiles();
			int exists=0;
			int rewinded=0;
			try {
				if(file != null) {
					if(file.length == 0)
						continue;
					rand_id=file[rand.nextInt(file.length)].getName();
					if(Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+rand_usr+"/Posts/"+rand_id)))
						rewinded=1;
					Result res = Operations.rewin_post(rand_usr, rand_id, usernames.get(rand_usr));
					if(Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+rand_author+"/Posts/"+rand_id))) {
						exists=1;
						if(Files.exists(Paths.get(StaticNames.PATH_TO_POSTS+rand_id))==false) {
							exists=0;
						}
					}
					if(exists==1 && rewinded ==0) {
						assertEquals(200, res.getResult());
						assertTrue(Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+rand_usr+"/Posts/"+rand_id)));
					}
					if(res.getResult()== 400)  assertTrue(Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+rand_usr+"/Posts/"+rand_id)));
				}
			} catch (NullPointerException e) {
				System.out.println("Post was deleted.");
				e.printStackTrace();
			}
			
		}
	
		if(rand_usr !=null) assertEquals(404, Operations.rewin_post(rand_usr, "oxxxymiron", usernames.get(rand_usr)).getResult());
	}
	
	
	@Test
	@DisplayName("Test view_blog")
	void test_view_blog() {
		try {
			while(all_fol_posted.size() == 0)
				Thread.sleep(400);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String rand_usr=null;
		Random rand=new Random();
		for(int i=0; i<NUM_OF_POSTS; i++) {
			rand_usr=all_fol_posted.get(rand.nextInt(all_fol_posted.size()));
			try {
				Result res = Operations.view_blog(rand_usr, usernames.get(rand_usr));
				assertEquals(200, res.getResult());
				Pattern pat = Pattern.compile("(?<=\"author\":\")(.*?)(?=\")");//retrieves only the author of the post
				Matcher match = pat.matcher(res.getReason());
				while(match.find()) {
					assertTrue(match.group().equals(rand_usr));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		try {
			assertEquals(404, Operations.view_blog("DOENOTEXISTS", new ReentrantReadWriteLock()).getResult());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	@DisplayName("Test show_feed")
	void test_show_feed() {
		try {
			while(all_fol_posted.size() == 0)
				Thread.sleep(400);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String rand_usr=null;
		Random rand=new Random();
		for(int i=0; i<NUM_OF_POSTS; i++) {
			rand_usr=all_fol_posted.get(rand.nextInt(all_fol_posted.size()));
			try {
				Result res = Operations.show_feed(rand_usr, usernames.get(rand_usr));
				assertEquals(200, res.getResult());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		try {
			assertEquals(404, Operations.show_feed("DOENOTEXISTS", new ReentrantReadWriteLock()).getResult());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	@DisplayName("Test show_post")
	void test_show_post() {
		String rand_usr=null;
		Random rand=new Random();
		String rand_author=null;
		String rand_id=null;
		try {
			while(all_fol_posted.size() == 0)Thread.sleep(400);//this needed if the test is run with ExecutionMode.CONCURRENT
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		ArrayList<String> users=new ArrayList<String>(usernames.keySet());
		for(int i=0; i<NUM_OF_POSTS; i++) {
			rand_usr=users.get(rand.nextInt(users.size()));
			rand_author=all_fol_posted.get(rand.nextInt(all_fol_posted.size()));
			File[] file =new File(StaticNames.PATH_TO_PROFILES+rand_author+"/Posts").listFiles();
			int exists=0;
			try {
				if(file != null) {
					if(file.length == 0)
						continue;
					rand_id=file[rand.nextInt(file.length)].getName();
					Result res = Operations.show_post(rand_usr, rand_id, usernames.get(rand_usr));
					if(Files.exists(Paths.get(StaticNames.PATH_TO_PROFILES+rand_author+"/Posts/"+rand_id+"/"+StaticNames.NAME_FILE_POST))) {
						exists=1;
					}
					if(exists==1) {
						assertEquals(200, res.getResult());
					}
				}
			} catch (NullPointerException e) {
				System.out.println("Post was deleted.");
				e.printStackTrace();
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	
		if(rand_usr !=null) assertEquals(404, Operations.rewin_post(rand_usr, "oxxxymiron", usernames.get(rand_usr)).getResult());
	}
	
	@Test
	@DisplayName("Test delete_post")
	void test_delete_post() {
		Random rand=new Random();
		String rand_author=null;
		String rand_id=null;
		File rand_file = null;
		try {
			while(all_fol_posted.size() == 0)Thread.sleep(400);//this needed if the test is run with ExecutionMode.CONCURRENT
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(int i=0; i<NUM_DEL_POSTS; i++) {
			rand_author=all_fol_posted.get(rand.nextInt(all_fol_posted.size()));
			File[] file =new File(StaticNames.PATH_TO_PROFILES+rand_author+"/Posts").listFiles();
			int exists=0;
			try {
				if(file != null) {
					if(file.length == 0)
						continue;
					rand_id=(rand_file=file[rand.nextInt(file.length)]).getName();
					if(Files.exists(Paths.get(StaticNames.PATH_TO_POSTS+rand_id))) {
						exists=1;
					}
					Result res = Operations.delete_post(rand_author, rand_id, usernames.get(rand_author));
					if(exists==1) {
						if(Files.isSymbolicLink(rand_file.toPath())) {
							assertEquals(401, res.getResult());
						} else {
							assertEquals(202, res.getResult());
						}
					}
				}
			} catch (NullPointerException e) {
				System.out.println("Post was deleted.");
				e.printStackTrace();
			} catch (JsonParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	
		assertEquals(404, Operations.rewin_post(rand_author, "oxxxymiron", usernames.get(rand_author)).getResult());
	}
	
	@AfterAll
	public static void cleanUp() {
		try {
			Stream.of((new File((StaticNames.PATH_TO_PROFILES)).listFiles())).forEach(path -> {
				if(path.isDirectory() || Files.isSymbolicLink(path.toPath())) {
					String file_name=null;
					file_name=path.getName();
					if(file_name.startsWith("&test_user&"))
						User_Data.deleteDir(new File(StaticNames.PATH_TO_PROFILES + file_name));
				}
			});
			Stream.of((new File(StaticNames.PATH_TO_TAGS)).listFiles()).forEach(path -> {
				if(path.isDirectory()) {
					String file_name=null;
					file_name=path.getName();
					if(file_name.startsWith("&test_tag&"))
						User_Data.deleteDir(new File(StaticNames.PATH_TO_TAGS + file_name));
				}
			});
			Stream.of((new File(StaticNames.PATH_TO_POSTS)).listFiles()).forEach(path -> {
				if(Files.isSymbolicLink(path.toPath())) {
					try {
						if(!path.exists())Files.delete(path.toPath());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						System.err.println("Could not delete a symbolic link: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Stream<Arguments> create_users_with_tags_in_common() {
		Stream.Builder<Arguments> builder = Stream.builder();
		String tag1 = "&test_tag&ricci";
		int j_1=0;
		int j_2=0;
		int j_3=0;
		String tag2 = "&test_tag&potere";
		String tag3 = "&test_tag&3minute";
		for(int i = 0; i<(NUM_USERS_SAME_COLON*NUM_USERS_SAME_ROW); i++) {
			int mod = i%3;
			String username = "&test_user&"+i+User_Data.generateString(5);
			String pass=User_Data.generateString(8);
			String tags = "&test_tag&"+i+User_Data.generateString(6);
			Sign_In si=null;
			try {
				si = new Sign_In(PORT,  this.tags_in_mem, this.usernames);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(mod==0) {
				tags = tags+" " + tag1;
				users_same_tag[mod][j_1]=username;
				j_1++;
				j_1=(j_1)%NUM_USERS_SAME_COLON;
			} else if(mod==1) {
				tags = tags+" " + tag2;
				users_same_tag[mod][j_2]=username;
				j_2++;
				j_2=(j_2)%NUM_USERS_SAME_COLON;
			} else {
				tags = tags+" " + tag3;
				users_same_tag[mod][j_3]=username;
				j_3++;
				j_3=(j_3)%NUM_USERS_SAME_COLON;
			}
			builder.add(Arguments.arguments(username));
			
			try {
				si.register(username, pass, tags);
			} catch (RemoteException | UsernameAlreadyExistsException | TooManyTagsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return builder.build();
	}
	
	public Stream<Arguments> users_and_pas(){
		Stream.Builder<Arguments> build = Stream.builder();
		this.user_pas.forEach((k,v)-> {
			build.add(Arguments.arguments(k, v));
		});
		return build.build();
	}

}
