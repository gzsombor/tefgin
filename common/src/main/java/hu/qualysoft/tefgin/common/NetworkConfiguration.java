package hu.qualysoft.tefgin.common;

import java.net.URI;

public class NetworkConfiguration {

    final private URI proxyUri;
    final private int port;
    final private String username;
    final private String password;

    public NetworkConfiguration(URI proxyUri) {
        this.proxyUri = proxyUri;
        this.port = getPortFromUri(proxyUri);
        if (proxyUri != null && proxyUri.getUserInfo() != null) {
            final String[] userPassword = proxyUri.getUserInfo().split(":");
            this.username = userPassword[0];
            this.password = userPassword[1];
        } else {
            this.username = null;
            this.password = null;
        }
    }

    private int getPortFromUri(URI uri) {
        if (uri == null) {
            return -1;
        }

        if (uri.getPort() > 0) {
            return uri.getPort();
        } else {
            if ("http".equals(uri.getScheme())) {
                return 80;
            } else if ("https".equals(uri.getScheme())) {
                return 443;
            }
        }
        return 3128;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public URI getProxyUri() {
        return proxyUri;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return proxyUri != null ? proxyUri.getHost() : null;
    }

    public boolean isHasProxy() {
        return proxyUri != null;
    }

    public boolean hasProxyAuthentication() {
        return username != null && password != null && !username.isEmpty() && !password.isEmpty();
    }
}
