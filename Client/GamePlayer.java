import javafx.util.Pair;

import java.util.List;

public class GamePlayer implements GamePlayerInterface {
    protected String id;
    protected int N, K;
    protected int coins;
    protected Pair<Integer, Integer> location;
    protected boolean isServer;
    protected boolean isBackup;
    protected List<String> playerList;


    @Override
    public String getId() {
        return id;
    }

    @Override
    public void joinGame() {

    }

    @Override
    public void refresh() {

    }

    @Override
    public void exit() {

    }

    @Override
    public void move(int direction) {

    }

    @Override
    public void pingServer() {

    }

    @Override
    public void pingBackup() {

    }

    @Override
    public GamePlayerInterface getServer() {
        return null;
    }

    @Override
    public GamePlayerInterface getBackup() {
        return null;
    }

    @Override
    public List<String> refreshListFromTracker(int delay) {
        return null;
    }

    @Override
    public Pair<Integer, Integer> generateLocation() {
        return null;
    }

    @Override
    public void initiateTreasury(int n, int k) {

    }

    @Override
    public void notifyBackup() {

    }

    @Override
    public void updateTrackerWithPlayerList(List<String> currentPlayers) {

    }

    @Override
    public void promote() {

    }
}
