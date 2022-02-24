package fr.openent.presences.core.constants;

public class UserType {
    public static final String TEACHER = "Teacher";
    public static final String PERSONNEL = "Personnel";
    public static final String RELATIVE = "Relative";
    public static final String STUDENT = "Student";

    private UserType() {
        throw new IllegalStateException("Utility class");
    }
}

