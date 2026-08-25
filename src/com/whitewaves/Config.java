package com.whitewaves;

public class Config {
    // Продакшн-сервер на AWS.
    // Для локальної розробки в KEmulator на тому ж ПК заміни на "http://localhost:3000",
    // для реального телефону в LAN — на "http://<LAN-IP-ПК>:3000".
    public static final String API_BASE_URL = "http://13.60.154.115:3000";
}
