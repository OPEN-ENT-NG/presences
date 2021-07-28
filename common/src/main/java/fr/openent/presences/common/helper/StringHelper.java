package fr.openent.presences.common.helper;

public class StringHelper {

    private StringHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static String repeat(String toRepeat, int time) {
        return new String(new char[time]).replace("\0", toRepeat);
    }
}
