package com.whitewaves.net;

import java.util.Hashtable;
import java.util.Vector;

// Мінімальний ручний JSON-парсер: CLDC/MIDP не мають вбудованого JSON.
// Об'єкти -> Hashtable, масиви -> Vector, числа без крапки/експоненти -> Integer, інші -> Double.
public class Json {

    public static Object parse(String text) {
        Parser p = new Parser(text);
        return p.parseValue();
    }

    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < 0x20) {
                sb.append("\\u");
                String hex = Integer.toHexString(c);
                for (int j = hex.length(); j < 4; j++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isWs(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static class Parser {
        private final String s;
        private int pos;

        Parser(String s) {
            this.s = s;
            this.pos = 0;
        }

        private void skipWs() {
            while (pos < s.length() && isWs(s.charAt(pos))) {
                pos++;
            }
        }

        Object parseValue() {
            skipWs();
            char c = s.charAt(pos);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            if (c == 't') {
                pos += 4;
                return new Boolean(true);
            }
            if (c == 'f') {
                pos += 5;
                return new Boolean(false);
            }
            if (c == 'n') {
                pos += 4;
                return null;
            }
            return parseNumber();
        }

        private Hashtable parseObject() {
            Hashtable map = new Hashtable();
            pos++; // {
            skipWs();
            if (s.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (true) {
                skipWs();
                String key = parseString();
                skipWs();
                pos++; // :
                Object value = parseValue();
                if (value != null) {
                    map.put(key, value);
                }
                skipWs();
                char c = s.charAt(pos);
                pos++;
                if (c == ',') {
                    continue;
                }
                if (c == '}') {
                    break;
                }
            }
            return map;
        }

        private Vector parseArray() {
            Vector list = new Vector();
            pos++; // [
            skipWs();
            if (s.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.addElement(value == null ? "" : value);
                skipWs();
                char c = s.charAt(pos);
                pos++;
                if (c == ',') {
                    continue;
                }
                if (c == ']') {
                    break;
                }
            }
            return list;
        }

        private String parseString() {
            StringBuffer sb = new StringBuffer();
            pos++; // opening quote
            while (true) {
                char c = s.charAt(pos);
                if (c == '"') {
                    pos++;
                    break;
                }
                if (c == '\\') {
                    pos++;
                    char esc = s.charAt(pos);
                    if (esc == '"') {
                        sb.append('"');
                    } else if (esc == '\\') {
                        sb.append('\\');
                    } else if (esc == '/') {
                        sb.append('/');
                    } else if (esc == 'n') {
                        sb.append('\n');
                    } else if (esc == 'r') {
                        sb.append('\r');
                    } else if (esc == 't') {
                        sb.append('\t');
                    } else if (esc == 'b') {
                        sb.append('\b');
                    } else if (esc == 'f') {
                        sb.append('\f');
                    } else if (esc == 'u') {
                        String hex = s.substring(pos + 1, pos + 5);
                        sb.append((char) Integer.parseInt(hex, 16));
                        pos += 4;
                    } else {
                        sb.append(esc);
                    }
                    pos++;
                } else {
                    sb.append(c);
                    pos++;
                }
            }
            return sb.toString();
        }

        private Object parseNumber() {
            int start = pos;
            boolean isFloat = false;
            while (pos < s.length()) {
                char c = s.charAt(pos);
                if (c == '-' || c == '+' || (c >= '0' && c <= '9')) {
                    pos++;
                } else if (c == '.' || c == 'e' || c == 'E') {
                    isFloat = true;
                    pos++;
                } else {
                    break;
                }
            }
            String numStr = s.substring(start, pos);
            if (isFloat) {
                return new Double(Double.parseDouble(numStr));
            }
            try {
                return new Integer(Integer.parseInt(numStr));
            } catch (NumberFormatException e) {
                return new Double(Double.parseDouble(numStr));
            }
        }
    }
}
