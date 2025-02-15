package com.mycompany.newserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class NewServer {
    private static ArrayList<NewClient> clients = new ArrayList<>();
    private static ArrayList<String> userNames = new ArrayList<>();
    private static ArrayList<String> playRoomPlayers = new ArrayList<>();
    private static Timer playRoomTimer;
    private static boolean timerRunning = false;
    private static int timeLeft = 30; // Global timer variable

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(9991);
        System.out.println("Server is running on port 9090...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("Connected to a new client");

            NewClient clientThread = new NewClient(clientSocket, clients, userNames, playRoomPlayers);
            clients.add(clientThread);
            new Thread(clientThread).start();
        }
    }

    static void broadcastUserList() {
        String userList = "USERLIST:" + String.join(",", userNames);
        for (NewClient client : clients) {
            client.sendMessage(userList);
        }
    }

    static void broadcastPlayRoomList() {
        String playList = "PLAYROOM:" + String.join(",", playRoomPlayers);
        for (NewClient client : clients) {
            client.sendMessage(playList);
        }
    }

    static void startPlayRoomTimer() {
        if (playRoomPlayers.size() < 2) return; // Don't start if less than 2 players

        if (timerRunning) {
            // Reset timer when a new player joins
            timeLeft = 30;
            System.out.println("Timer reset to 30 seconds!");
            return;
        }

        timerRunning = true;
        timeLeft = 30; // Start countdown from 30
        playRoomTimer = new Timer();
        playRoomTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (playRoomPlayers.size() == 5) {
                    broadcastGameStart();
                    stopTimer();
                    return;
                }

                if (timeLeft == 0) {
                    broadcastGameStart();
                    stopTimer();
                } else {
                    broadcastTimeLeft(timeLeft);
                    timeLeft--;
                }
            }
        }, 0, 1000);
    }

    static void stopTimer() {
        if (playRoomTimer != null) {
            playRoomTimer.cancel();
            timerRunning = false;
        }
    }

    static void broadcastTimeLeft(int timeLeft) {
        for (NewClient client : clients) {
            client.sendMessage("TIMER:" + timeLeft);
        }
    }

    static void broadcastGameStart() {
        for (NewClient client : clients) {
            client.sendMessage("GAME_START");
        }
        stopTimer();
    }

    static void removeClient(NewClient client, String userName) {
        clients.remove(client);
        userNames.remove(userName);
        playRoomPlayers.remove(userName);
        broadcastUserList();
        broadcastPlayRoomList();
    }
}
