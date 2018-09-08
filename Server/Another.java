import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Another extends Remote {
    String another() throws RemoteException;
}
