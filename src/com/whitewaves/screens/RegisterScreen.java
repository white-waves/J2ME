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

public class RegisterScreen extends Form implements CommandListener {
    private final WhiteWavesMIDlet midlet;
    private final TextField loginField;
    private final TextField nicknameField;
    private final TextField passwordField;
    private final Command submitCommand = new Command("Зареєструватися", Command.OK, 1);
    private final Command backCommand = new Command("Назад", Command.BACK, 2);

    public RegisterScreen(WhiteWavesMIDlet midlet) {
        super("Реєстрація");
        this.midlet = midlet;
        loginField = new TextField("Логін", "", 32, TextField.ANY);
        nicknameField = new TextField("Нікнейм", "", 32, TextField.ANY);
        passwordField = new TextField("Пароль", "", 32, TextField.PASSWORD);
        append(loginField);
        append(nicknameField);
        append(passwordField);
        addCommand(submitCommand);
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.getDisplay().setCurrent(new LoginScreen(midlet));
        } else if (c == submitCommand) {
            doRegister();
        }
    }

    private void doRegister() {
        final String login = loginField.getString();
        final String nickname = nicknameField.getString();
        final String password = passwordField.getString();
        if (login.length() == 0 || nickname.length() == 0 || password.length() == 0) {
            midlet.getDisplay().setCurrent(errorAlert("Заповніть усі поля"), this);
            return;
        }
        busy();
        ApiClient.register(login, password, nickname, new ApiCallback() {
            public void onSuccess(Object result) {
                ApiClient.login(login, password, new ApiCallback() {
                    public void onSuccess(Object loginResult) {
                        Hashtable data = (Hashtable) loginResult;
                        String token = (String) data.get("token");
                        SessionStore.save(token, login, nickname);
                        midlet.getDisplay().setCurrent(new HomeScreen(midlet, token, nickname));
                    }

                    public void onError(String message) {
                        restore();
                        midlet.getDisplay().setCurrent(errorAlert(message), RegisterScreen.this);
                    }
                });
            }

            public void onError(String message) {
                restore();
                midlet.getDisplay().setCurrent(errorAlert(message), RegisterScreen.this);
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
        append(nicknameField);
        append(passwordField);
    }

    private Alert errorAlert(String message) {
        Alert a = new Alert("Помилка", message, null, AlertType.ERROR);
        a.setTimeout(Alert.FOREVER);
        return a;
    }
}
