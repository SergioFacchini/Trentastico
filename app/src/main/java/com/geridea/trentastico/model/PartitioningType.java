package com.geridea.trentastico.model;


/*
 * Created with ♥ by Slava on 24/03/2017.
 */

public enum PartitioningType {
    /** Nothing has been found */
    NONE("(^$)"),

    /**
     * "Matricole pari"/"Matricole dispari"
     */
    ODD_EVEN("\\((Matricole (dis)?pari)\\)"),

    /**
     * "Gruppo A"/"Gruppo B"
     */
    NAMED_GROUP("\\((Gruppo [A-Za-z])\\)"),

    /**
     * "Mod.1"/"Mod.2"
     */
    ENUMERATED_GROUP("\\((Mod. ?[A-Za-z0-9])\\)"),

    /**
     * "A-L"/"M-Z" - "A-K"/"L-Z"
     */
    FIRST_LETTER_GROUP("\\(([A-Za-z]-[A-Za-z])\\)"),

    /**
     * "Gruppo 1 di 3"/"Gruppo 2 di 3"/"Gruppo 3 di 3"
     */
    N_OF_N_GROUP("\\((Gruppo [0-9] di [0-9])\\)"),

    /**
     * "Part. AL 1° modulo"/"Part. MZ 1° modulo"/"Part. AL 2° modulo"/"Part. MZ 2° modulo"
     */
    GROUP_MODULE("\\((Part. [A-Za-z][A-Za-z] [0-9]° modulo)\\)");

    private final String regex;

    PartitioningType(String regex) {
        this.regex = regex;
    }

    public String getRegex() {
        return regex;
    }
}
