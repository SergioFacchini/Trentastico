package trentastico.geridea.com.trentastico.activities.network.operations;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
public class ReadingErrorOperation implements ILoadingOperation {

    @Override
    public String describe() {
        return "Non sono riuscito a scaricare gli orari. Hai una connessione internet attiva?";
    }

}
