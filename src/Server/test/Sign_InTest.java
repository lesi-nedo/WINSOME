package test;

import static org.junit.jupiter.api.Assertions.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Random;

import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import sign_in.Sign_In;
import sign_in.Sign_In_Interface;
import sign_in.Tags_Interface;
import sign_in.TooManyTagsException;
import utils.StaticNames;
import utils.StaticNames_Client;
import utils.User_Data;

@Execution(ExecutionMode.CONCURRENT)
class Sign_InTest {
	static final String REG_NAME= "localhost";
	static final String SER_NAME="SIGN_IN";
	static final int PORT=2021;
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

	@Test
	@DisplayName("Test with password == null")
	void testPas() {
		System.out.println("FirstParallelUnitTest testPas() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			serObj.register("test" + generateString(5), null, generateString(20) + " " + generateString(6) + " " + generateString(10));
		});
		assertTrue(e.getMessage().contains("Argument can not be null"));
		
	}
	
	@Test
	@DisplayName("Test with username == null")
	void testUser() {
		System.out.println("FirstParallelUnitTest testUser() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			serObj.register(null, generateString(5), generateString(20) + " " + generateString(6) + " " + generateString(10));
		});
		assertTrue(e.getMessage().contains("Argument can not be null"));
	}
	
	@Test
	@DisplayName("Test with tags == null")
	void testTags() {
		System.out.println("FirstParallelUnitTest testTags() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(IllegalArgumentException.class, () -> {
			serObj.register("test" + generateString(5), generateString(5), null);
		});
		assertTrue(e.getMessage().contains("Argument can not be null"));
	}
	
	@Test
	@DisplayName("Test with tags > 5")
	void testTagsLimit() {
		System.out.println("FirstParallelUnitTest testPasLimit() start => " + Thread.currentThread().getName());
		Sign_In_Interface serObj = init_client();
		Exception e = assertThrows(TooManyTagsException.class, () -> {
			serObj.register("test" + generateString(5), generateString(5), generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20) + " " + generateString(20));
		});
		System.out.println(e.getMessage());
		assertTrue(e.getMessage().contains("Maxium number of tags allowed is: " + String.valueOf(Tags_Interface.MAX_NUM_OF_TAGS)));
	}

}
