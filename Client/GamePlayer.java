import javafx.util.Pair;
import org.apache.commons.lang3.SerializationUtils;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GamePlayer implements GamePlayerInterface {
  final Object lock = new Object();
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
    return gameState;
  }

  public void setGameState(GameState gameState) {
    this.gameState = gameState;
  }

  @Override
  public GameState newPlayerJoined(String newJoinerId) throws RemoteException {
    synchronized (lock) {
      List<String> playerList = this.gameState.getPlayerList();
      playerList.add(newJoinerId);
      if (playerList.size() == 2) {
        this.gameState.setBackupPlayer(newJoinerId);
        this.setBackupPlayer(tracker.getPlayer(newJoinerId));
      }
      List<Pair<Integer, Integer>> availableLocation = this.gameState.getAvailableLocation();
      int random = ThreadLocalRandom.current().nextInt(0, availableLocation.size() + 1);
      Map<String, Pair<Integer, Integer>> playersLocation = this.gameState.getPlayersLocation();
      playersLocation.put(newJoinerId, availableLocation.get(random));
      availableLocation.remove(random);
      Map<String, Integer> playersScore = this.gameState.getPlayersScore();
      playersScore.put(newJoinerId, 0);
      return SerializationUtils.clone(gameState);
    }
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
      this.gameState = serverPlayer.newPlayerJoined(id);
    }
    printGameState();
  }

  private void generateTreasure() {
    List<Pair<Integer, Integer>> availableLocation = this.gameState.getAvailableLocation();
    int random = ThreadLocalRandom.current().nextInt(0, availableLocation.size() + 1);
    Pair<Integer, Integer> newLocation = availableLocation.get(random);
    this.gameState.getCoinsLocation().add(newLocation);
    this.gameState.getAvailableLocation().remove(newLocation);
    System.out.println(
        "New coin generated at " + newLocation.getKey() + ", " + newLocation.getValue());
  }

  @Override
  public GameState movePlayer(String id, int direction) throws RemoteException {
    synchronized (lock) {
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
  }

  public void move(int direction) throws RemoteException {
    System.out.println("User: " + id + " action is: " + direction);
    GameState refresh = serverPlayer.movePlayer(id, direction);
    this.setGameState(refresh);
    printGameState();
  }

  @Override
  public void pingServer() {
    try {
      serverPlayer.isAlive();
      // System.out.println("Server is alive...");
    } catch (RemoteException e) {
      // System.out.println("Server down...");
    }
  }

  @Override
  public void pingBackup() {}

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
      // This backupd server has been promoted
      if (gameState.getServerPlayer().equals(id)) {
        this.serverPlayer = this;
        tracker.notifyOnServerChange(gameState.getPlayerList());
        this.backupPlayer = tracker.getPlayer(gameState.getBackupPlayer());
      }
    } catch (Exception e) {
      System.out.println("Not bakup server...");
    }
    return false;
  }

  @Override
  public void updateTrackerWithPlayerList(List<String> currentPlayers) {}

  @Override
  public void promote() {}

  private void printGameState() {
    Map<Pair<Integer, Integer>, String> locationMap = new HashMap<>();
    List<Pair<Integer, Integer>> coinLocList = new ArrayList<>(gameState.getCoinsLocation());
    Map<String, Pair<Integer, Integer>> playerLocMap = new HashMap<>(gameState.getPlayersLocation());
    Map<String, Integer> scores = new HashMap<>(gameState.getPlayersScore());
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

    System.out.println("Game server is :" + gameState.getServerPlayer());
    System.out.println("Backup server is :" + gameState.getBackupPlayer());
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
}
