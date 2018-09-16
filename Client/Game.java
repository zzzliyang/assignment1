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
//package example.hello;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Game {

    private Game() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Wrong number of parameters...exiting");
            System.exit(0);
        }
        String id = args[2];
        String host = (args.length < 1) ? null : args[0];
        GamePlayerInterface stub = null;
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            TrackerInterface tracker = (TrackerInterface) registry.lookup("TRACKER");
            GamePlayer gamePlayer = new GamePlayer(id, tracker);
            stub = (GamePlayerInterface) UnicastRemoteObject.exportObject(gamePlayer, 0);
            gamePlayer.joinGame(stub);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Runnable runnablePingServer = () -> {
                try {
                    //TimeUnit.MILLISECONDS.wait(500);
                    while (true) {
                        Thread.sleep(500);
                        gamePlayer.pingServer();
                        Thread.sleep(500);
                        gamePlayer.pingBackup();
                    }
                } catch (InterruptedException | RemoteException e) {
                    System.out.println("Pinging service interrupted...");
                }
            };
            Runnable runnableReceivePing = () -> {
                try {
                    //TimeUnit.MILLISECONDS.wait(500);
                    while (true) {
                        Thread.sleep(2001);
                        gamePlayer.receivePing();
                    }
                } catch (InterruptedException | RemoteException e) {
                    System.out.println("Pinging service interrupted...");
                }
            };
            executor.execute(runnablePingServer);
            executor.execute(runnableReceivePing);
            Scanner keys = new Scanner(System.in);
            System.out.println("User action: ");
            int action = keys.nextInt();
            int[] acceptableAction = new int[]{0, 1, 2, 3, 4, 9};
            while (true) {
                if (action == 0 || action == 1 || action == 2 || action == 3 || action == 4 || action == 9) {
                    gamePlayer.move(action);
                    if (action == 9)
                        System.exit(0);
                } else
                    System.out.println("Invalid input!");
                System.out.println("User action: ");
                action = keys.nextInt();
            }
//	    hello.setAnother(stub);
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
