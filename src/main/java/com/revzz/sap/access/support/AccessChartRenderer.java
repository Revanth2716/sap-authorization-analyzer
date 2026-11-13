package com.revzz.sap.access.support;

import com.revzz.sap.access.domain.RoleAssessment;
import com.revzz.sap.access.domain.Severity;
import com.revzz.sap.access.domain.UserExposure;

import java.util.List;

/**
 * Server-side SVG charts.
 *
 * <p>The charts are drawn on the server, as plain SVG markup, with no chart
 * library, no CDN and no client-side JavaScript. That is a deliberate choice
 * for a tool that has to run inside a corporate landscape where outbound
 * internet access is often blocked, and where a security review has to be
 * able to save the page and still see the evidence.</p>
 */
public final class AccessChartRenderer {

    private AccessChartRenderer() {
    }

    /** The colour of a severity band. */
    public static String severityColour(Severity severity) {
        if (severity == null) {
            return "#63748a";
        }
        switch (severity) {
            case CRITICAL:
                return "#d7263d";
            case HIGH:
                return "#e8710a";
            case MEDIUM:
                return "#b8860b";
            case LOW:
                return "#1a7f5a";
            default:
                return "#2e9e6b";
        }
    }

    /**
     * A horizontal bar chart of the governance score of every role.
     *
     * @param assessments the roles, already sorted worst first
     */
    public static String governanceBars(List<RoleAssessment> assessments) {
        int width = 900;
        int labelWidth = 168;
        int valueWidth = 84;
        int rowHeight = 34;
        int top = 46;
        int height = top + Math.max(1, assessments.size()) * rowHeight + 16;

        int barWidth = width - labelWidth - valueWidth - 24;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"governance\" viewBox=\"0 0 ").append(width).append(' ')
                .append(height).append("\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" ")
                .append("aria-label=\"Access governance score by role\">");

        svg.append("<text x=\"0\" y=\"18\" class=\"c-head\">GOVERNANCE SCORE BY ROLE</text>");
        svg.append("<text x=\"").append(width).append("\" y=\"18\" class=\"c-scale\" ")
                .append("text-anchor=\"end\">0 &#8211; 100</text>");

        svg.append("<rect x=\"").append(labelWidth).append("\" y=\"").append(top - 14)
                .append("\" width=\"").append(barWidth)
                .append("\" height=\"").append(Math.max(1, assessments.size()) * rowHeight)
                .append("\" fill=\"#eef2f7\" rx=\"4\"/>");

        for (int i = 0; i < assessments.size(); i++) {
            RoleAssessment assessment = assessments.get(i);
            int y = top + i * rowHeight;
            int filled = Math.round(barWidth
                    * Math.max(0, Math.min(100, assessment.getScore())) / 100f);
            String colour = severityColour(assessment.getSeverity());

            svg.append("<text x=\"0\" y=\"").append(y + 5)
                    .append("\" class=\"c-label\">")
                    .append(esc(assessment.getRole().getRoleName())).append("</text>");
            svg.append("<rect x=\"").append(labelWidth).append("\" y=\"").append(y - 11)
                    .append("\" width=\"").append(Math.max(2, filled))
                    .append("\" height=\"20\" fill=\"").append(colour).append("\" rx=\"3\"/>");
            svg.append("<text x=\"").append(labelWidth + barWidth + 8).append("\" y=\"").append(y + 4)
                    .append("\" class=\"c-value\" fill=\"").append(colour).append("\">")
                    .append(assessment.getScore()).append("</text>");
            svg.append("<text x=\"").append(labelWidth + barWidth + 40).append("\" y=\"").append(y + 4)
                    .append("\" class=\"c-sub\">").append(assessment.getSeverity().name())
                    .append("</text>");
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /**
     * The blast radius: one row per person, showing what they can reach.
     *
     * <p>A chip is a sensitive authorisation object the person ends up with
     * once the composite roles are expanded. A red marker means they are also
     * in a segregation-of-duties conflict. The people with the most chips are
     * the people to talk to first.</p>
     */
    public static String exposureMap(List<UserExposure> exposures) {
        int width = 900;
        int labelWidth = 150;
        int rowHeight = 40;
        int top = 58;
        int chipWidth = 116;
        int chipHeight = 24;
        int height = top + Math.max(1, exposures.size()) * rowHeight + 20;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg class=\"exposure\" viewBox=\"0 0 ").append(width).append(' ')
                .append(height).append("\" xmlns=\"http://www.w3.org/2000/svg\" role=\"img\" ")
                .append("aria-label=\"Who can reach which sensitive authorisation objects\">");

        svg.append("<text x=\"0\" y=\"18\" class=\"c-head\">WHO CAN REACH WHAT</text>");
        svg.append("<text x=\"").append(width).append("\" y=\"18\" class=\"c-scale\" ")
                .append("text-anchor=\"end\">one chip per sensitive object reached</text>");

        svg.append("<text x=\"0\" y=\"").append(top - 12)
                .append("\" class=\"c-sub\">PERSON</text>");
        svg.append("<text x=\"").append(labelWidth).append("\" y=\"").append(top - 12)
                .append("\" class=\"c-sub\">SENSITIVE AUTHORISATION OBJECTS REACHED</text>");

        for (int i = 0; i < exposures.size(); i++) {
            UserExposure exposure = exposures.get(i);
            int y = top + i * rowHeight;

            svg.append("<text x=\"0\" y=\"").append(y + 15)
                    .append("\" class=\"c-label\">")
                    .append(esc(exposure.getUserId())).append("</text>");
            svg.append("<text x=\"").append(labelWidth - 12).append("\" y=\"").append(y + 15)
                    .append("\" class=\"c-sub\" text-anchor=\"end\">")
                    .append(esc(exposure.getSystemId())).append("</text>");

            if (exposure.getSensitiveObjects().isEmpty()) {
                svg.append("<text x=\"").append(labelWidth).append("\" y=\"").append(y + 15)
                        .append("\" class=\"c-cell-missing\">nothing sensitive</text>");
            }
            for (int c = 0; c < exposure.getSensitiveObjects().size(); c++) {
                int x = labelWidth + c * (chipWidth + 8);
                svg.append("<rect x=\"").append(x).append("\" y=\"").append(y - 2)
                        .append("\" width=\"").append(chipWidth).append("\" height=\"")
                        .append(chipHeight).append("\" rx=\"5\" fill=\"#fdeaec\" "
                                + "stroke=\"#d7263d\"/>");
                svg.append("<text x=\"").append(x + chipWidth / 2).append("\" y=\"")
                        .append(y + 14).append("\" class=\"c-chip\">")
                        .append(esc(exposure.getSensitiveObjects().get(c))).append("</text>");
            }

            if (!exposure.getConflicts().isEmpty()) {
                int markerX = width - 14;
                svg.append("<circle cx=\"").append(markerX).append("\" cy=\"")
                        .append(y + 10).append("\" r=\"9\" fill=\"#d7263d\"/>");
                svg.append("<text x=\"").append(markerX).append("\" y=\"")
                        .append(y + 14).append("\" class=\"c-marker\">!</text>");
                svg.append("<text x=\"").append(markerX - 16).append("\" y=\"")
                        .append(y + 14).append("\" class=\"c-sub\" text-anchor=\"end\">")
                        .append(exposure.getConflicts().size()).append(" conflict")
                        .append(exposure.getConflicts().size() == 1 ? "" : "s")
                        .append("</text>");
            }
        }

        svg.append("</svg>");
        return svg.toString();
    }

    /** Escapes the five characters that matter in XML text and attributes. */
    public static String esc(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                case '\'':
                    out.append("&apos;");
                    break;
                default:
                    out.append(c);
            }
        }
        return out.toString();
    }
}
