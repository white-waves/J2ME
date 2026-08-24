package com.whitewaves.screens;

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

public class ChangePasswordScreen extends Form implements CommandListener {
    private final WhiteWavesMIDlet midlet;
    private final String token;
    private final String nickname;
    private final TextField oldPasswordField;
    private final TextField newPasswordField;
    private final Command saveCommand = new Command("Зберегти", Command.OK, 1);
    private final Command backCommand = new Command("Назад", Command.BACK, 2);

    public ChangePasswordScreen(WhiteWavesMIDlet midlet, String token, String nickname) {
        super("Зміна пароля");
        this.midlet = midlet;
        this.token = token;
        this.nickname = nickname;
        oldPasswordField = new TextField("Старий пароль", "", 32, TextField.PASSWORD);
        newPasswordField = new TextField("Новий пароль", "", 32, TextField.PASSWORD);
        append(oldPasswordField);
        append(newPasswordField);
        addCommand(saveCommand);
        addCommand(backCommand);
        setCommandListener(this);
    }

    public void commandAction(Command c, Displayable d) {
        if (c == backCommand) {
            midlet.getDisplay().setCurrent(new HomeScreen(midlet, token, nickname));
        } else if (c == saveCommand) {
            doSave();
        }
    }

    private void doSave() {
        final String oldPassword = oldPasswordField.getString();
        final String newPassword = newPasswordField.getString();
        if (oldPassword.length() == 0 || newPassword.length() == 0) {
            midlet.getDisplay().setCurrent(errorAlert("Заповніть обидва поля"), this);
            return;
        }
        busy();
        ApiClient.changePassword(token, oldPassword, newPassword, new ApiCallback() {
            public void onSuccess(Object result) {
                Alert a = new Alert("Готово", "Пароль змінено", null, AlertType.INFO);
                a.setTimeout(Alert.FOREVER);
                midlet.getDisplay().setCurrent(a, new HomeScreen(midlet, token, nickname));
            }

            public void onError(String message) {
                restore();
                midlet.getDisplay().setCurrent(errorAlert(message), ChangePasswordScreen.this);
            }
        });
    }

    private void busy() {
        deleteAll();
        append(new StringItem(null, "Зачекайте..."));
    }

    private void restore() {
        deleteAll();
        append(oldPasswordField);
        append(newPasswordField);
    }

    private Alert errorAlert(String message) {
        Alert a = new Alert("Помилка", message, null, AlertType.ERROR);
        a.setTimeout(Alert.FOREVER);
        return a;
    }
}
