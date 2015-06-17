package hu.qualysoft.tefgin.common;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggingProxySelector extends ProxySelector {
    final static Logger LOG = LoggerFactory.getLogger(LoggingProxySelector.class);

    ProxySelector other;

    public LoggingProxySelector(ProxySelector other) {
        this.other = other;
    }

    @Override
    public List<Proxy> select(URI uri) {
        final List<Proxy> result = other.select(uri);
        LOG.info("Proxy for " + uri + " -> " + result);
        return result;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        LOG.warn("connectFailed to " + uri + " socketAddress=" + sa, ioe);
        other.connectFailed(uri, sa, ioe);
    }
}