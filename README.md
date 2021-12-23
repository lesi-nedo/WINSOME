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
To use you need to add the Root CA server certificate to the java cacerts key store.

The Root CA is located in `src/Server/ssl/` and the name is: `ca-cert`.

To add the certificate to the cacerts go to the location `eg: "C:\Program Files\Java\jdk1.8.0_221\jre\lib\security` where the cacerts is present and open the command prompt to execute the following command.

`keytool -import -alias aliasName* -file pathToRootCA.crt** -keystore cacerts`

Password is `changeit`

* any name
** `SOME PATH/src/Server/ssl/ca-cert`
