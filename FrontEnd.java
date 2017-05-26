import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.*;

public class FrontEnd {

	// Get registry
    Registry registry = null;
    ServerInterface stub;
    String[] servers = {"main", "backup1", "backup2"};
//    The int value below is the default primary server from the servers list above
    int primary = 0;
    int port = 12000;
    
    private FrontEnd() throws Exception {
//    	Open a welcomeSocket on 11000
    	ServerSocket welcomeSocket = new ServerSocket(11000);
//    	Create the registry on the port above
    	LocateRegistry.createRegistry(port);
    	while(true) {
			try {
				Socket connectionSocket = welcomeSocket.accept();
//				Attempt to connect to the primary server
				try {
					registry = LocateRegistry.getRegistry("localhost", port);
					stub = (ServerInterface) registry.lookup(servers[primary]);
					registry.lookup(servers[primary]);
					stub.setPrimary();
					new Thread(new ThreadHandler(connectionSocket, servers[primary], port)).start();
				}
				catch(Exception e) {
//					Below is a modification necessary for when the primary server fails
//					The front end firstly loops through all of the servers
					for (int i = 0; i < servers.length; i++) {
						try {
//							The front end attempts to connect to one of other servers via its RMI binding
							Registry registry = LocateRegistry.getRegistry("localhost", port);
							stub = (ServerInterface) registry.lookup(servers[i]);
//							If no error has been thrown, the server is online, and can therefore become the new primary server
							stub.setPrimary();
//							Update the primary index so new connections know what server to connect to
							primary = i;
//							Create a new thread
							new Thread(new ThreadHandler(connectionSocket, servers[i], port)).start();
//							Break to ensure that no other servers in the array are connected to, since a new primary server has been designated
							break;
						}
						catch (Exception ex) {}
					}
//					If the stub is still null, there are no servers available
					if (stub == null) {
						DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
						outToClient.writeBytes("1\nThere are no servers available at the moment, please try again later.\n");
					}
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
    }

    public static void main(String[] args) throws Exception {
    	new FrontEnd();
    }
}

