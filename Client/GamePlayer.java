import javafx.util.Pair;
import org.apache.commons.lang3.SerializationUtils;

import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class GamePlayer implements GamePlayerInterface {
    protected String id;
    protected int N, K;
    // TODO: can be removed
    protected int coins;
    // TODO: can be removed
    protected Pair<Integer, Integer> location;
    protected boolean isServer;
    protected boolean isBackup;
    protected GamePlayerInterface serverPlayer;
    protected GamePlayerInterface backupPlayer;
    protected TrackerInterface tracker;
    protected GameState gameState;
    private Set<String> livePlayers = new HashSet<>();

    public GamePlayer(String id, TrackerInterface tracker) {
        this.id = id;
        this.tracker = tracker;
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
        return SerializationUtils.clone(gameState);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public synchronized GameState newPlayerJoined(String newJoinerId) throws RemoteException {
        List<String> playerList = this.gameState.getPlayerList();
        playerList.add(newJoinerId);
        this.livePlayers.add(newJoinerId);
        if (playerList.size() == 2) {
            this.gameState.setBackupPlayer(newJoinerId);
            this.setBackupPlayer(tracker.getPlayer(newJoinerId));
        }
        List<Pair<Integer, Integer>> availableLocation = this.gameState.getAvailableLocation();
        int random = ThreadLocalRandom.current().nextInt(0, availableLocation.size());
        Map<String, Pair<Integer, Integer>> playersLocation = this.gameState.getPlayersLocation();
        playersLocation.put(newJoinerId, availableLocation.get(random));
        availableLocation.remove(random);
        Map<String, Integer> playersScore = this.gameState.getPlayersScore();
        playersScore.put(newJoinerId, 0);
        notifyBackup();
        return SerializationUtils.clone(gameState);
    }

    @Override
    public void isAlive(String id) throws RemoteException {
        //System.out.println(id + " is alive...");
        this.livePlayers.add(id);
    }

    @Override
    public void isAlive() throws RemoteException {
    }

    @Override
    public void joinGame(GamePlayerInterface stub) throws RemoteException {
        System.out.println("Joining game...");
        NewJoinerPack newJoinerPack = tracker.addPlayer(id, stub);
        List<String> players = newJoinerPack.getPlayers();
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
            try {
                this.gameState = serverPlayer.newPlayerJoined(id);
            } catch (ConnectException e) {
                System.out.println("Failed to contact server... Set back up server as server...");
                this.serverPlayer = tracker.getPlayer(players.get(1));
                this.gameState = serverPlayer.newPlayerJoined(id);
            }
            String backupPlayer = gameState.getBackupPlayer();
            if(backupPlayer.equals(id)){
                this.setBackup(true);
                this.backupPlayer = this;
                System.out.println("This is backup server...");
            } else {
                this.backupPlayer = tracker.getPlayer(backupPlayer);
            }
        }
        printGameState();
    }

    private void generateTreasure() {
        List<Pair<Integer, Integer>> availableLocation = this.gameState.getAvailableLocation();
        int random = ThreadLocalRandom.current().nextInt(0, availableLocation.size());
        Pair<Integer, Integer> newLocation = availableLocation.get(random);
        this.gameState.getCoinsLocation().add(newLocation);
        this.gameState.getAvailableLocation().remove(newLocation);
        System.out.println(
                "New coin generated at " + newLocation.getKey() + ", " + newLocation.getValue());
    }

    @Override
    public synchronized GameState movePlayer(String id, int direction) throws RemoteException {
        Map<String, Pair<Integer, Integer>> playersLocation = gameState.getPlayersLocation();
        Pair<Integer, Integer> currentLocation = playersLocation.get(id);
        Map<String, Integer> playersScore = gameState.getPlayersScore();
        List<Pair<Integer, Integer>> availableLocation = gameState.getAvailableLocation();
        List<Pair<Integer, Integer>> coinsLocation = gameState.getCoinsLocation();
        String serverPlayer = gameState.getServerPlayer();
        String backupPlayer = gameState.getBackupPlayer();
        Pair<Integer, Integer> newLocation = null;
        switch (direction) {
            case 0:
                return SerializationUtils.clone(gameState);
            case 1:
                newLocation = new Pair<>(currentLocation.getKey(), currentLocation.getValue() - 1);
                break;
            case 2:
                newLocation = new Pair<>(currentLocation.getKey() + 1, currentLocation.getValue());
                break;
            case 3:
                newLocation = new Pair<>(currentLocation.getKey(), currentLocation.getValue() + 1);
                break;
            case 4:
                newLocation = new Pair<>(currentLocation.getKey() - 1, currentLocation.getValue());
                break;
            case 9: // remove player
                List<String> playerList = gameState.getPlayerList();
                availableLocation.add(currentLocation);
                playersLocation.remove(id);
                playersScore.remove(id);
                playerList.remove(id);
                if (serverPlayer.equals(id)) {
                    gameState.setServerPlayer(backupPlayer);
                    gameState.setBackupPlayer(playerList.size() > 1 ? playerList.get(1) : null);
                } else if (backupPlayer.equals(id)) {
                    gameState.setBackupPlayer(playerList.size() > 1 ? playerList.get(1) : null);
                }
                notifyBackup();
                return SerializationUtils.clone(gameState);
        }
        int coinIndex = coinsLocation.indexOf(newLocation);
        if (coinIndex >= 0) {
            System.out.println("Moving to a coin location...");
            playersScore.put(id, playersScore.get(id) + 1);
            coinsLocation.remove(coinIndex);
            generateTreasure();
        } else {
            int index = availableLocation.indexOf(newLocation);
            if (index < 0) {
                System.out.println(
                        "Move is not valid. Returning current game state without doing move...");
                return SerializationUtils.clone(gameState);
            }
            availableLocation.remove(index);
        }
        availableLocation.add(currentLocation);
        playersLocation.put(id, newLocation);
        notifyBackup();
        return SerializationUtils.clone(gameState);
    }

    public synchronized void move(int direction) throws RemoteException {
        System.out.println("User: " + id + " action is: " + direction + " server is: " + gameState.getServerPlayer());
        if (isServer) {
            movePlayer(id, direction);
            printGameState();
        } else {
            GameState refresh;
            try {
                refresh = serverPlayer.movePlayer(id, direction);
            } catch (ConnectException | ConnectIOException e) {
                System.out.println("Server is down... Submit move request to backup server: " + gameState.getBackupPlayer());
                if(gameState.getPlayerList().size()==1)
                    refresh = this.movePlayer(id, direction);
                else
                    refresh = backupPlayer.movePlayer(id, direction);
            }
            this.setGameState(refresh);
            printGameState();
        }
    }

    @Override
    public void pingServer() throws RemoteException {
        if (isServer) return;
        String deadServer = gameState.getServerPlayer();
        System.out.println("Try pinging " + deadServer);
        System.out.println("Pinged at time " + System.currentTimeMillis());
        boolean wasBackup = isBackup;
        try {
            serverPlayer.isAlive(id);
            //System.out.println("Server is alive...");
        } catch (RemoteException e) {
            System.out.println("Server is down...");
            if (wasBackup) {
                this.onPlayerExit(deadServer);
                List<String> playerList = gameState.getPlayerList();
                System.out.println("Server is dead, update tracker with player list:");
                playerList.forEach(System.out::println);
                tracker.updateList(playerList);
                gameState.setServerPlayer(id);
                String backupPlayer = playerList.size() > 1 ? playerList.get(1) : null;
                gameState.setBackupPlayer(backupPlayer);
                this.serverPlayer = this;
                this.livePlayers = new HashSet<>(playerList);
                this.setServer(true);
                this.setBackup(false);
                if (backupPlayer != null) {
                    this.backupPlayer = tracker.getPlayer(backupPlayer);
                    notifyBackup();
                } else {
                    this.backupPlayer = null;
                }
            } else {
                while (true) {
                    List<String> playerList = tracker.getPlayerList();
                    String firstPlayer = playerList.get(0);
                    if (!firstPlayer.equals(deadServer)) {
                        playerList.forEach(System.out::print);
                        this.serverPlayer = tracker.getPlayer(firstPlayer);
                        System.out.println("The new server is : " + firstPlayer);
                        String backupPlayer = playerList.size() > 1 ? playerList.get(1) : null;
                        System.out.println("The new backup is : " + backupPlayer);
                        if (backupPlayer != null) {
                            if (id.equals(backupPlayer)) {
                                this.backupPlayer = this;
                                this.setBackup(true);
                            } else {
                                this.backupPlayer = tracker.getPlayer(backupPlayer);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void pingBackup() throws RemoteException {
        if (isBackup) return;
        String deadBackup = gameState.getBackupPlayer();
        try {
            if(backupPlayer!=null) {
                backupPlayer.isAlive();
            }
            //System.out.println("Server is alive...");
        } catch (RemoteException e) {
            System.out.println("Backup server is down...");
            if (isServer) {
                this.onPlayerExit(deadBackup);
                List<String> playerList = gameState.getPlayerList();
                System.out.println("Backup is dead, update tracker with player list:");
                playerList.forEach(System.out::println);
                tracker.updateList(playerList);
                String backupPlayer = playerList.size() > 1 ? playerList.get(1) : null;
                gameState.setBackupPlayer(backupPlayer);
                if (backupPlayer != null) {
                    this.backupPlayer = tracker.getPlayer(backupPlayer);
                    notifyBackup();
                } else {
                    this.backupPlayer = null;
                }
            } else {
                while (true) {
                    List<String> playerList = tracker.getPlayerList();
                    String secondPlayer = playerList.get(1);
                    if (!secondPlayer.equals(deadBackup)) {
                        if (id.equals(secondPlayer)) {
                            this.backupPlayer = this;
                            this.setBackup(true);
                        } else {
                            this.backupPlayer = tracker.getPlayer(secondPlayer);
                        }
                        break;
                    }
                }
            }
        }
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
            int random = ThreadLocalRandom.current().nextInt(0, availableLocations.size());
            coinsLocation.add(availableLocations.get(random));
            availableLocations.remove(random);
        }
        int random = ThreadLocalRandom.current().nextInt(0, availableLocations.size());
        Map<String, Pair<Integer, Integer>> playersLocation = new HashMap<>();
        playersLocation.put(id, availableLocations.get(random));
        availableLocations.remove(random);
        Map<String, Integer> playersScore = new HashMap<>();
        playersScore.put(id, 0);
        List<String> playerList = new ArrayList<>();
        playerList.add(id);
        this.gameState =
                new GameState(
                        id, null, coinsLocation, playersLocation, playersScore, availableLocations, playerList);
    }

    @Override
    public void notifyBackup() throws RemoteException {
        if (backupPlayer != null) {
            backupPlayer.updateGameStateFromServer(gameState);
        }
    }

    @Override
    public boolean updateGameStateFromServer(GameState gameState) {
        try {
            this.gameState = gameState;
            if (gameState.getBackupPlayer().equals(id)) {
                this.backupPlayer = this;
                this.setBackup(true);
                this.setServer(false);
            }
        } catch (Exception e) {
            System.out.println("Not backup server...");
        }
        return false;
    }

    private void printGameState() {
        GameState currentState = SerializationUtils.clone(gameState);
        Map<Pair<Integer, Integer>, String> locationMap = new HashMap<>();
        List<Pair<Integer, Integer>> coinLocList = currentState.getCoinsLocation();
        Map<String, Pair<Integer, Integer>> playerLocMap = currentState.getPlayersLocation();
        Map<String, Integer> scores = currentState.getPlayersScore();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                Pair<Integer, Integer> key = new Pair<>(i, j);
                locationMap.put(key, "  ");
            }
        }
        coinLocList.forEach(k -> locationMap.put(k, "* ")); // coins with *
        playerLocMap
                .entrySet()
                .forEach(
                        entry -> locationMap.put(entry.getValue(), entry.getKey())); // player with 2-char id

        System.out.println("Game server is :" + currentState.getServerPlayer());
        System.out.println("Backup server is :" + currentState.getBackupPlayer());
        System.out.println("Current player: " + id + ", current score: " + coins);
        for (int i = 0; i < 3 * N + 3; i++) {
            System.out.print("=");
        }
        System.out.println();
        for (int i = 0; i < N; i++) {
            System.out.print("||");
            for (int j = 0; j < N; j++) {
                Pair<Integer, Integer> key = new Pair<>(i, j);
                System.out.print(locationMap.get(key));
                System.out.print("|");
            }
            System.out.print("|");
            System.out.println();
        }
        for (int i = 0; i < 3 * N + 3; i++) {
            System.out.print("=");
        }
        System.out.println();

        System.out.println("Player scores:");
        scores
                .entrySet()
                .forEach(s -> System.out.println("Player id: " + s.getKey() + ", score: " + s.getValue()));
        System.out.println("End of player scores!");
    }

    private void onPlayerExit(String leftPlayer) throws RemoteException {
        GameState currentState = this.getGameState();
        List<String> playerList = currentState.getPlayerList();
        playerList.remove(leftPlayer);
        System.out.println("Player " + leftPlayer + " has left, remaining players: ");
        playerList.forEach(System.out::println);
        tracker.updateList(playerList);
        List<Pair<Integer, Integer>> availableLocation = currentState.getAvailableLocation();
        Map<String, Pair<Integer, Integer>> playersLocation = currentState.getPlayersLocation();
        Map<String, Integer> playersScore = currentState.getPlayersScore();
        Pair<Integer, Integer> currentLocation = playersLocation.get(leftPlayer);
        availableLocation.add(currentLocation);
        playersLocation.remove(leftPlayer);
        playersScore.remove(leftPlayer);
        this.setGameState(currentState);
    }

    public void receivePing() throws RemoteException, InterruptedException {
        if (!isServer) return;
        List<String> playerList = gameState.getPlayerList();
        List<String> toCheck = playerList.subList(1, playerList.size());
        System.out.println("player list: ");
        for (String player: toCheck) System.out.println(player);
        System.out.println("live player set: ");
        for (String player: livePlayers) System.out.println(player);
        if (livePlayers.size() != toCheck.size()) {
            for (String player: toCheck) {
                if (!livePlayers.contains(player)) {
                    System.out.println("Checked at time " + System.currentTimeMillis());
                    System.out.println("Player : " + player + " disconnected...");
                    this.onPlayerExit(player);
                    notifyBackup();
                }
            }
        }
        livePlayers.clear();
    }
}
