import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Rentong on 8/9/2018.
 */
public interface GamePlayerInterface extends Remote {
    boolean isAlive(String id) throws RemoteException;
    boolean isAlive() throws RemoteException;

    void joinGame(GamePlayerInterface stub) throws RemoteException;

    GameState movePlayer(String id, int direction) throws RemoteException;
    void pingServer() throws RemoteException;
    void pingBackup() throws RemoteException;
    void notifyBackup() throws RemoteException;

    GameState getGameState() throws RemoteException;
    GameState newPlayerJoined(String id) throws RemoteException;

    boolean updateGameStateFromServer(GameState gameState) throws RemoteException;
}
