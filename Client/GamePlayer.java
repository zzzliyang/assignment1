import javafx.concurrent.Task;
import javafx.util.Pair;
import org.apache.commons.lang3.SerializationUtils;

import javax.swing.*;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

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
    private JTextArea output;

    public GamePlayer(String id, TrackerInterface tracker, JTextArea textArea) {
        this.id = id;
        this.tracker = tracker;
        this.output = textArea;
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
        List<Pair<Integer, Integer>> availableLocation = this.gameState.getAvailableLocation();
        int random = ThreadLocalRandom.current().nextInt(0, availableLocation.size());
        Map<String, Pair<Integer, Integer>> playersLocation = this.gameState.getPlayersLocation();
        playersLocation.put(newJoinerId, availableLocation.get(random));
        System.out.println("New player " + newJoinerId + " location: " + availableLocation.get(random).getKey() + availableLocation.get(random).getValue());
        availableLocation.remove(random);
        Map<String, Integer> playersScore = this.gameState.getPlayersScore();
        playersScore.put(newJoinerId, 0);
        if (playerList.size() == 2) {
            this.gameState.setBackupPlayer(newJoinerId);
            this.setBackupPlayer(tracker.getPlayer(newJoinerId));
            System.out.println("New joiner " + newJoinerId + " is backup server.");
        } else {
            System.out.println("Notifying new joiner: " + newJoinerId + " to backup server " + this.gameState.getBackupPlayer());
            notifyBackup();
        }
        return SerializationUtils.clone(gameState);
    }

    @Override
    public boolean isAlive(String id) throws RemoteException {
        System.out.println(id + " is alive..." + new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())));
        this.livePlayers.add(id);
        return true;
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }

    @Override
    public void joinGame(GamePlayerInterface stub) throws RemoteException {
        System.out.println("Joining game...");
        NewJoinerPack newJoinerPack = tracker.addPlayer(id, stub);
        List<String> players = newJoinerPack.getPlayers();
        this.N = newJoinerPack.getN();
        this.K = newJoinerPack.getK();
        if (players.size() == 1 || players.get(0).equals(id)) {
            this.setServer(true);
            this.serverPlayer = this;
            System.out.println("This is server, initiating game state...");
            initiateGameState();
        } else {
            System.out.println("This is player, getting game state from server..." + players.get(0));
            this.serverPlayer = tracker.getPlayer(players.get(0));
            try {
                this.gameState = serverPlayer.newPlayerJoined(id);
            } catch (ConnectException e) {
                System.out.println("Failed to contact server..." + players.get(0) + " Set back up server as server..." + players.get(1));
                this.serverPlayer = tracker.getPlayer(players.get(1));
                this.gameState = serverPlayer.newPlayerJoined(id);
                System.out.println("Got location: " + this.gameState.getPlayersLocation().get(id));
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
        System.out.println("Server " + this.id + " is moving " + id + " direction: " + direction);
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
            } catch (ConnectException | ConnectIOException | UnmarshalException e) {
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
        if (isServer) {
          pingBackup();
          System.out.println("Instead try pinging backup");
          return;
        }
        String deadServer = gameState.getServerPlayer();
        System.out.println("Try pinging " + deadServer);
        System.out.println("Pinged at time " + new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())));
        boolean wasBackup = isBackup;
        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Boolean> future = executor.submit(() -> serverPlayer.isAlive(id));
            System.out.println("Result: "+ future.get(200, TimeUnit.MILLISECONDS));
            System.out.println("Server is alive..." + new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println("Server is down..." + new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())));
            e.printStackTrace(System.out);
            if (wasBackup) {
                this.onPlayerExit(deadServer);
                List<String> playerList = gameState.getPlayerList();
                System.out.println("Server is dead, update tracker with player list:");
                playerList.forEach(System.out::println);
                System.out.println(id + " is removing: " + deadServer);
                tracker.removePlayer(deadServer);
                gameState.setServerPlayer(id);
                String backupPlayer = playerList.size() > 1 ? playerList.get(1) : null;
                gameState.setBackupPlayer(backupPlayer);
                this.serverPlayer = this;
                this.livePlayers = new HashSet<>(playerList);
                this.setServer(true);
                this.setBackup(false);
                if (backupPlayer != null) {
                    System.out.println("Setting backup server to " + backupPlayer);
                    this.backupPlayer = tracker.getPlayer(backupPlayer);
                    notifyBackup();
                } else {
                    System.out.println("Setting backup server to null");
                    this.backupPlayer = null;
                }
            } else {
//                if (true) {
                    List<String> playerList = tracker.getPlayerList();
                    String firstPlayer = playerList.get(0);
                    if (!firstPlayer.equals(deadServer)) {
                        playerList.forEach(System.out::print);
                        this.gameState.setPlayerList(playerList);
                        this.gameState.setServerPlayer(firstPlayer);
                        this.serverPlayer = tracker.getPlayer(firstPlayer);
                        System.out.println("The new server is : " + firstPlayer);
                        String backupPlayer = playerList.size() > 1 ? playerList.get(1) : null;
                        System.out.println("The new backup is : " + backupPlayer);
                        if (backupPlayer != null) {
                            if (id.equals(backupPlayer)) {
                                this.backupPlayer = this;
                                this.setBackup(true);
                                this.gameState.setBackupPlayer(backupPlayer);
                            } else {
                                this.backupPlayer = tracker.getPlayer(backupPlayer);
                                this.gameState.setBackupPlayer(backupPlayer);
                            }
                        }
//                        break;
                    } else {
                        System.out.println(new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())) + " players list not updated");
                    }
//                }
            }
        }
    }

    @Override
    public void pingBackup() throws RemoteException {
        if (isBackup) {
          pingServer();
          System.out.println("Instead try pinging backup");
          return;
        }
        String deadBackup = gameState.getBackupPlayer();
        try {
            if(backupPlayer!=null) {
                System.out.println("Try pinging backup" + deadBackup);
                System.out.println("Pinged at time " + new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())));
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Boolean> future = executor.submit(() -> backupPlayer.isAlive());
                System.out.println("Result: "+ future.get(200, TimeUnit.MILLISECONDS));
            }
            //System.out.println("Server is alive...");
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println("Backup server is down..." + new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())));
            if (isServer) {
                this.onPlayerExit(deadBackup);
                List<String> playerList = gameState.getPlayerList();
                System.out.println("Backup is dead, " + id + " is update tracker to remove: " + deadBackup);
                System.out.println("Currently live: ");
                playerList.forEach(System.out::println);
                tracker.removePlayer(deadBackup);
                String backupPlayer = playerList.size() > 1 ? playerList.get(1) : null;
                gameState.setBackupPlayer(backupPlayer);
                if (backupPlayer != null) {
                    this.backupPlayer = tracker.getPlayer(backupPlayer);
                    notifyBackup();
                } else {
                    this.backupPlayer = null;
                }
            } else {
//                while (true) {
                    List<String> playerList = tracker.getPlayerList();
                    playerList.forEach(System.out::print);
                    System.out.println("checking whether tracker list has been updated");
                    String secondPlayer = playerList.get(1);
                    if (!secondPlayer.equals(deadBackup)) {
                        this.gameState.setPlayerList(playerList);
                        if (id.equals(secondPlayer)) {
                            this.gameState.setBackupPlayer(secondPlayer);
                            this.backupPlayer = this;
                            this.setBackup(true);
                        } else {
                            this.backupPlayer = tracker.getPlayer(secondPlayer);
                            this.gameState.setBackupPlayer(secondPlayer);
                        }
//                        break;
                    }
//                }
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
    public synchronized void notifyBackup() throws RemoteException {
        if (backupPlayer != null) {
            String backup = gameState.getBackupPlayer();
            System.out.println("Updating backup player " + backup);
            try{
                backupPlayer.updateGameStateFromServer(gameState);
            } catch (RemoteException e){
                System.out.println("Fail to notify pervious backup "+backup);
                this.onPlayerExit(backup);
                List<String> playerList = gameState.getPlayerList();
                System.out.println("Backup is dead, " + id + " is update tracker to remove: " + backup);
                System.out.println("Currently live: ");
                playerList.forEach(System.out::println);
                tracker.removePlayer(backup);
                String backupPlayer = playerList.size() > 1 ? playerList.get(1) : null;
                gameState.setBackupPlayer(backupPlayer);
                if (backupPlayer != null) {
                    this.backupPlayer = tracker.getPlayer(backupPlayer);
                    notifyBackup();
                } else {
                    this.backupPlayer = null;
                }
            }
        }
    }

    @Override
    public boolean updateGameStateFromServer(GameState gameState) {
        try {
            this.gameState = gameState;
            this.serverPlayer = tracker.getPlayer(gameState.getServerPlayer());
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
        printMessageln("Game server is :" + currentState.getServerPlayer());
        printMessageln("Backup server is :" + currentState.getBackupPlayer());
        printMessageln("Current player: " + id + ", current score: " + coins);
        for (int i = 0; i < 3 * N + 3; i++) {
            printMessage("=");
        }
        printMessageln("");
        for (int i = 0; i < N; i++) {
            printMessage("||");
            for (int j = 0; j < N; j++) {
                Pair<Integer, Integer> key = new Pair<>(i, j);
                printMessage(locationMap.get(key));
                printMessage("|");
            }
            printMessage("|");
            printMessageln("");
        }
        for (int i = 0; i < 3 * N + 3; i++) {
            printMessage("=");
        }
        printMessageln("");

        printMessageln("Player scores:");
        scores
                .entrySet()
                .forEach(s -> printMessageln("Player id: " + s.getKey() + ", score: " + s.getValue()));
        printMessageln("End of player scores!");
    }

    private void printMessage(String message) {
        System.out.print(message);
        output.append(message);
    }

    private void printMessageln(String message) {
        System.out.println(message);
        output.append(message + "\n");
    }

    private synchronized void onPlayerExit(String leftPlayer) throws RemoteException {
        GameState currentState = this.getGameState();
        List<String> playerList = currentState.getPlayerList();
        playerList.remove(leftPlayer);
        System.out.println("Player is dead, " + id + " is update tracker to remove: " + leftPlayer);
        System.out.println("Currently live: ");
        playerList.forEach(System.out::println);
        tracker.removePlayer(leftPlayer);
        List<Pair<Integer, Integer>> availableLocation = currentState.getAvailableLocation();
        Map<String, Pair<Integer, Integer>> playersLocation = currentState.getPlayersLocation();
        Map<String, Integer> playersScore = currentState.getPlayersScore();
        Pair<Integer, Integer> currentLocation = playersLocation.get(leftPlayer);
        availableLocation.add(currentLocation);
        playersLocation.remove(leftPlayer);
        playersScore.remove(leftPlayer);
        if (isServer && leftPlayer.equals(currentState.getBackupPlayer())) {
            if (playerList.size() > 1) {
                String backupPlayer = playerList.get(1);
                currentState.setBackupPlayer(backupPlayer);
                this.backupPlayer = tracker.getPlayer(backupPlayer);
            } else {
                currentState.setBackupPlayer(null);
                this.backupPlayer = tracker.getPlayer(null);
            }
        }
        this.setGameState(currentState);
    }

    public void receivePing() throws RemoteException {
        if (!isServer) return;
        List<String> playerList = gameState.getPlayerList();
        List<String> toCheck = playerList.subList(1, playerList.size());
        System.out.println("player list: ");
        for (String player: toCheck) System.out.println(player);
        System.out.println("live player set: ");
        for (String player: livePlayers) System.out.println(player);
        System.out.println("Checked at time " + new SimpleDateFormat("mm:ss:SS").format(new Date(System.currentTimeMillis())));
        if (livePlayers.size() != toCheck.size()) {
            for (String player: toCheck) {
                if (!livePlayers.contains(player)) {
                    System.out.println("Player : " + player + " disconnected...");
                    this.onPlayerExit(player);
                    notifyBackup();
                }
            }
        }
        livePlayers.clear();
    }
}
