package com.geridea.trentastico.network.request;


/*
 * Created with ♥ by Slava on 29/03/2017.
 */

class ResponseUnsuccessfulException extends Exception {
    private int code;

    public ResponseUnsuccessfulException(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
