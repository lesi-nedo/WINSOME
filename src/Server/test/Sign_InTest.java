package test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.core.JsonParseException;

import sign_in.Sign_In;
import sign_in.Sign_In_Interface;
import sign_in.Tags_Interface;
import sign_in.TooManyTagsException;
import sign_in.UsernameAlreadyExistsException;
import utils.StaticNames;
import utils.StaticNames_Client;
import utils.User_Data;

@Execution(ExecutionMode.CONCURRENT)
class Sign_InTest {
	private static final String REG_NAME= "localhost";
	private static final String SER_NAME="SIGN_IN";
	private static final int NUM_USERS_TO_TEST=20;
	private static final int PORT=2021;
	private static Sign_In_Interface init_client() {
		User_Data.setSettings_Server(StaticNames_Client.PATH_TO_SSL);
		Sign_In_Interface serObj=null;
		try {
			Registry r = LocateRegistry.getRegistry(REG_NAME, PORT, new SslRMIClientSocketFactory());
			serObj=(Sign_In_Interface) r.lookup(SER_NAME);
		} catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
		return serObj;
	}
	@BeforeAll
	public static void setup() {
		User_Data.setSettings_Server(StaticNames.PATH_TO_SSL);
		ConcurrentMap<String, ReadWriteLock> tags_in_mem=new ConcurrentHashMap<String, ReadWriteLock>();
		ConcurrentMap<String, ReadWriteLock> usernames=new ConcurrentHashMap<String, ReadWriteLock>();
		try {
			User_Data.load_Tags(tags_in_mem);
			User_Data.load_Usernames(usernames);
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Sign_In sign_ser;
		SslRMIClientSocketFactory csf=new SslRMIClientSocketFactory();
		SslRMIServerSocketFactory ssf=new SslRMIServerSocketFactory(null, null, true);
		Registry reg;		
		try {
			sign_ser=new Sign_In(PORT, tags_in_mem, usernames);
			LocateRegistry.createRegistry(PORT, csf, ssf);
			reg=LocateRegistry.getRegistry(REG_NAME, PORT, new SslRMIClientSocketFactory());
			reg.bind(SER_NAME, sign_ser);
		} catch (Exception e) {
			// TODO Auto-generated catch block
            System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
		System.out.println("Started RMI Registry");
	}

	@DisplayName("Test with password incorrect")
	@ParameterizedTest
	@CsvSource({
		"&test_user&2222, , '&test_tag&i &test_tag&love &test_tag&Pisa'",
		"&test_user&444,  ' ', '&test_tag&this &test_tag&is &test_tag&incorrect'",
		"&test_user&777, yeah, '&test_tag&happy &test_tag&now'"
	})
	void testPas(String username, String password, String tags) {
		System.out.println("FirstParallelUnitTest testPas() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			serObj.register(username, password, tags);
		});
		assertTrue(e.getMessage().contains("Incorrect argument"));
		
	}
	
	@DisplayName("Test with two same username")
	@Test
	void testSameUser() {
		System.out.println("FirstParallelUnitTest testPas() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		try {
			serObj.register("&test_user&1", "some_password", "&test_tag&i &test_tag&love &test_tag&Pisa");
		} catch (IllegalArgumentException | RemoteException | UsernameAlreadyExistsException
				| TooManyTagsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		Exception e = assertThrows(UsernameAlreadyExistsException.class, () -> {
			serObj.register("&test_user&1", "some_password", "&test_tag&can &test_tag&you &test_tag&hear &test_tag&me");
		});
		assertTrue(e.getMessage().contains("User name is taken."));
		
	}
	
	@DisplayName("Test with incorrect username")
	@ParameterizedTest
	@CsvSource({
		"' ', some_password, '&test_tag&i &test_tag&love &test_tag&Pisa'",
		",  some_password, '&test_tag&this &test_tag&is &test_tag&incorrect'",
		"' &test_user&777', some_password, '&test_tag&happy &test_tag&now'"
	})
	void testUser(String username, String password, String tags) {
		System.out.println("FirstParallelUnitTest testUser() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			serObj.register(username, password, tags);
		});
		assertTrue(e.getMessage().contains("Incorrect argument"));
	}
	
	@DisplayName("Test with incorrect tags")
	@ParameterizedTest
	@CsvSource({
		"&test_user&799, some_password, ' '",
		"&test_user&777,  some_password,",
	})
	void testTags(String username, String password, String tags) {
		System.out.println("FirstParallelUnitTest testTags() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			serObj.register(username, password, tags);
		});
		assertTrue(e.getMessage().contains("Incorrect argument"));
	}
	
	@Test
	@DisplayName("Test with tags > 5")
	void testTagsLimit() {
		System.out.println("FirstParallelUnitTest testPasLimit() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(TooManyTagsException.class, () -> {
			serObj.register("&test_user&777" + User_Data.generateString(5), User_Data.generateString(5), "&test_tag&" + User_Data.generateString(20) + " " + "&test_tag&" + User_Data.generateString(20) + " " + "&test_tag&" + User_Data.generateString(20) + " " + "&test_tag&" + User_Data.generateString(20) + " " + "&test_tag&" + User_Data.generateString(20) + " " + "&test_tag&" + User_Data.generateString(20) + " " + "&test_tag&" + User_Data.generateString(20));
		});
		assertTrue(e.getMessage().contains("Maxium number of tags allowed is: " + String.valueOf(Tags_Interface.MAX_NUM_OF_TAGS)));
	}
	@DisplayName("Test with correct user")
	@ParameterizedTest(name= "{index} ==> {arguments} ")
	@MethodSource("genUsers")
	void testCorrect(String username, String password, String tags) {
		System.out.println("FirstParallelUnitTest testCorrect() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		try {
			Thread.sleep((new Random()).nextInt(400)+400);
			assertEquals(Sign_In_Interface.CREATED, serObj.register(username, password, tags));
		} catch (IllegalArgumentException | RemoteException | UsernameAlreadyExistsException | TooManyTagsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static Stream<Arguments> genUsers(){
		Stream.Builder<Arguments> builder = Stream.builder();
		for(int i=0; i < NUM_USERS_TO_TEST; i++) {
			builder.add(Arguments.arguments("&test_user&" + i + User_Data.generateString(6), User_Data.generateString(10), "&test_tag&" + User_Data.generateString(4) + " " + "&test_tag&" + User_Data.generateString(10) + " " + "&test_tag&" + User_Data.generateString(20)));
		}
		return builder.build();
	}
	
	@AfterAll
	public static void cleanUp() {
		try {
			Stream.of((new File((StaticNames.PATH_TO_PROFILES))).listFiles()).forEach(path -> {
				if(path.isDirectory()) {
					String file_name=null;
					file_name=path.getName().toString();
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

}
