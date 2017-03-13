package trentastico.geridea.com.trentastico;


/*
 * Created with ♥ by Slava on 13/03/2017.
 */

public final class Config {

    //Networking

    /**
     * How much ms we should wait after a request has failed loading and the next retry?
     */
    public static final int WAITING_TIME_AFTER_A_REQUEST_FAILED = 5000;

    //Database
    public static final String DATABASE_NAME = "data.db";
    public static final int DATABASE_VERSION = 1;
}
