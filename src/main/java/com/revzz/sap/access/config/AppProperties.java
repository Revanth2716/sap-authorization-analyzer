package com.revzz.sap.access.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Application configuration.
 *
 * <p>The policy lives here, not in the code: how far a role may differ
 * between systems before it counts as drift, how broad a composite role may
 * get, and how long an unused role may linger. These are the numbers a
 * customer agrees with its security team, so they belong in configuration.</p>
 */
@Component
@ConfigurationProperties(prefix = "access")
public class AppProperties {

    private String applicationName = "SAP Authorisation & Access Governance Analyzer";
    private String sapSystem = "PFCG";
    private boolean seedDemoUsers = true;
    private boolean seedDemoData = true;
    private String demoPassword = "demo1234";
    private String gateway = "local";
    private Seed seed = new Seed();
    private Policy policy = new Policy();
    private Mail mail = new Mail();

    public static class Seed {
        private long randomSeed = 20261113L;

        public long getRandomSeed() {
            return randomSeed;
        }

        public void setRandomSeed(long randomSeed) {
            this.randomSeed = randomSeed;
        }
    }

    public static class Policy {
        private int orphanRoleDays = 90;
        private int maxDriftSystems = 0;
        private int maxCompositeExpansion = 5;

        public int getOrphanRoleDays() {
            return orphanRoleDays;
        }

        public void setOrphanRoleDays(int orphanRoleDays) {
            this.orphanRoleDays = orphanRoleDays;
        }

        public int getMaxDriftSystems() {
            return maxDriftSystems;
        }

        public void setMaxDriftSystems(int maxDriftSystems) {
            this.maxDriftSystems = maxDriftSystems;
        }

        public int getMaxCompositeExpansion() {
            return maxCompositeExpansion;
        }

        public void setMaxCompositeExpansion(int maxCompositeExpansion) {
            this.maxCompositeExpansion = maxCompositeExpansion;
        }
    }

    public static class Mail {
        private boolean enabled = false;
        private String from = "access-governance@example.com";
        private String to = "basis-team@example.com";
        private String subjectPrefix = "[SAP Access]";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getSubjectPrefix() {
            return subjectPrefix;
        }

        public void setSubjectPrefix(String subjectPrefix) {
            this.subjectPrefix = subjectPrefix;
        }
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getSapSystem() {
        return sapSystem;
    }

    public void setSapSystem(String sapSystem) {
        this.sapSystem = sapSystem;
    }

    public boolean isSeedDemoUsers() {
        return seedDemoUsers;
    }

    public void setSeedDemoUsers(boolean seedDemoUsers) {
        this.seedDemoUsers = seedDemoUsers;
    }

    public boolean isSeedDemoData() {
        return seedDemoData;
    }

    public void setSeedDemoData(boolean seedDemoData) {
        this.seedDemoData = seedDemoData;
    }

    public String getDemoPassword() {
        return demoPassword;
    }

    public void setDemoPassword(String demoPassword) {
        this.demoPassword = demoPassword;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public Seed getSeed() {
        return seed;
    }

    public void setSeed(Seed seed) {
        this.seed = seed;
    }

    public Policy getPolicy() {
        return policy;
    }

    public void setPolicy(Policy policy) {
        this.policy = policy;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }
}
