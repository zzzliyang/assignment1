import javafx.util.Pair;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public GameState getGameState() {
        return gameState;
    }

    @Override
    public GameState newPlayerJoined(String newJoinerId) {
        this.playerList.add(newJoinerId);
        if (this.playerList.size() == 2) {
            this.gameState.setBackupPlayer(newJoinerId);
        }
        List<Pair<Integer, Integer>> availableLocation = this.gameState.getAvailableLocation();
        int random = ThreadLocalRandom.current().nextInt(0, availableLocation.size() + 1);
        Map<String, Pair<Integer, Integer>> playersLocation = this.gameState.getPlayersLocation();
        playersLocation.put(newJoinerId, availableLocation.get(random));
        availableLocation.remove(random);
        Map<String, Integer> playersScore = this.gameState.getPlayersScore();
        playersScore.put(newJoinerId, 0);
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void joinGame(TrackerInterface tracker, GamePlayerInterface stub) throws RemoteException {
        System.out.println("Joining game...");
        NewJoinerPack newJoinerPack = tracker.addPlayer(id, stub);
        List<String> players = newJoinerPack.getPlayers();
        this.playerList = players;
        this.N = newJoinerPack.getN();
        this.K = newJoinerPack.getK();
        if (players.size() == 1) {
            this.setServer(true);
            this.serverPlayer = this;
            System.out.println("This is server, initiating game state...");
            initiateGameState();
        } else {
            System.out.println("This is player, getting game state from server...");
            this.serverPlayer = tracker.getPlayer(players.get(0));
            this.gameState = serverPlayer.newPlayerJoined(id);
        }
        printGameState();
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

    public void initiateGameState() {
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
        int random = ThreadLocalRandom.current().nextInt(0, availableLocations.size() + 1);
        Map<String, Pair<Integer, Integer>> playersLocation = new HashMap<>();
        playersLocation.put(id, availableLocations.get(random));
        availableLocations.remove(random);
        Map<String, Integer> playersScore = new HashMap<>();
        playersScore.put(id, 0);
        this.gameState = new GameState(
                id,
                null,
                coinsLocation,
                playersLocation,
                playersScore,
                availableLocations
        );
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

    private void printGameState() {
        Map<Pair<Integer,Integer>, String> locationMap = new HashMap<>();
        List<Pair<Integer,Integer>> coinLocList = gameState.getCoinsLocation();
        Map<String, Pair<Integer,Integer>> playerLocMap = gameState.getPlayersLocation();
        Map<String,Integer> scores = gameState.getPlayersScore();
        for(int i=0;i<N;i++){
            for(int j=0;j<N;j++){
                Pair<Integer,Integer> key = new Pair<>(i,j);
                locationMap.put(key, "  ");
            }
        }
        coinLocList.forEach(k->locationMap.put(k,"* ")); //coins with *
        playerLocMap.entrySet().forEach(entry-> locationMap.put(entry.getValue(),entry.getKey())); //player with 2-char id

        System.out.println("Game server is :" + gameState.getServerPlayer());
        System.out.println("Backup server is :" + gameState.getBackupPlayer());
        System.out.println("Current player: "+ id + ", current score: " + coins);
        for(int i=0;i<2*N+4;i++){ System.out.print("=");}
        System.out.println();
        for(int i=0;i<N;i++){
            System.out.print("||");
            for(int j=0;j<N;j++){
                Pair<Integer,Integer> key = new Pair<>(i,j);
                System.out.print(locationMap.get(key));
            }
            System.out.print("||");
            System.out.println();
        }
        for(int i=0;i<2*N+4;i++){ System.out.print("=");}
        System.out.println();

        System.out.println("Player scores:");
        scores.entrySet().forEach(s->System.out.println("Player id: "+ s.getKey() + ", score: " + s.getValue()));
        System.out.println("End of player scores!");
    }
}
