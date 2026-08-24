package com.whitewaves.screens;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

import com.whitewaves.Strings;
import com.whitewaves.WhiteWavesMIDlet;
import com.whitewaves.net.ApiClient;
import com.whitewaves.net.ApiClient.ApiCallback;

public class GameSessionScreen extends Form implements CommandListener {
    private final WhiteWavesMIDlet midlet;
    private final String token;
    private final String myNickname;
    private final String lobbyId;

    private final Command readyCommand = new Command("Готовий", Command.OK, 1);
    private final Command backCommand = new Command("Назад", Command.BACK, 2);
    private final StringItem opponentItem;
    private final StringItem statusItem;

    private Timer timer;
    private boolean polling = false;
    private boolean inProgress = false;
    private boolean ready = false;

    public GameSessionScreen(WhiteWavesMIDlet midlet, String token, String myNickname, String lobbyId, Hashtable initialLobby) {
        super("Ігрова сесія");
        this.midlet = midlet;
        this.token = token;
        this.myNickname = myNickname;
        this.lobbyId = lobbyId;

        opponentItem = new StringItem("Суперник", opponentFrom(initialLobby));
        statusItem = new StringItem("Статус", (String) initialLobby.get("status"));
        append(opponentItem);
        append(statusItem);
        addCommand(readyCommand);
        addCommand(backCommand);
        setCommandListener(this);

        applyLobby(initialLobby);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                poll();
            }
        }, 3000, 3000);
    }

    private String opponentFrom(Hashtable lobby) {
        Vector nicknames = (Vector) lobby.get("nicknames");
        for (int i = 0; i < nicknames.size(); i++) {
            String n = (String) nicknames.elementAt(i);
            if (!n.equals(myNickname)) {
                return n;
            }
        }
        return "?";
    }

    private void applyLobby(Hashtable lobby) {
        String status = (String) lobby.get("status");
        statusItem.setText(status);
        opponentItem.setText(opponentFrom(lobby));
        if ("in_progress".equals(status)) {
            inProgress = true;
            stopTimer();
            removeCommand(readyCommand);
        }
    }

    private void poll() {
        if (inProgress || polling) {
            return;
        }
        polling = true;
        ApiClient.getLobby(token, lobbyId, new ApiCallback() {
            public void onSuccess(Object result) {
                polling = false;
                Hashtable data = (Hashtable) result;
                applyLobby((Hashtable) data.get("lobby"));
            }

            public void onError(String message) {
                polling = false;
                stopTimer();
                statusItem.setText(Strings.cat("Помилка: ", message));
            }
        });
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            stopTimer();
            midlet.getDisplay().setCurrent(new HomeScreen(midlet, token, myNickname));
        } else if (c == readyCommand) {
            doReady();
        }
    }

    private void doReady() {
        if (ready) {
            return;
        }
        ready = true;
        removeCommand(readyCommand);
        ApiClient.markReady(token, lobbyId, new ApiCallback() {
            public void onSuccess(Object result) {
                Hashtable data = (Hashtable) result;
                applyLobby((Hashtable) data.get("lobby"));
            }

            public void onError(String message) {
                ready = false;
                addCommand(readyCommand);
                statusItem.setText(Strings.cat("Помилка: ", message));
            }
        });
    }
}
