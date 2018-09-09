import javafx.util.Pair;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class GamePlayer implements GamePlayerInterface {
    protected String id;
    protected int N, K;
    protected int coins;
    protected Pair<Integer, Integer> location;
    protected boolean isServer;
    protected boolean isBackup;
    protected List<String> playerList;
    protected GamePlayerInterface serverPlayer;
    protected GamePlayerInterface backupPlayer;
    protected GameState gameState;

    public GamePlayer(String id) {
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getN() {
        return N;
    }

    public void setN(int n) {
        N = n;
    }

    public int getK() {
        return K;
    }

    public void setK(int k) {
        K = k;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public Pair<Integer, Integer> getLocation() {
        return location;
    }

    public void setLocation(Pair<Integer, Integer> location) {
        this.location = location;
    }

    public boolean isServer() {
        return isServer;
    }

    public void setServer(boolean server) {
        isServer = server;
    }

    public boolean isBackup() {
        return isBackup;
    }

    public void setBackup(boolean backup) {
        isBackup = backup;
    }

    public List<String> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(List<String> playerList) {
        this.playerList = playerList;
    }

    public GamePlayerInterface getServerPlayer() {
        return serverPlayer;
    }

    public void setServerPlayer(GamePlayerInterface serverPlayer) {
        this.serverPlayer = serverPlayer;
    }

    public GamePlayerInterface getBackupPlayer() {
        return backupPlayer;
    }

    public void setBackupPlayer(GamePlayerInterface backupPlayer) {
        this.backupPlayer = backupPlayer;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void joinGame(TrackerInterface tracker) throws RemoteException {
        NewJoinerPack newJoinerPack = tracker.addPlayer(this);
        List<String> players = newJoinerPack.getPlayers();
        this.playerList = players;
        this.N = newJoinerPack.getN();
        this.K = newJoinerPack.getK();
        if (players.size() == 1) {
            this.setServer(true);
            this.serverPlayer = this;
            this.initiateTreasury();
            this.generateLocation();
        } else if (players.size() == 2) {
            this.setBackup(true);
        }
        this.serverPlayer = tracker.getPlayer(players.get(0));
        this.backupPlayer = tracker.getPlayer(players.get(1));
    }

    private void generateTreasure() {

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

    public void initiateTreasury() {
        List<Pair<Integer, Integer>> availableLocations = new ArrayList<>();
        List<Pair<Integer, Integer>> coinsLocation = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                availableLocations.add(new Pair<>(i, j));
            }
        }
        for (int i = 0; i < K; i++) {
            int random = ThreadLocalRandom.current().nextInt(0, availableLocations.size() + 1);
            coinsLocation.add(availableLocations.get(random));
            availableLocations.remove(random);
        }
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
