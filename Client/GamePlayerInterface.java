import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Rentong on 8/9/2018.
 */
public interface GamePlayerInterface extends Remote {
    default void isAlive() throws RemoteException {}
    void joinGame(GamePlayerInterface stub) throws RemoteException;
//    void refresh() throws RemoteException;
//    void exit() throws RemoteException;
    GameState movePlayer(String id, int direction) throws RemoteException;
    void pingServer() throws RemoteException;
    void pingBackup() throws RemoteException;
//    GamePlayerInterface getServer() throws RemoteException;
//    GamePlayerInterface getBackup() throws RemoteException;
    List<String> refreshListFromTracker(int delay) throws RemoteException;
    Pair<Integer, Integer> generateLocation() throws RemoteException;
    void notifyBackup() throws RemoteException;
    void updateTrackerWithPlayerList(List<String> currentPlayers) throws RemoteException;
    void promote() throws RemoteException;
    GameState getGameState() throws RemoteException;
    GameState newPlayerJoined(String id) throws RemoteException;

    boolean updateGameStateFromServer(GameState gameState) throws RemoteException;
}
