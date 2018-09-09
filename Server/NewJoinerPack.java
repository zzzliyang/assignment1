import java.io.Serializable;
import java.util.List;

public class NewJoinerPack implements Serializable {

    private List<String> players;

    private int N;

    private int K;

    public NewJoinerPack() {
    }

    public NewJoinerPack(List<String> players, int n, int k) {
        this.players = players;
        N = n;
        K = k;
    }

    public List<String> getPlayers() {
        return players;
    }

    public void setPlayers(List<String> players) {
        this.players = players;
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
}
