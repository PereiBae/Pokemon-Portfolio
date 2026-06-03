package com.pokemonportfolio.config.domain;

public enum VerificationStatus {
    UNVERIFIED("Unverified"),
    VERIFIED("Verified");

    private final String label;

    VerificationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
