package com.revzz.sap.access;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * SAP Authorisation &amp; Access Governance Analyzer.
 *
 * <p>A role name is not an authorisation. The authorisation is the whole
 * expansion, held by a person, in a system.</p>
 *
 * <p>So this tool works on the expansion: what a composite role really
 * grants, who holds both sides of a segregation-of-duties rule, and whether
 * the same role holds the same values in development and production.</p>
 */
@SpringBootApplication
public class AccessApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccessApplication.class, args);
    }

    /** Entry point for a WAR deployment on the SAP NetWeaver Java stack. */
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(AccessApplication.class);
    }
}
