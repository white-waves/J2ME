package com.whitewaves;

// javac компілює оператор "+" для рядків через StringBuilder, якого немає в CLDC bootclasspath.
// Тому конкатенація тут - явно через StringBuffer.
public class Strings {
    public static String cat(String a, String b) {
        return new StringBuffer(a).append(b).toString();
    }

    public static String cat(String a, String b, String c) {
        return new StringBuffer(a).append(b).append(c).toString();
    }
}
