package it.eng.dome.invoicing.observability.health;

// For more information, see: https://datatracker.ietf.org/doc/html/draft-inadarei-api-health-check-06

public enum HealthStatus {

    UNKNOWN(0),
    PASS(1),
    WARN(2),
    FAIL(3);

    private int severity;

    HealthStatus(int severity) {
        this.severity = severity;
    }

    public int getSeverity() {
        return this.severity;
    }

}
