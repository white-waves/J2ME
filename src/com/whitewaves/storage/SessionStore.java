package com.whitewaves.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class SessionStore {
    private static final String STORE_NAME = "session";

    public static class Session {
        public String token;
        public String login;
        public String nickname;
    }

    public static void save(String token, String login, String nickname) {
        try {
            RecordStore.deleteRecordStore(STORE_NAME);
        } catch (RecordStoreException e) {
            // не існував - ігноруємо
        }
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, true);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeUTF(token);
            dos.writeUTF(login);
            dos.writeUTF(nickname);
            byte[] data = bos.toByteArray();
            rs.addRecord(data, 0, data.length);
        } catch (IOException e) {
            // ігноруємо - сесія просто не збережеться
        } catch (RecordStoreException e) {
            // ігноруємо
        } finally {
            close(rs);
        }
    }

    public static Session load() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE_NAME, false);
            if (rs.getNumRecords() == 0) {
                return null;
            }
            byte[] data = rs.getRecord(1);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
            Session s = new Session();
            s.token = dis.readUTF();
            s.login = dis.readUTF();
            s.nickname = dis.readUTF();
            return s;
        } catch (RecordStoreException e) {
            return null;
        } catch (IOException e) {
            return null;
        } finally {
            close(rs);
        }
    }

    public static void clear() {
        try {
            RecordStore.deleteRecordStore(STORE_NAME);
        } catch (RecordStoreException e) {
            // не існував - ігноруємо
        }
    }

    private static void close(RecordStore rs) {
        if (rs != null) {
            try {
                rs.closeRecordStore();
            } catch (RecordStoreException e) {
                // ignore
            }
        }
    }
}
