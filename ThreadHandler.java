import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

public class ThreadHandler extends Thread{
	
	// Array of all current server names
	String[] servers = {"main", "backup1", "backup2"};
	// The current server
	String selectedServer;
	// Front End socket
	Socket connectionSocket;
	// Normally 1,2 or 3
	String clientCommand;
	// Whatever the user wishes to do
	String clientSentence;
	// Username provided by the user
	String username;
	
	Registry registry;
	ServerInterface stub;
	BufferedReader inFromClient = null;
	DataOutputStream outToClient = null;
	String response;
	// This message is supplied after every command is succesfully executed, or the user has just connected
	String defaultMessage = "Please select one of the options below:\n 1) Place an order\n 2) Retrieve order history\n 3) Cancel an order\n";
	// Tracks if the user has just connected
	Boolean firstStart;
	
	int port;
	
	public ThreadHandler(Socket x, String selectedServer, int port) {
		connectionSocket = x;
		this.port = port;
		this.selectedServer = selectedServer;
		firstStart = true;
	}
	
//	Take the command and the sentence from the user and carry out whatever method they have requested
	public void newHandler(String clientCommand, String clientSentence) throws Exception{
//		If the user has requested to add an order
		if (clientCommand.equals("1")) {
			stub.placeOrder(username, clientSentence);
			outToClient.writeBytes("5\nOrder successfully placed\n" + defaultMessage); 
		}
//		If the user has requested to view their current orders
		else if(clientCommand.equals("2")) {
			response = "";
			ArrayList<String[]> orders = stub.getOrders(username);
			if (orders == null) {
				outToClient.writeBytes("5\nYou have no orders \n" + defaultMessage);
			}
			else {
				int count = 0;
				for(int i = 0; i < orders.size(); i++) {
					count = count + 1;
					response = response + "Order " + (i + 1) + ":" + "\n";
					for(int j = 0; j < orders.get(i).length; j++) {
						response = response + "Item " + (j + 1) + ": " + orders.get(i)[j] + "\n";
						count = count + 1;
					}
					count = count + 1;
					response = response + "\n";
				}
				if (orders.size() == 0) {
					outToClient.writeBytes("5\nYou have no orders \n" + defaultMessage);
				}
				else {
					outToClient.writeBytes("" + (count + 4) + "\n" + response + defaultMessage);
				}
			}
		}
//		If the user has requested to cancel an order
		else if(clientCommand.equals("3")) {
			response = stub.cancelOrder(username, Integer.parseInt(clientSentence));
			outToClient.writeBytes("5\n" + response + "\n" + defaultMessage);
		}
//		If the user has typed in an unknow command (not 1, 2 or 3)
		else {
			outToClient.writeBytes("5\nThis is an invalid command, please try again\n" + defaultMessage);
		}
	}
	
	public void run() {
		//This gets ran once per client
		try {
//			If this is the first time, ask for a username 
			if (firstStart == true) {
				outToClient = new DataOutputStream(connectionSocket.getOutputStream());
				String response = "";
				registry = LocateRegistry.getRegistry("localhost", port);
				stub = (ServerInterface) registry.lookup(selectedServer);
				inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
				outToClient.writeBytes("1\nUsername:\n");
				username = inFromClient.readLine();
				outToClient.writeBytes("4\n" + defaultMessage);
			}
		
			//This gets ran until client/server closes connection
			while(true) {
//				Wait for the client to enter a command, then pass it to the handler
				clientCommand = inFromClient.readLine();
				if (clientCommand.equals("1")) {
					outToClient.writeBytes("1\nPlease enter your order details, a maximum of 3 items are allowed, please seperate each item by a comma" + "\n");
					clientSentence = inFromClient.readLine();
				}
				else if(clientCommand.equals("3")) {
					outToClient.writeBytes("1\nPlease enter the order number that you wish to cancel\n");
					clientSentence = inFromClient.readLine();
				}
				newHandler(clientCommand, clientSentence);
			}
		}
		catch (java.rmi.ConnectException e) {
//			Below is a modification necessary for when the primary server fails
//			If an error occurs (such as the original server handling this thread not being operational any more)
			try {
//				A boolean variable that tracks whether a new server has been found
				Boolean change = false;
//				Loop through all of the servers to try and find a new primary server
				for (String server : servers) {
					try {
//						Attempt to connect to the server
						registry = LocateRegistry.getRegistry("localhost", port);
//						Change the stub to the new primary server
						stub = (ServerInterface) registry.lookup(server);
//						If no error has been thrown, the server is online, and can therefore become the new primary server
						stub.setPrimary();
//						Update the tracking variable
						change = true;
//						Update the primary index so new connections know what server to connect to
						break;
					}
					catch (Exception ex) {}
				}
//				If a new server hasn't been found, there must be no servers online
				if (change == false) {
					outToClient.writeBytes("1\nThere are no servers available\n");
				}
//				If a server is found
				else {
//					Set this boolean variable to be false, ensuring that the user is not asked for a username
					firstStart = false;
//					Send the message that was meant to be sent to the original primary server to the new primary server
					newHandler(clientCommand, clientSentence);
//					Restart the thread
					run();
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		catch (SocketException e) {
//			A catch method to check when a user disconnects
			System.out.println("User disconnected");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
}
