package com.whitewaves.screens;

import java.util.Hashtable;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

import com.whitewaves.Strings;
import com.whitewaves.WhiteWavesMIDlet;
import com.whitewaves.net.ApiClient;
import com.whitewaves.net.ApiClient.ApiCallback;
import com.whitewaves.storage.SessionStore;

public class HomeScreen extends Form implements CommandListener {
    private final WhiteWavesMIDlet midlet;
    private final String token;
    private final String nickname;

    private final Command findGameCommand = new Command("Знайти гру", Command.SCREEN, 1);
    private final Command changePasswordCommand = new Command("Змінити пароль", Command.SCREEN, 2);
    private final Command logoutCommand = new Command("Вийти", Command.SCREEN, 3);
    private final Command exitCommand = new Command("Вихід", Command.EXIT, 4);

    public HomeScreen(WhiteWavesMIDlet midlet, String token, String nickname) {
        super(nickname);
        this.midlet = midlet;
        this.token = token;
        this.nickname = nickname;
        append(new StringItem(null, "Завантаження статистики..."));
        addCommand(findGameCommand);
        addCommand(changePasswordCommand);
        addCommand(logoutCommand);
        addCommand(exitCommand);
        setCommandListener(this);
        loadStats();
    }

    private void loadStats() {
        ApiClient.getPlayerStats(nickname, new ApiCallback() {
            public void onSuccess(Object result) {
                Hashtable data = (Hashtable) result;
                Hashtable stats = (Hashtable) data.get("stats");
                deleteAll();
                append(new StringItem("Країна", str(data.get("country"))));
                append(new StringItem("Бої", str(stats.get("battles"))));
                append(new StringItem("Перемоги", str(stats.get("wins"))));
                append(new StringItem("Знищено кораблів", str(stats.get("shipsDestroyed"))));
                append(new StringItem("Очки", str(stats.get("points"))));
            }

            public void onError(String message) {
                deleteAll();
                append(new StringItem(null, Strings.cat("Помилка: ", message)));
            }
        });
    }

    private String str(Object o) {
        return o == null ? "0" : o.toString();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) {
            midlet.notifyDestroyed();
        } else if (c == logoutCommand) {
            SessionStore.clear();
            midlet.getDisplay().setCurrent(new LoginScreen(midlet));
        } else if (c == findGameCommand) {
            midlet.getDisplay().setCurrent(new FindGameScreen(midlet, token, nickname));
        } else if (c == changePasswordCommand) {
            midlet.getDisplay().setCurrent(new ChangePasswordScreen(midlet, token, nickname));
        }
    }
}
