import java.io.*;
import java.net.*;

public class NewClient {
	
	// Used for user input
	static BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	static Socket clientSocket = null;
	
	public NewClient() throws Exception {
		// Monitors if the user would like to carry on processing
		Boolean cont = true;
		// Attempts to connect to the server
		connectToServer();
			
		while(cont == true) {
			try {				
				DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
				BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				
				// Count is the amount of lines the server has returned
				int count = Integer.parseInt(inFromServer.readLine());
				int i = 0;
				while (i < count) {
					// Print out all lines sent by the user
					System.out.println(inFromServer.readLine());
					i = i + 1;
				}
				
				// Read the input from the user
				String sentence = inFromUser.readLine();
				// If the user wishes to stop execution
				if (sentence.equals("Close") || sentence.equals("close")) {
					System.out.println("CLOSING");
					clientSocket.close();
					cont = false;
					System.exit(0);
				}
				else {
					// Send the users request to the Front End
					outToServer.writeBytes(sentence + "\n");
				}
			}
			catch (SocketException e) {
				// If the user loses connection with the Front End
				clientSocket = null;
				System.out.println("Server no longer operational");
				// Attempt to reconnect
				connectToServer();
			}
		}
	}
	
	// Allows the user to connect to the front end
	private void connectToServer() {
		try {
			System.out.println("Attempting connection with the server");
			// Port number can change as required
			clientSocket = new Socket("localhost", 11000);
		}
		catch (Exception e) {
			// If the Front End cannot be reached
			System.out.println("There are no servers available at the moment, please try again later.");
			System.exit(0);
		}
	}
	
	public static void main(String args[]) throws Exception {
		new NewClient();
	}
}
