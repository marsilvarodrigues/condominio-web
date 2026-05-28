package com.pmrodrigues.commons.versioning;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

/**
 * Custom MVC {@link RequestCondition} that matches requests against the {@code X-API-Version} header.
 *
 * <p>Matching rules:
 * <ul>
 *   <li>Header absent or blank — the condition matches (the controller is a candidate);
 *       {@link #compareTo} selects the highest-version candidate, so the latest version wins.</li>
 *   <li>Header present and equals this condition's version — match.</li>
 *   <li>Header present but different — no match ({@code null} returned).</li>
 * </ul>
 */
public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    /** Name of the HTTP request header used to select the API version. */
    public static final String API_VERSION_HEADER = "X-API-Version";

    private final String version;

    /**
     * Creates a condition for the given version string.
     *
     * @param version the API version this condition represents (e.g. {@code "1"})
     */
    public ApiVersionRequestCondition(String version) {
        this.version = version;
    }

    /**
     * Returns the version string this condition represents.
     *
     * @return version string
     */
    public String getVersion() {
        return version;
    }

    /**
     * Combines class-level and method-level conditions — method-level wins.
     *
     * @param other the method-level condition to combine with
     * @return the method-level condition
     */
    @Override
    public ApiVersionRequestCondition combine(ApiVersionRequestCondition other) {
        return other;
    }

    /**
     * Returns this condition if the request matches, or {@code null} if it does not.
     *
     * <p>A missing header causes the condition to return itself so the controller becomes
     * a candidate; {@link #compareTo} then picks the highest-version winner.
     *
     * @param request the current HTTP request
     * @return this condition on match, {@code null} otherwise
     */
    @Override
    public ApiVersionRequestCondition getMatchingCondition(HttpServletRequest request) {
        String requestedVersion = request.getHeader(API_VERSION_HEADER);
        if (requestedVersion == null || requestedVersion.isBlank()) {
            return this;
        }
        return version.equals(requestedVersion.trim()) ? this : null;
    }

    /**
     * Compares two conditions so that the one with the higher version number wins.
     * A negative return value means {@code this} is preferred over {@code other}.
     *
     * @param other   the other condition to compare against
     * @param request the current HTTP request (unused)
     * @return negative if {@code this} version is higher, positive if lower, zero if equal
     */
    @Override
    public int compareTo(ApiVersionRequestCondition other, HttpServletRequest request) {
        return Integer.compare(parseVersion(other.version), parseVersion(this.version));
    }

    private static int parseVersion(String v) {
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
