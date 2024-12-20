package com.fixme;

public class FixCheckSumException extends Exception {
    public static final String checkSumMissing = "Checksum is missing.";
    public static final String checkSumIncorrect = "Checksum is incorrect.";

    public FixCheckSumException(String errorMessage) {
        super(errorMessage);
    }
}

