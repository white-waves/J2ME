package com.whitewaves.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.Display;

import com.whitewaves.Config;
import com.whitewaves.Strings;

public class ApiClient {

    public interface ApiCallback {
        void onSuccess(Object result);
        void onError(String message);
    }

    private interface Body {
        Object run() throws Exception;
    }

    public static class ApiException extends Exception {
        private final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    private static Display display;

    public static void init(Display d) {
        display = d;
    }

    public static void register(final String login, final String password, final String nickname, ApiCallback cb) {
        execute(new Body() {
            public Object run() throws Exception {
                StringBuffer json = new StringBuffer();
                json.append('{');
                json.append("\"login\":\"").append(Json.escape(login)).append('"');
                json.append(",\"password\":\"").append(Json.escape(password)).append('"');
                json.append(",\"nickname\":\"").append(Json.escape(nickname)).append('"');
                json.append('}');
                return doRequest("POST", "/api/register", json.toString(), null);
            }
        }, cb);
    }

    public static void login(final String login, final String password, ApiCallback cb) {
        execute(new Body() {
            public Object run() throws Exception {
                StringBuffer json = new StringBuffer();
                json.append('{');
                json.append("\"login\":\"").append(Json.escape(login)).append('"');
                json.append(",\"password\":\"").append(Json.escape(password)).append('"');
                json.append('}');
                return doRequest("POST", "/api/login", json.toString(), null);
            }
        }, cb);
    }

    public static void changePassword(final String token, final String oldPassword, final String newPassword, ApiCallback cb) {
        execute(new Body() {
            public Object run() throws Exception {
                StringBuffer json = new StringBuffer();
                json.append('{');
                json.append("\"oldPassword\":\"").append(Json.escape(oldPassword)).append('"');
                json.append(",\"newPassword\":\"").append(Json.escape(newPassword)).append('"');
                json.append('}');
                return doRequest("POST", "/api/change-password", json.toString(), token);
            }
        }, cb);
    }

    public static void getPlayerStats(final String nickname, ApiCallback cb) {
        execute(new Body() {
            public Object run() throws Exception {
                return doRequest("GET", Strings.cat("/api/player-stats/", urlEncode(nickname)), null, null);
            }
        }, cb);
    }

    public static void findGame(final String token, ApiCallback cb) {
        execute(new Body() {
            public Object run() throws Exception {
                return doRequest("POST", "/api/find-game", null, token);
            }
        }, cb);
    }

    public static void getLobby(final String token, final String lobbyId, ApiCallback cb) {
        execute(new Body() {
            public Object run() throws Exception {
                return doRequest("GET", Strings.cat("/api/lobby/", lobbyId), null, token);
            }
        }, cb);
    }

    public static void markReady(final String token, final String lobbyId, ApiCallback cb) {
        execute(new Body() {
            public Object run() throws Exception {
                return doRequest("POST", Strings.cat("/api/lobby/", lobbyId, "/ready"), null, token);
            }
        }, cb);
    }

    private static void execute(final Body body, final ApiCallback cb) {
        final Display d = display;
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Object result = body.run();
                    d.callSerially(new Runnable() {
                        public void run() {
                            cb.onSuccess(result);
                        }
                    });
                } catch (final Exception e) {
                    final String msg = (e instanceof ApiException) ? e.getMessage() : "Помилка з'єднання";
                    d.callSerially(new Runnable() {
                        public void run() {
                            cb.onError(msg);
                        }
                    });
                }
            }
        }).start();
    }

    private static Object doRequest(String method, String path, String body, String token) throws Exception {
        HttpConnection conn = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            conn = (HttpConnection) Connector.open(Strings.cat(Config.API_BASE_URL, path), Connector.READ_WRITE, true);
            conn.setRequestMethod(method);
            if (token != null) {
                conn.setRequestProperty("Authorization", Strings.cat("Bearer ", token));
            }
            if (body != null) {
                byte[] bodyBytes = body.getBytes("UTF-8");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
                out = conn.openOutputStream();
                out.write(bodyBytes);
                out.flush();
            }
            int status = conn.getResponseCode();
            in = conn.openInputStream();
            String responseText = readAll(in);
            Object parsed = responseText.length() > 0 ? Json.parse(responseText) : null;
            if (status < 200 || status >= 300) {
                String msg = "Помилка сервера";
                if (parsed instanceof Hashtable) {
                    Object err = ((Hashtable) parsed).get("error");
                    if (err != null) {
                        msg = err.toString();
                    }
                }
                throw new ApiException(status, msg);
            }
            return parsed;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[512];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return new String(buf.toByteArray(), "UTF-8");
    }

    private static String urlEncode(String s) {
        StringBuffer sb = new StringBuffer();
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF-8");
        } catch (IOException e) {
            bytes = s.getBytes();
        }
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            boolean safe = (b >= 'a' && b <= 'z') || (b >= 'A' && b <= 'Z') || (b >= '0' && b <= '9')
                    || b == '-' || b == '_' || b == '.' || b == '~';
            if (safe) {
                sb.append((char) b);
            } else {
                sb.append('%');
                String hex = Integer.toHexString(b).toUpperCase();
                if (hex.length() < 2) {
                    sb.append('0');
                }
                sb.append(hex);
            }
        }
        return sb.toString();
    }
}
