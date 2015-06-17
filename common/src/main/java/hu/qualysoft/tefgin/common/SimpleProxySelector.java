package hu.qualysoft.tefgin.common;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class SimpleProxySelector extends ProxySelector {

    Proxy httpProxy;

    public SimpleProxySelector(Proxy httpProxy) {
        this.httpProxy = httpProxy;
    }

    @Override
    public List<Proxy> select(URI uri) {
        if ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) {
            return Collections.singletonList(httpProxy);
        }
        return Collections.singletonList(Proxy.NO_PROXY);
    }

    public void setHttpProxy(Proxy httpProxy) {
        this.httpProxy = httpProxy;
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {

    }

}
