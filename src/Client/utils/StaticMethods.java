package utils;

import java.nio.file.Paths;

public class StaticMethods {
	
	

	//@Requires: PATH_TO_SSL != null
	//@Throws: illegalArgumentException
	//@Effects: sets different properties need for the SslRMIClientSocketFactory and SslRMIServerSocketFactory
	//@param PATH_TO_SSL: the path where the certificate is stored
	public static void setSettings_client(String PATH_TO_SSL) {
		if(PATH_TO_SSL==null)
			throw new IllegalArgumentException();
		System.setProperty("javax.net.ssl.debug", "all");
		System.setProperty("javax.net.ssl.keyStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames_Client.KEYSTORE_NAME).toString());
<<<<<<< HEAD
		System.setProperty("javax.net.ssl.keyStorePassword", StaticNames.PASS_SSL);
		System.setProperty("javax.net.ssl.trustStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames_Client.TRUSTSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.trustStorePassword", StaticNames.PASS_SSL);
=======
		System.setProperty("javax.net.ssl.keyStorePassword", StaticNames_Client.PASS_SSL);
		System.setProperty("javax.net.ssl.trustStore", Paths.get(".").resolve(PATH_TO_SSL+StaticNames_Client.TRUSTSTORE_NAME).toString());
		System.setProperty("javax.net.ssl.trustStorePassword", StaticNames_Client.PASS_SSL);
>>>>>>> 0d8d0c3 (updated some stuff, fixed a bug in CalcEarningsThread)
	}

}
