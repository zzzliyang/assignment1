import javafx.util.Pair;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GameState implements Serializable {

    // TODO: can be inferred from playerlist
    private String serverPlayer;
    // TODO: can be inferred from playerlist
    private String backupPlayer;

    private List<Pair<Integer, Integer>> coinsLocation;

    private Map<String, Pair<Integer, Integer>> playersLocation;

    private Map<String, Integer> playersScore;

    private List<Pair<Integer, Integer>> availableLocation;

    private List<String> playerList;

    public GameState() {
    }

    public GameState(String serverPlayer, String backupPlayer, List<Pair<Integer, Integer>> coinsLocation, Map<String, Pair<Integer, Integer>> playersLocation, Map<String, Integer> playersScore, List<Pair<Integer, Integer>> availableLocation, List<String> playerList) {
        this.serverPlayer = serverPlayer;
        this.backupPlayer = backupPlayer;
        this.coinsLocation = coinsLocation;
        this.playersLocation = playersLocation;
        this.playersScore = playersScore;
        this.availableLocation = availableLocation;
        this.playerList = playerList;
    }

    public String getServerPlayer() {
        return serverPlayer;
    }

    public void setServerPlayer(String serverPlayer) {
        this.serverPlayer = serverPlayer;
    }

    public String getBackupPlayer() {
        return backupPlayer;
    }

    public void setBackupPlayer(String backupPlayer) {
        this.backupPlayer = backupPlayer;
    }

    public List<Pair<Integer, Integer>> getCoinsLocation() {
        return coinsLocation;
    }

    public void setCoinsLocation(List<Pair<Integer, Integer>> coinsLocation) {
        this.coinsLocation = coinsLocation;
    }

    public Map<String, Pair<Integer, Integer>> getPlayersLocation() {
        return playersLocation;
    }

    public void setPlayersLocation(Map<String, Pair<Integer, Integer>> playersLocation) {
        this.playersLocation = playersLocation;
    }

    public Map<String, Integer> getPlayersScore() {
        return playersScore;
    }

    public void setPlayersScore(Map<String, Integer> playersScore) {
        this.playersScore = playersScore;
    }

    public List<Pair<Integer, Integer>> getAvailableLocation() {
        return availableLocation;
    }

    public void setAvailableLocation(List<Pair<Integer, Integer>> availableLocation) {
        this.availableLocation = availableLocation;
    }

    public List<String> getPlayerList() {
        return playerList;
    }

    public void setPlayerList(List<String> playerList) {
        this.playerList = playerList;
    }
}
