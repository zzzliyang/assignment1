import javafx.util.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Created by Rentong on 8/9/2018.
 */
public interface GamePlayerInterface extends Remote {
    String getId();

    void joinGame(TrackerInterface tracker) throws RemoteException;
    void refresh();
    void exit();
    void move(int direction);
    void pingServer();
    void pingBackup();
    GamePlayerInterface getServer();
    GamePlayerInterface getBackup();
    List<String> refreshListFromTracker(int delay);

    Pair<Integer, Integer> generateLocation();
    void notifyBackup();
    void updateTrackerWithPlayerList(List<String> currentPlayers);
//    void updatePlayerState()
    void promote();
}
