package trentastico.geridea.com.trentastico.model;

public class StudyCourse {

    private long department;
    private long course;
    private long year;

    public StudyCourse(long department, long course, long year) {
        this.department = department;
        this.course = course;
        this.year = year;
    }

    public long getDepartment() {
        return department;
    }

    public long getCourse() {
        return course;
    }

    public long getYear() {
        return year;
    }
}
