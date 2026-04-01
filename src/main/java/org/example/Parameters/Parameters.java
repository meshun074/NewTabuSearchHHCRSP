package org.example.Parameters;

public class Parameters {
    private final String instance;
    private final int numberOfIteration;

    public Parameters(String instance, int numberOfIteration) {
        this.instance = instance;
        this.numberOfIteration = numberOfIteration;
    }

    public String getInstance() {
        return instance;
    }

    public int getNumberOfIteration() {return numberOfIteration;}

}
