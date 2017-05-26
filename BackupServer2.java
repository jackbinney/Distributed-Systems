import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
	
public class BackupServer2 implements ServerInterface {

	Boolean primary;
	int port;
	Map<String, ArrayList<String[]>> orders = new HashMap<String, ArrayList<String[]>>();
	String[][] otherServers = {{"localhost", "main"}, {"localhost", "backup1"}};
	
    public BackupServer2(int port) {
    	primary = false;
    	this.port = port;
    	
    	try {
    	    // Create remote object stub from server object
    	    ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(this, 0);

    	    // Get registry
    	    Registry registry = LocateRegistry.getRegistry("localhost", port);
    	    registry.rebind("backup2", stub);

    	    // Write ready message to console
    	    System.err.println("Server ready");
    	} catch (Exception e) {
    	    System.err.println("Server exception: " + e.toString());
    	    e.printStackTrace();
    	}
    	
    	// try and get the orders from other servers (used when the server goes offline then comes back online again)
    	try {
    		for (String[] server : otherServers) {
    			try {
    				Registry registry = LocateRegistry.getRegistry(server[0], port);
    				ServerInterface stub = (ServerInterface) registry.lookup(server[1]);
    				// Get items from the primary server if it's still up, else get the items from another backup server
    				if (stub.getPrimary() == true) {
    					orders = stub.getAllOrders();
    					break;
    				}
    				else {
    					orders = stub.getAllOrders();
    				}
    			}
    			catch(Exception e) {
    				
    			}
    		}
    	}
    	catch (Exception e) {}
    }
    
    public void placeOrder(String username, String order) {
//    	If the user has not already made an order before, create a new hash within the hashmap
    	if (orders.get(username) == null) {
    		orders.put(username, new ArrayList<String[]>());
    	}
    	String[] orderSplit = order.split(",");
//    	Split the users order by each comma
    	for (String item : orderSplit) {
//    		Remove any whitespace from the start and finish of each order item
    		item.trim();
    	}
//    	If the amount of items is less than 3 add it immediately, else add the first three items
    	if (orderSplit.length < 3) {
    		orders.get(username).add(orderSplit);
    	}
    	else {
    		String[] temp = {orderSplit[0], orderSplit[1], orderSplit[2]};
        	orders.get(username).add(temp);
    	}
    	
//    	If the server is the primary server, update all other servers with the new information
    	if (primary == true) {
    		for (String[] server : otherServers) {
    			try {
    				Registry registry = LocateRegistry.getRegistry(server[0], port);
    				ServerInterface stub = (ServerInterface) registry.lookup(server[1]);
    				stub.placeOrder(username, order);
    			}
    			catch(Exception e) {
    				
    			}
    		 }
    	}
    }
    
//  Returns the orders for a given username
    public ArrayList<String[]> getOrders(String username) {
    	return orders.get(username);
    }
    
//  Returns all orders, regardless of username
    public Map<String, ArrayList<String[]>> getAllOrders() {
    	return orders;
    }
    
//  Cancel an order given a username and the order number
    public String cancelOrder(String username, int orderNumber) {
    	// -1 is needed because orders start from 1 and arrays start at 0
    	try {
    		if (orders.get(username) != null) {
//    			Remove the order from the orders
    			orders.get(username).remove(orderNumber - 1);
//    			If this server is the primary server, update all other servers with this information
    			if (primary == true) {
    	    		for (String[] server : otherServers) {
    	    			try {
    	    				Registry registry = LocateRegistry.getRegistry(server[0], port);
    	    				ServerInterface stub = (ServerInterface) registry.lookup(server[1]);
    	    				stub.cancelOrder(username, orderNumber);
    	    			}
    	    			catch(Exception e) {
    	    				
    	    			}
    	    		 }
    	    	}
    			return "Order successfully cancelled";
    		}
    		else {
    			return "You have no orders to cancel";
    		}
    	}
//    	If the user does not enter a valid order number
    	catch (Exception e) {
    		return "Invalid order number";
    	}
    }
	
//  Makes this server the primary server
    public void setPrimary() {
    	System.out.println("THIS SERVER IS NOW THE PRIMARY SERVER");
    	primary = true;
    }
    
//  Returns whether this server is the primary server
    public Boolean getPrimary() {
    	return primary;
    }
    
//  Starts the server
    public static void main(String args[]) {
    	new BackupServer2(12000);
    }
}
