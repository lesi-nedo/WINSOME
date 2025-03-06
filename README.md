# WINSOME - WINdow to Sharing Opinions and Media Experiences

## Overview
WINSOME is a social media platform that allows users to share posts, receive likes and comments, and earn rewards based on interactions. This project implements a distributed social media service with a client-server architecture.

## Features
- User registration and login system
- Create, view, and delete posts
- Like and comment on posts
- Follow/unfollow other users
- Wallet system with reward calculations
- Multi-threaded server architecture
- RMI for specific operations
- TCP for main client-server communication

## Architecture
The project follows a client-server architecture:
- **Server**: Handles user management, post storage, and reward calculations
- **Client**: Provides a command-line interface for interacting with the platform
- **RMI**: Used for registration and callback services
- **TCP**: Used for main communication protocol

## Installation

### Prerequisites
- Java 11 or higher
- Maven for dependency management

### Setup
1. Clone the repository:
   ```
   git clone https://github.com/yourusername/WINSOME.git
   cd WINSOME
   ```

2. Compile the project:
   ```
   mvn clean package
   ```

## Usage

### Starting the Server
```
java -cp target/WINSOME-1.0-SNAPSHOT.jar server.ServerMain [config_file]
```

### Starting the Client
```
java -cp target/WINSOME-1.0-SNAPSHOT.jar client.ClientMain [config_file]
```

### Client Commands
- `register <username> <password> <tags>`: Register a new user
- `login <username> <password>`: Login to WINSOME
- `logout`: Logout from the current session
- `list users`: List users with at least one common tag
- `list followers`: List users who follow you
- `list following`: List users you are following
- `follow <username>`: Follow a user
- `unfollow <username>`: Unfollow a user
- `blog`: View your posts
- `post <title> <content>`: Create a new post
- `show feed`: Show recent posts from users you follow
- `show post <id>`: Show details of a post
- `delete <id>`: Delete your post
- `rewin <id>`: Repost someone else's post
- `rate <id> <vote>`: Rate a post (1 for like, -1 for dislike)
- `comment <id> <text>`: Comment on a post
- `wallet`: Show your wallet with rewards
- `wallet btc`: Show wallet converted to Bitcoin

## Configuration
Configuration files can be provided for both client and server:
- `server.properties`: Server configuration
- `client.properties`: Client configuration

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   ├── client/
│   │   ├── server/
│   │   ├── common/
│   │   └── utils/
│   └── resources/
│       ├── server.properties
│       └── client.properties
└── test/
    └── java/
```

## Technologies
- Java
- Java RMI
- TCP Sockets
- JSON for data serialization
- Multi-threading
- Maven

## Author
Oleksiy Nedobiychuk - Bachelor in Computer Science, University of Pisa
