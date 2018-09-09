/*
 * Copyright 2004 Sun Microsystems, Inc. All  Rights Reserved.
 *  
 * Redistribution and use in source and binary forms, with or 
 * without modification, are permitted provided that the following 
 * conditions are met:
 * 
 * -Redistributions of source code must retain the above copyright  
 *  notice, this list of conditions and the following disclaimer.
 * 
 * -Redistribution in binary form must reproduce the above copyright 
 *  notice, this list of conditions and the following disclaimer in 
 *  the documentation and/or other materials provided with the 
 *  distribution.
 *  
 * Neither the name of Sun Microsystems, Inc. or the names of 
 * contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *  
 * You acknowledge that Software is not designed, licensed or 
 * intended for use in the design, construction, operation or 
 * maintenance of any nuclear facility.
 */
//package Server;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Tracker implements TrackerInterface {

    protected int N;
    protected int K;
    protected List<String> playerList = new ArrayList<>();
    protected Map<String, GamePlayerInterface> playerMap = new HashMap<>();

    public Tracker(int n, int k) {
        N = n;
        K = k;
    }

    public String sayHello() {
        return "Welcome to the game!";
    }

    @Override
    public GamePlayerInterface getPlayer(String id) throws RemoteException {
        return playerMap.get(id);
    }

    @Override
    public NewJoinerPack addPlayer(String id, GamePlayerInterface player) throws RemoteException {
        System.out.println("Player + " + id + " is joining...");
        if (player != null) {
            playerList.add(id);
            playerMap.put(id, player);
        }
        System.out.println("Current players: " + playerList.size());
        for (String playerName: playerList) System.out.println("--------------" + playerName + "--------------");
        return new NewJoinerPack(playerList, N, K);
    }

    @Override
    public void removePlayer(String id) throws RemoteException {
        playerList.remove(id);
        playerMap.remove(id);
    }

    @Override
    public void updateList(List<String> players) throws RemoteException {
        playerList = players == null ? new ArrayList<>() : players;
        for (String id : playerMap.keySet()) {
            if (!playerList.contains(id))
                playerMap.remove(id);
        }
    }

    @Override
    public void notifyOnServerChange(List<String> players) throws RemoteException {
        this.playerList = players;
    }

    public static void main(String args[]) {
        TrackerInterface stub = null;
        Registry registry = null;
        if (args.length != 3) {
            System.out.println("Wrong number of parameters...exiting");
            System.exit(0);
        }
        try {
            String n = args[1], k = args[2];
            Tracker obj = new Tracker(Integer.valueOf(n), Integer.valueOf(k));
            stub = (TrackerInterface) UnicastRemoteObject.exportObject(obj, 0);
            registry = LocateRegistry.getRegistry();
            registry.bind("TRACKER", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            try {
                registry.unbind("TRACKER");
                registry.bind("TRACKER", stub);
                System.err.println("Server ready");
            } catch (Exception ee) {
                System.err.println("Server exception: " + ee.toString());
                ee.printStackTrace();
            }
        }
    }
}
