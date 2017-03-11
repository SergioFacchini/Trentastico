package trentastico.geridea.com.trentastico.activities.model;

/*
 * Created with ♥ by Slava on 03/03/2017.
 */
public class Course {

    private final int id;
    private final String name;

    public Course(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
