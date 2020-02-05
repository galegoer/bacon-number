package ca.utoronto.utm.mcs;

public class Memory 
{
    private static String value;

    public String getValue() {
        return value;
    }

    public void setValue(String newVal) {
        value = newVal;
    }

    public Memory() {}
}
