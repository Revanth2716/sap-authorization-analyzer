package com.revzz.sap.access.domain;

/**
 * An access finding.
 *
 * <p>Every finding carries the recommended action, because "this user holds
 * two conflicting roles" is not something anybody can act on at 09:00.</p>
 */
public class AuthorizationFinding {

    private Severity severity;
    private String category;
    private String subject;
    private String message;
    private String recommendation;

    public AuthorizationFinding() {
    }

    public AuthorizationFinding(Severity severity, String category, String subject,
                                String message, String recommendation) {
        this.severity = severity;
        this.category = category;
        this.subject = subject;
        this.message = message;
        this.recommendation = recommendation;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
