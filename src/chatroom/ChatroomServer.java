package chatroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatroomServer {
    
    private static final int DEFAULT_PORT = 13000;
    private static final int THE_LONELIEST_NUMBER = 1;
    private static final String GOODBYE_MESSAGE = " has left the group!";
    
    private final ServerSocket server;
    private final Map<Socket, String> nameMap = new HashMap<>();
    private int numberOfFriends = 0;
    
    
    /**
     * Create new server for the chatroom
     * @param port port number to open server on, requires 0 <= port <= 65535
     * @throws IOException if IO error occurs during creation of server
     */
    public ChatroomServer(int port) throws IOException {
        server = new ServerSocket(port);
    }
    
    /**
     * Create new server with default port number 1313
     * @throws IOException if IO error occurs during creation of server
     */
    public ChatroomServer() throws IOException {
        server = new ServerSocket(DEFAULT_PORT);
    }
    
    /**
     * Run the server, accepting and handling chatroom connections
     * @throws IOException if an error occurs waiting for a connection
     */
    public void serve() throws IOException {
        while (true) {
            // this method will block until a new socket attempts to connect to server
            Socket newFriendSocket = server.accept();
            numberOfFriends++;
            
            Thread newFriendThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        try {
                            String newFriendName = getFriendName(newFriendSocket);
                            nameMap.put(newFriendSocket, newFriendName);
                            handleInput(newFriendSocket);
                        } finally {
                            newFriendSocket.close();
                            nameMap.remove(newFriendSocket);
                            numberOfFriends--;
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            });
            newFriendThread.start();
        }
    }
    
    /**
     * Ask chatroom member for their name for later identification
     * @param friendSocket socket where member can be reached
     * @return name of the user of the socket
     * @throws IOException if IO error occurs with friendSocket
     */
    private String getFriendName(Socket friendSocket) throws IOException {
        BufferedReader clientWrite = new BufferedReader(new InputStreamReader(friendSocket.getInputStream()));
        PrintWriter clientRead = new PrintWriter(friendSocket.getOutputStream(), true);
        
        String peopleOrperson = " people";
        String isOrAre = " are ";
        if (numberOfFriends == THE_LONELIEST_NUMBER) {
            peopleOrperson = " person";
            isOrAre = " is ";
        }
        
        String welcomeMessage = "Welcome to the chatroom! There" + isOrAre + "currently " + numberOfFriends +
                                peopleOrperson + " including you in the room! Please enter your name: ";
        clientRead.println(welcomeMessage);
        
        String friendName = clientWrite.readLine();
        while (friendName.equals("")) {
            String repeatMessage = "Sorry, didn't catch that. Please enter your name: ";
            clientRead.println(repeatMessage);
            friendName = clientWrite.readLine();
        }
        
        String instructionMessage = "Welcome " + friendName + "! Start typing below to send messages to your friends.";
        clientRead.println(instructionMessage);
        
        return friendName;
    }
    
    /**
     * Handle a single client's input, sending messages to all other users
     * An input of "GOODBYE" will remove the user from the chatroom
     * @param sender socket where user can be reached
     * @throws IOException if IO error occurs with sender
     */
    private void handleInput(Socket sender) throws IOException {
        BufferedReader clientWrite = new BufferedReader(new InputStreamReader(sender.getInputStream()));
        
        try {
            String newMessage = clientWrite.readLine();
            while (newMessage != null) {
                if (newMessage.equals("GOODBYE")) {
                    this.handleOutput(GOODBYE_MESSAGE, sender);
                    return;
                } else {
                    this.handleOutput(newMessage, sender);
                }
                newMessage = clientWrite.readLine();
            }
        } finally {
            clientWrite.close();
        }
    }
    
    /**
     * Sends message from sender to all other users
     * @param message String message to be sent to members of chatroom
     * @param sender socket where message originated from
     * @throws IOException if IO error occurs with any of the user sockets
     */
    private synchronized void handleOutput(String message, Socket sender) throws IOException {
        Set<Socket> recipients = this.removeSender(sender);
        String senderName = this.nameMap.get(sender);
        String sendableMessage = senderName + ": " + message;
        for (Socket recipient: recipients) {
            PrintWriter screen = new PrintWriter(recipient.getOutputStream(), true);
            screen.println(sendableMessage);
        }
    }
    
    /**
     * Create a set of recipients for a message
     * @param sender socket of the sender of a message
     * @return a set of all user sockets in the chatroom not including sender
     */
    private Set<Socket> removeSender(Socket sender) {
        Set<Socket> everyone = new HashSet<>(this.nameMap.keySet());
        everyone.remove(sender);
        return everyone;
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("Chatroom open!");
            ChatroomServer test = new ChatroomServer();
            test.serve();
        } catch (IOException ioe) {
            System.err.println(ioe.getStackTrace());
        }
    }
}
