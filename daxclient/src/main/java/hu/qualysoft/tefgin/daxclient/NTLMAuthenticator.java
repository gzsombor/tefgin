package hu.qualysoft.tefgin.daxclient;

import hu.qualysoft.tefgin.common.NetworkConfiguration;
import hu.qualysoft.tefgin.common.Utils;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NTLMAuthenticator extends Authenticator {

    private static Logger LOG = LoggerFactory.getLogger(NTLMAuthenticator.class);
    private static NTLMAuthenticator instance;

    private final ThreadLocal<PasswordAuthentication> authentication = new ThreadLocal<>();

    private final Map<String, PasswordAuthentication> proxyPasswords = new HashMap<>();

    public static synchronized NTLMAuthenticator getInstance() {
        if (instance == null) {
            instance = new NTLMAuthenticator();
        }
        return instance;
    }

    private NTLMAuthenticator() {

    }

    public void setProxyAuthentication(String host, String username, String password) {
        proxyPasswords.put(host.toLowerCase(), new PasswordAuthentication(username, password.toCharArray()));
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        final String host = this.getRequestingHost();

        LOG.info("getPassword authentication for " + host + ", type=" + getRequestorType());

        if (getRequestorType() == RequestorType.PROXY) {
            final PasswordAuthentication proxyPassword = proxyPasswords.get(host.toLowerCase());
            if (proxyPassword != null) {
                LOG.info("proxy authentication returned, with username='{}'", proxyPassword.getUserName());
                return new PasswordAuthentication(proxyPassword.getUserName(), proxyPassword.getPassword());
            }
        }

        if (host.endsWith(".qualysoft.com")) {
            final PasswordAuthentication pw = authentication.get();
            LOG.info("qualysoft.com authentication with {} (username : {})", pw, pw != null ? pw.getUserName() : "none");
            return new PasswordAuthentication(pw.getUserName(), pw.getPassword());
        } else {
            return null;
        }
    }

    public void setPasswordAuthentication(PasswordAuthentication pw) {
        authentication.set(pw);
    }

    public void clearPasswordAuthentication() {
        authentication.remove();
    }

    public <X> X secureCall(final PasswordAuthentication auth, Callable<X> func) throws Exception {
        try {
            setPasswordAuthentication(auth);
            return func.call();
        } finally {
            clearPasswordAuthentication();
        }
    }

    /**
     * Install as the default authenticator
     */
    public void install() {
        Authenticator.setDefault(this);
        final NetworkConfiguration conf = Utils.getNetworkConfiguration();
        if (conf.isHasProxy() && conf.hasProxyAuthentication()) {

            if (conf.hasProxyAuthentication()) {
                this.setProxyAuthentication(conf.getHost(), conf.getUsername(), conf.getPassword());
            }
        }
    }

}
