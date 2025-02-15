package com.mycompany.newserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ClientGUI {
    private JFrame connectionFrame;
    private JFrame waitingRoomFrame;
    private JFrame playRoomFrame;
    private JTextField nameField;
    private JTextArea waitingRoomArea;
    private JTextArea playRoomArea;
    private JLabel timerLabel;
    private JButton connectButton;
    private JButton playButton;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String userName;
    private static final int SERVER_PORT = 9991;
    private static final String SERVER_IP = "localhost";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    public ClientGUI() {
        setupConnectionFrame();
    }

    private void setupConnectionFrame() {
        connectionFrame = new JFrame("Player Connection Room");
        connectionFrame.setSize(300, 150);
        connectionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectionFrame.setLayout(new BorderLayout());

        JPanel panel = new JPanel(new FlowLayout());
        panel.add(new JLabel("Enter Name:"));
        nameField = new JTextField(15);
        panel.add(nameField);
        connectButton = new JButton("Connect");
        panel.add(connectButton);

        connectionFrame.add(panel, BorderLayout.CENTER);
        connectButton.addActionListener(e -> connectToServer());

        connectionFrame.setVisible(true);
    }

    private void setupWaitingRoomFrame() {
        waitingRoomFrame = new JFrame("Waiting Room");
        waitingRoomFrame.setSize(400, 300);
        waitingRoomFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        waitingRoomFrame.setLayout(new BorderLayout());

        waitingRoomArea = new JTextArea();
        waitingRoomArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(waitingRoomArea);

        playButton = new JButton("PLAY");
        playButton.addActionListener(e -> sendPlayRequest());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(playButton);

        waitingRoomFrame.add(scrollPane, BorderLayout.CENTER);
        waitingRoomFrame.add(buttonPanel, BorderLayout.SOUTH);
        waitingRoomFrame.setVisible(true);
    }

    private void setupPlayRoomFrame() {
        playRoomFrame = new JFrame("Play Room");
        playRoomFrame.setSize(400, 300);
        playRoomFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        playRoomFrame.setLayout(new BorderLayout());

        playRoomArea = new JTextArea();
        playRoomArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(playRoomArea);

        timerLabel = new JLabel("Time Left: 30", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 18));

        playRoomFrame.add(scrollPane, BorderLayout.CENTER);
        playRoomFrame.add(timerLabel, BorderLayout.NORTH);
        playRoomFrame.setVisible(true);
    }

    private void connectToServer() {
        userName = nameField.getText().trim();
        if (userName.isEmpty()) {
            JOptionPane.showMessageDialog(connectionFrame, "Please enter a valid name.");
            return;
        }

        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(userName);
            String response = in.readLine();
            if ("ERROR".equals(response)) {
                JOptionPane.showMessageDialog(connectionFrame, "Username already taken.");
                socket.close();
                return;
            }

            connectionFrame.setVisible(false);
            setupWaitingRoomFrame();

            new Thread(this::listenForMessages).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(connectionFrame, "Connection failed.");
        }
    }

    private void sendPlayRequest() {
        out.println("PLAY");
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("USERLIST:")) {
                    waitingRoomArea.setText("Connected Users:\n" + message.substring(9).replace(",", "\n"));
                } else if (message.startsWith("PLAYROOM:")) {
                    if (playRoomFrame == null) {
                        setupPlayRoomFrame();
                    }
                    playRoomArea.setText("Players in Play Room:\n" + message.substring(9).replace(",", "\n"));
                } else if (message.startsWith("TIMER:")) {
                    timerLabel.setText("Time Left: " + message.substring(6));
                } else if (message.equals("PLAYROOM_FULL")) {
                    JOptionPane.showMessageDialog(waitingRoomFrame, "Play Room is full!");
                } else if (message.equals("GAME_START")) {
                    JOptionPane.showMessageDialog(playRoomFrame, "Game Started!");
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(playRoomFrame, "Disconnected from server.");
        }
    }
}
