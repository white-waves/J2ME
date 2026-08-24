package com.whitewaves;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

import com.whitewaves.net.ApiClient;
import com.whitewaves.screens.HomeScreen;
import com.whitewaves.screens.LoginScreen;
import com.whitewaves.storage.SessionStore;

public class WhiteWavesMIDlet extends MIDlet {
    private Display display;

    public void startApp() {
        if (display != null) {
            return;
        }
        display = Display.getDisplay(this);
        ApiClient.init(display);
        SessionStore.Session session = SessionStore.load();
        if (session != null) {
            display.setCurrent(new HomeScreen(this, session.token, session.nickname));
        } else {
            display.setCurrent(new LoginScreen(this));
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
    }

    public Display getDisplay() {
        return display;
    }
}
