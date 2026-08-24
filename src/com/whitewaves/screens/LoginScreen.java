package com.whitewaves.screens;

import java.util.Hashtable;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

import com.whitewaves.WhiteWavesMIDlet;
import com.whitewaves.net.ApiClient;
import com.whitewaves.net.ApiClient.ApiCallback;
import com.whitewaves.storage.SessionStore;

public class LoginScreen extends Form implements CommandListener {
    private final WhiteWavesMIDlet midlet;
    private final TextField loginField;
    private final TextField passwordField;
    private final Command loginCommand = new Command("Увійти", Command.OK, 1);
    private final Command registerCommand = new Command("Реєстрація", Command.SCREEN, 2);
    private final Command exitCommand = new Command("Вихід", Command.EXIT, 3);

    public LoginScreen(WhiteWavesMIDlet midlet) {
        super("White Waves");
        this.midlet = midlet;
        loginField = new TextField("Логін", "", 32, TextField.ANY);
        passwordField = new TextField("Пароль", "", 32, TextField.PASSWORD);
        append(loginField);
        append(passwordField);
        addCommand(loginCommand);
        addCommand(registerCommand);
        addCommand(exitCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) {
            midlet.notifyDestroyed();
        } else if (c == registerCommand) {
            midlet.getDisplay().setCurrent(new RegisterScreen(midlet));
        } else if (c == loginCommand) {
            doLogin();
        }
    }

    private void doLogin() {
        final String login = loginField.getString();
        final String password = passwordField.getString();
        if (login.length() == 0 || password.length() == 0) {
            midlet.getDisplay().setCurrent(errorAlert("Введіть логін і пароль"), this);
            return;
        }
        busy();
        ApiClient.login(login, password, new ApiCallback() {
            public void onSuccess(Object result) {
                Hashtable data = (Hashtable) result;
                String token = (String) data.get("token");
                Hashtable user = (Hashtable) data.get("user");
                String nickname = (String) user.get("nickname");
                SessionStore.save(token, login, nickname);
                midlet.getDisplay().setCurrent(new HomeScreen(midlet, token, nickname));
            }

            public void onError(String message) {
                restore();
                midlet.getDisplay().setCurrent(errorAlert(message), LoginScreen.this);
            }
        });
    }

    private void busy() {
        deleteAll();
        append(new StringItem(null, "Зачекайте..."));
    }

    private void restore() {
        deleteAll();
        append(loginField);
        append(passwordField);
    }

    private Alert errorAlert(String message) {
        Alert a = new Alert("Помилка", message, null, AlertType.ERROR);
        a.setTimeout(Alert.FOREVER);
        return a;
    }
}
