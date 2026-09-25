package com;

public class SampleJavaClass {

    /**
     * Sample method for testing Java compilation and DTK build.
     *
     * @param name Name to be displayed
     * @return Greeting message
     */
    public String getGreeting(String name) {
        return "Hello " + name + "! Sample Java class is working successfully.";
    }

    public static void main(String[] args) {
        SampleJavaClass sample = new SampleJavaClass();

        String message = sample.getGreeting("Crocs OMS");
        System.out.println(message);
    }
}