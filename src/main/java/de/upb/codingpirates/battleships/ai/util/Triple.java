package de.upb.codingpirates.battleships.ai.util;


public class Triple<T, Z, K>  {

    private T val1;
    private Z val2;
    private K val3;

    public Triple(T val1, Z val2, K val3) {

        this.val1 = val1;
        this.val2 = val2;
        this.val3 = val3;
    }


    //client id
    public T getVal1() {
        return val1;
    }

    //point
    public Z getVal2() {
        return val2;

    }

    //value
    public K getVal3() {
        return val3;
    }
    @Override
    public String toString(){
        return String.format("[Point %s with value %s of client %s ]", getVal2(), getVal3(), getVal1());
    }

}
