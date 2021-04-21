
package com.ichi2.anki.exception;

public class NoEnoughServerSpaceException extends Exception {
    public long rest;
    public long need;
    public NoEnoughServerSpaceException(long rest,long need) {
        super("need "+need+",but rest is "+rest);
        this.rest=rest;
        this.need=need;
    }
}
