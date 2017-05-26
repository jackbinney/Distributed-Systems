import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

// Provides an interface used by the Java RMI
public interface ServerInterface extends Remote {
    
    void placeOrder(String username, String order) throws RemoteException;
    
    ArrayList<String[]> getOrders(String username) throws RemoteException;
    
    Map<String, ArrayList<String[]>> getAllOrders() throws RemoteException;
    
    String cancelOrder(String username, int order) throws RemoteException;
    
    void setPrimary() throws RemoteException;
    
    Boolean getPrimary() throws RemoteException;
}
