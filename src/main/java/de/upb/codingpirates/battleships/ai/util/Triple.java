package de.upb.codingpirates.battleships.ai.util;


/**
 * A Triple object can store any 3 values.
 *
 * @param <T> first
 * @param <Z> second
 * @param <K> third
 * @author Benjmain Kasten
 */
public class Triple<T, Z, K> {

    private T val1;
    private Z val2;
    private K val3;

    /**
     * Constructor
     *
     * @param val1 first
     * @param val2 second
     * @param val3 third
     */
    public Triple(T val1, Z val2, K val3) {
        this.val1 = val1;
        this.val2 = val2;
        this.val3 = val3;
    }


    //client id

    /**
     * Getter for value 1
     *
     * @return value 1
     */
    public T getVal1() {
        return val1;
    }

    //point

    /**
     * Getter for value 2
     *
     * @return value 2
     */
    public Z getVal2() {
        return val2;

    }

    //value

    /**
     * Getter for value 3
     *
     * @return value 3
     */
    public K getVal3() {
        return val3;
    }

    @Override
    public String toString() {
        return String.format("[%s, value %.4s, client %s]", getVal2(), getVal3(), getVal1());
    }

}
