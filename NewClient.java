package com.mycompany.newserver;

import java.io.*;
import java.net.*;
import java.util.*;

class NewClient implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private ArrayList<NewClient> clients;
    private ArrayList<String> userNames;
    private ArrayList<String> playRoomPlayers;
    private String userName;

    public NewClient(Socket c, ArrayList<NewClient> clients, ArrayList<String> userNames, ArrayList<String> playRoomPlayers) throws IOException {
        this.client = c;
        this.clients = clients;
        this.userNames = userNames;
        this.playRoomPlayers = playRoomPlayers;
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            userName = in.readLine();
            if (userName == null || userName.trim().isEmpty() || userNames.contains(userName)) {
                out.println("ERROR");
                client.close();
                return;
            }

            userNames.add(userName);
            out.println("USERLIST:" + String.join(",", userNames));
            NewServer.broadcastUserList();

            String request;
            while ((request = in.readLine()) != null) {
                if (request.equalsIgnoreCase("PLAY")) {
                    if (playRoomPlayers.size() >= 5) {
                        out.println("PLAYROOM_FULL");
                    } else {
                        playRoomPlayers.add(userName);
                        NewServer.broadcastPlayRoomList();
                        NewServer.startPlayRoomTimer(); // Now resets timer when a new player joins
                    }
                } else if (request.equalsIgnoreCase("EXIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client connection error.");
        } finally {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            NewServer.removeClient(this, userName);
        }
    }

    void sendMessage(String message) {
        out.println(message);
    }
}
