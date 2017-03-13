package trentastico.geridea.com.trentastico.network.operations;

/*
 * Created with ♥ by Slava on 13/03/2017.
 */
public class ParsingErrorOperation implements ILoadingOperation {

    @Override
    public String describe() {
        return "Non sono riuscito ad interpretare gli orari. Riprovo a scaricarli...";
    }

}
