package test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;
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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

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
	private static final int NUM_USERS_TO_TEST=100;
	private static final int PORT=2021;
	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
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
	public static String generateString(int length) {
	    Random random = new Random();
	    StringBuilder builder = new StringBuilder(length);

	    for (int i = 0; i < length; i++) {
	        builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
	    }

	    return builder.toString();
	}
	@BeforeAll
	public static void setup() {
		User_Data.setSettings_Server(StaticNames.PATH_TO_SSL);

		Sign_In sign_ser;
		SslRMIClientSocketFactory csf=new SslRMIClientSocketFactory();
		SslRMIServerSocketFactory ssf=new SslRMIServerSocketFactory(null, null, true);
		Registry reg;		
		try {
			sign_ser=new Sign_In(PORT);
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
		"&test_user&2222, , 'i love Pisa'",
		"&test_user&444,  ' ', 'this is incorrect'",
		"&test_user&777, yeah, 'happy now'"
	})
	void testPas(String username, String password, String tags) {
		System.out.println("FirstParallelUnitTest testPas() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			serObj.register(username, password, tags);
		});
		assertTrue(e.getMessage().contains("Incorrect argument"));
		
	}
	
	@DisplayName("Test with incorrect username")
	@ParameterizedTest
	@CsvSource({
		"' ', some_password, 'i love Pisa'",
		",  some_password, 'this is incorrect'",
		"' &test_user&777', some_password, 'happy now'"
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
			serObj.register("&test_user&777" + generateString(5), generateString(5), generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20));
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
			builder.add(Arguments.arguments("&test_user&" + i + generateString(6), generateString(10), generateString(4) + " " + generateString(10) + " " + generateString(20)));
		}
		return builder.build();
	}
	
	@AfterAll
	public static void cleanUp() {
		try {
			JsonFactory jsonFact=new JsonFactory();
			File temp_file=new File(StaticNames.ALL_USERNAMES_TEMP);
			File curr_file=new File(StaticNames.ALL_USERNAMES);
			temp_file.createNewFile();
			JsonGenerator jsonGen = jsonFact.createGenerator(temp_file, StaticNames.ENCODING);
			JsonParser jsonPar = jsonFact.createParser(curr_file);
			jsonGen.useDefaultPrettyPrinter();
			
			StaticNames.USERNAMES_LOCK.lock();
			while (jsonPar.nextToken()!=JsonToken.END_ARRAY) {
				String user=jsonPar.getText();
				if(!user.startsWith("&test_user&")) {
					jsonGen.copyCurrentEvent(jsonPar);
				} else {
					User_Data.deleteUserFromProf(user);
					User_Data.deleteDir(new File(StaticNames.PATH_TO_PROFILES+user));
				}
				
			}
			jsonGen.copyCurrentEvent(jsonPar);
			jsonGen.flush();
			curr_file.delete();
			temp_file.renameTo(new File(StaticNames.ALL_USERNAMES));
			StaticNames.USERNAMES_LOCK.unlock();
			jsonGen.close();
			jsonPar.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
