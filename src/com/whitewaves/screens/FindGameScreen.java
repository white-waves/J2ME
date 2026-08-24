package com.whitewaves.screens;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

import com.whitewaves.Strings;
import com.whitewaves.WhiteWavesMIDlet;
import com.whitewaves.net.ApiClient;
import com.whitewaves.net.ApiClient.ApiCallback;

public class FindGameScreen extends Form implements CommandListener {
    private final WhiteWavesMIDlet midlet;
    private final String token;
    private final String myNickname;
    private final Command backCommand = new Command("Назад", Command.BACK, 1);
    private final StringItem statusItem;
    private Timer timer;
    private boolean matched = false;
    private boolean polling = false;

    public FindGameScreen(WhiteWavesMIDlet midlet, String token, String myNickname) {
        super("Пошук гри");
        this.midlet = midlet;
        this.token = token;
        this.myNickname = myNickname;
        statusItem = new StringItem(null, "Очікування суперника...");
        append(statusItem);
        addCommand(backCommand);
        setCommandListener(this);
        timer = new Timer();
        poll();
        timer.schedule(new TimerTask() {
            public void run() {
                poll();
            }
        }, 3000, 3000);
    }

    private void poll() {
        if (matched || polling) {
            return;
        }
        polling = true;
        ApiClient.findGame(token, new ApiCallback() {
            public void onSuccess(Object result) {
                polling = false;
                Hashtable data = (Hashtable) result;
                String status = (String) data.get("status");
                if ("match_found".equals(status)) {
                    matched = true;
                    stopTimer();
                    Hashtable lobby = (Hashtable) data.get("lobby");
                    String lobbyId = (String) lobby.get("_id");
                    midlet.getDisplay().setCurrent(new GameSessionScreen(midlet, token, myNickname, lobbyId, lobby));
                } else {
                    statusItem.setText("Очікування суперника...");
                }
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
        }
    }
}
