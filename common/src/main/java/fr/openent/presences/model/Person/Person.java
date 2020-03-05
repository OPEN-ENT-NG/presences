package fr.openent.presences.model.Person;

public class Person {

    protected String id;
    protected String displayName;
    protected String info; // (could be email, phone number...)

    public String getName() {
        return displayName;
    }

    public void setName(String name) {
        this.displayName = name;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return info;
    }

    public void setEmail(String info) {
        this.info = info;
    }

}
