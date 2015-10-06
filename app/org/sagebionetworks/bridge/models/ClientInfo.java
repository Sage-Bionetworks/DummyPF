package org.sagebionetworks.bridge.models;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * <p>
 * Parsed representation of the User-Agent header provided by the client, when
 * it is in our prescribed format:
 * </p>
 * 
 * <p>
 * appName/appVersion (osName; osVersion) sdkName/sdkVersion
 * </p>
 * 
 * <p>
 * osName and osVersion are optional, and punctuation is removed when they are
 * not present. Some examples:
 * </p>
 * 
 * <ul>
 *     <li>Asthma/26 (Unknown iPhone; iPhone OS 9.1) BridgeSDK/4</li>
 *     <li>CardioHealth/1 (Unknown iPhone; iPhone OS 9.0.2) BridgeSDK/4</li>
 *     <li>Unknown Client/14 BridgeJavaSDK/10</li>
 * </ul>
 * 
 * <p>
 * Other clients with more typical browser user agent strings will be represented by ClientInfo that 
 * have all empty fields. Some examples of these headers, from our logs:
 * </p>
 * 
 * <ul>
 *     <li>Amazon Route 53 Health Check Service; ref:c97cd53f-2272-49d6-a8cd-3cd658d9d020; report http://amzn.to/1vsZADi</li>
 *     <li>Integration Tests (Linux/3.13.0-36-generic) BridgeJavaSDK/3</li>
 * </ul>
 * 
 * <p>
 * ClientInfo is not the end result of a generic user agent string parser. Those are very complicated and we 
 * do not need all this information (we log the user agent string from the client but only use these strings from 
 * when they are in a given format in order to filter some API responses).
 * </p>
 *
 */
public final class ClientInfo {

    /**
     * A cache of ClientInfo objects that have already been parsed from user agent strings. 
     * We're using this, rather than ConcurrentHashMap, because external clients submit this string, 
     * and thus could create an infinite number of them, starving the server. The cache will protect 
     * against this with its size limit.
     */
    private static LoadingCache<String, ClientInfo> userAgents = CacheBuilder.newBuilder()
       .maximumSize(500)
       .build(new CacheLoader<String,ClientInfo>() {
            @Override
            public ClientInfo load(String userAgent) throws Exception {
                return ClientInfo.parseUserAgentString(userAgent);
            }
       });

    /**
     * A User-Agent string that does not follow our format is simply an unknown
     * client, and no filtering will be done for such a client. It is represented with 
     * a null object that is the ClientInfo object with all null fields. It is still
     * logged as we find it in the request.
     */
    public static final ClientInfo UNKNOWN_CLIENT = new ClientInfo.Builder().build();

    /**
     * For example, "Unknown Client/14 BridgeJavaSDK/10".
     */
    private static final Pattern SHORT_STRING = Pattern.compile("^([^/]+)\\/(\\d+)\\s([^/\\(]*)\\/(\\d+)($)");
    /**
     * For example, "Asthma/26 (Unknown iPhone; iPhone OS 9.1) BridgeSDK/4".
     */
    private static final Pattern LONG_STRING = Pattern
            .compile("^([^/]+)\\/(\\d+)\\s\\(([^;]+);([^\\)]*)\\)\\s([^/]*)\\/(\\d+)($)");

    private final String appName;
    private final Integer appVersion;
    private final String osName;
    private final String osVersion;
    private final String sdkName;
    private final Integer sdkVersion;

    private ClientInfo(String appName, Integer appVersion, String osName, String osVersion, String sdkName,
            Integer sdkVersion) {
        this.appName = appName;
        this.appVersion = appVersion;
        this.osName = osName;
        this.osVersion = osVersion;
        this.sdkName = sdkName;
        this.sdkVersion = sdkVersion;
    }

    public String getAppName() {
        return appName;
    }

    public Integer getAppVersion() {
        return appVersion;
    }

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getSdkName() {
        return sdkName;
    }

    public Integer getSdkVersion() {
        return sdkVersion;
    }
    
    public boolean isTargetedAppVersion(Integer minValue, Integer maxValue) {
        // If there's no declared client version, it matches anything.
        if (appVersion != null) {
            // Otherwise we can't be outside of either range boundary if the boundary
            // is declared.
            if ((minValue != null && appVersion.intValue() < minValue.intValue()) || 
                (maxValue != null && appVersion.intValue() > maxValue.intValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(appName);
        result = prime * result + Objects.hashCode(appVersion);
        result = prime * result + Objects.hashCode(osName);
        result = prime * result + Objects.hashCode(osVersion);
        result = prime * result + Objects.hashCode(sdkName);
        result = prime * result + Objects.hashCode(sdkVersion);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ClientInfo other = (ClientInfo) obj;
        return Objects.equals(appName, other.appName) && Objects.equals(appVersion, other.appVersion)
                && Objects.equals(osName, other.osName) && Objects.equals(osVersion, other.osVersion)
                && Objects.equals(sdkName, other.sdkName) && Objects.equals(sdkVersion, other.sdkVersion);
    }

    @Override
    public String toString() {
        return "ClientInfo [appName=" + appName + ", appVersion=" + appVersion + ", osName=" + osName + ", osVersion="
                + osVersion + ", sdkName=" + sdkName + ", sdkVersion=" + sdkVersion + "]";
    }

    static class Builder {
        private String appName;
        private Integer appVersion;
        private String osName;
        private String osVersion;
        private String sdkName;
        private Integer sdkVersion;

        public Builder withAppName(String appName) {
            this.appName = appName;
            return this;
        }
        public Builder withAppVersion(Integer appVersion) {
            this.appVersion = appVersion;
            return this;
        }
        public Builder withOsName(String osName) {
            this.osName = osName;
            return this;
        }
        public Builder withOsVersion(String osVersion) {
            this.osVersion = osVersion;
            return this;
        }
        public Builder withSdkName(String sdkName) {
            this.sdkName = sdkName;
            return this;
        }
        public Builder withSdkVersion(Integer sdkVersion) {
            this.sdkVersion = sdkVersion;
            return this;
        }
        /**
         * It's valid to have a client info object with no fields, if the
         * User-Agent header is not in our prescribed format.
         */
        public ClientInfo build() {
            return new ClientInfo(appName, appVersion, osName, osVersion, sdkName, sdkVersion);
        }

    }
    
    /**
     * Get a ClientInfo object given a User-Agent header string. These values are cached and 
     * headers that are not in the prescribed format return an empty client info object.
     * @param userAgent
     * @return
     */
    public static ClientInfo fromUserAgentCache(String userAgent) {
        if (!StringUtils.isBlank(userAgent)) {
            try {
                return userAgents.get(userAgent);    
            } catch(ExecutionException e) {
                // This should not happen, the CacheLoader doesn't throw exceptions
                throw new RuntimeException(e);
            }
        }
        return UNKNOWN_CLIENT;
    }
    
    static ClientInfo parseUserAgentString(String ua) {
        ClientInfo info = UNKNOWN_CLIENT;
        if (!StringUtils.isBlank(ua)) {
            info = parseLongUserAgent(ua);
            if (info == UNKNOWN_CLIENT) {
                info = parseShortUserAgent(ua);
            }
        }
        return info;
    }

    private static ClientInfo parseLongUserAgent(String ua) {
        Matcher matcher = LONG_STRING.matcher(ua);
        if (matcher.matches()) {
            return new ClientInfo.Builder()
                .withAppName(matcher.group(1).trim())
                .withAppVersion(Integer.parseInt(matcher.group(2).trim()))
                .withOsName(matcher.group(3).trim())
                .withOsVersion(matcher.group(4).trim())
                .withSdkName(matcher.group(5).trim())
                .withSdkVersion(Integer.parseInt(matcher.group(6).trim())).build();
        }
        return UNKNOWN_CLIENT;
    }

    private static ClientInfo parseShortUserAgent(String ua) {
        Matcher matcher = SHORT_STRING.matcher(ua);
        if (matcher.matches()) {
            return new ClientInfo.Builder()
                .withAppName(matcher.group(1).trim())
                .withAppVersion(Integer.parseInt(matcher.group(2).trim()))
                .withSdkName(matcher.group(3).trim())
                .withSdkVersion(Integer.parseInt(matcher.group(4).trim())).build();
        }
        return UNKNOWN_CLIENT;
    }

}
