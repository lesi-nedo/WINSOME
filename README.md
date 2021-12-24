## General
Hi people,
This is the project of Laboratorio di Reti.

## Text
<object data="ProgettoWINSOME_v2.pdf" type="application/pdf" width="700px" height="700px">
    <embed src="ProgettoWINSOME_v2.pdf">
        <p>This browser does not support PDFs. Please download the PDF to view it: <a href="ProgettoWINSOME_v2.pdf">Download PDF</a>.</p>
    </embed>
</object>

## Feedback
Any comments or bugs report is welcome

## Note

You'll need to create and sign a certificate, to do that go to the directory `/src/Server/ssl`, open a terminal and type:  
1. Generate keystore:  
`keytool -genkey -alias bmc -keyalg RSA -keystore KeyStore.jks -keysize 2048`  
2. Generate new ca-cert and ca-key:  
`openssl req -new -x509 -keyout ca-key -out ca-cert`  
3. Extracting cert/creating cert sign req(csr):  
`keytool -keystore KeyStore.jks -alias bmc -certreq -file cert-file`   
4. Sign the “cert-file” and cert-signed wil be the new cert:  
`openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial -passin pass:yourpass`  
5. Importing the ca-cert to keystore file:  
`keytool -keystore KeyStore.jks -alias CARoot -import -file ca-cert` 
6. Import cert-signed to keystore:  
`keytool -keystore KeyStore.jks -alias bmc -import -file cert-signed`  
7. Add ca-cert into the truststore:  
`keytool -keystore truststore.jks -alias bmc -import -file ca-cert`
  
Now go to the directory `/src/Client/ssl` and repeat the steps 1 through 7 (Choose a different name for the files eg: ca-cert-c). 
 
8. Add ca-cert (server certificate) itnto the client truststore:  
`keytool -keystore truststore.jks -alias bmc -import -file ../../Server/ssl/ca-cert`  
9. Add ca-cert-c (client certificate) into server truststore:  
`keytool -keystore ../../Server/ssl/truststore.jks -alias bmc -import -file ca-cert-c`

Do not forget to update the variable in `src/Server/utils/StaticNames` called `PASS_SSL` with the password used to generate the certificate.  
Now you'll need to add the Root CA server certificate (ca-cert) to the java cacerts key store.

The Root CA is located in `src/Server/ssl/` and the name is: `ca-cert`.

To add the certificate to the cacerts go to the location `eg: "C:\Program Files\Java\jdk1.8.0_221\jre\lib\security` in Windows or `/lib/jvm/java-11-openjdk-amd64/lib/security` in Linux where the cacerts is present and open the command prompt to execute the following command.

`keytool -import -alias aliasName* -file pathToRootCA.crt** -keystore cacerts`

Password is `changeit`

\* any name  
\*\* `SOME_PATH/src/Server/ssl/ca-cert`
