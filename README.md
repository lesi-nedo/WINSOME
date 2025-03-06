# WINSOME

A client-server application with Java RMI and TCP/IP socket communication.

## Overview

WINSOME is a social media platform implemented as a distributed Java application. It uses Java RMI for registration and notification services and TCP/IP sockets for client-server communication.

## Technologies

- Java SE
- Java RMI for registration and notifications
- TCP/IP sockets for client-server communication
- Jackson library for data serialization
- 
## Project Structure

- bin - Compiled class files and configuration
- libs - External libraries and dependencies
- src - Source code
- Client.jar - Compiled client application
- Server.jar - Compiled server application

## Prerequisites

- Java Runtime Environment (JRE) 8 or higher
- Network connection for client-server communication

## Configuration

The application uses two configuration files:
- `conf_file.txt` - Client configuration
- `conf_server.txt` - Server configuration

## Running the Application

### Start the Server
```bash
java -jar Server.jar
```

### Start the Client
```bash
java -jar Client.jar
```
## Client Usage

After starting the client, you can interact with the WINSOME platform using various commands.
Type `exit` to close the client application.


## Documentation

For detailed information about the project:
- See ProgettoWINSOME_v2.pdf for project specifications
- See relazione.pdf for implementation details

## License

This project is licensed under the MIT License