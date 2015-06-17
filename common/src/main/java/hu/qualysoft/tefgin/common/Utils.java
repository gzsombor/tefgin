package hu.qualysoft.tefgin.common;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {

    private final static Logger LOG = LoggerFactory.getLogger(Utils.class);
    private static final String ENDPOINT_URL = "https://tswebservice.qualysoft.com/TsWebService.asmx";

    public static final boolean LOGGING = true;
    public static final String DOMAIN = "Qualysoft";

    private static NetworkConfiguration configuration;

    private Utils() {
    }

    public static void setupProxyServerFromEnvironment() {
        final NetworkConfiguration conf = getNetworkConfiguration();
        if (conf.isHasProxy()) {
            LOG.info("Configuring java proxy authentication settings to use host='{}', port='{}', username='{}', password='{}')", conf.getHost(),
                    conf.getPort(), conf.getUsername(), conf.getPassword());

            ProxySelector.setDefault(new SimpleProxySelector(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(conf.getHost(), conf.getPort()))));
        } else {
            LOG.info("No proxy specified!");
        }
        ProxySelector.setDefault(new LoggingProxySelector(ProxySelector.getDefault()));
    }

    private static URI getProxyURI() {
        final String proxy = System.getProperty("proxy");
        if (proxy != null) {
            LOG.info("proxy settings from the environment: '{}'", proxy);
            return URI.create(proxy);
        }
        return null;
    }

    public static synchronized NetworkConfiguration getNetworkConfiguration() {
        if (configuration == null) {
            configuration = new NetworkConfiguration(getProxyURI());
        }
        return configuration;
    }

    public static String getEndpointUrl() {
        final String daxwebservice = System.getProperty("daxwebservice");
        final String url = daxwebservice != null ? daxwebservice : ENDPOINT_URL;
        LOG.info("getEndpointUrl : System.getProperty('daxwebservice') -> '{}'", url);
        return url;
    }

    public static String getTefUrl() {
        final String tefDomain = System.getProperty("tef.host", "tef.porscheinformatik.com");
        final String tefUrl = System.getProperty("tef.webapp", "https://" + tefDomain + "/tef5");
        LOG.info("tefDomain : System.getProperty('tef.host') -> '{}'", tefDomain);
        LOG.info("tefUrl : System.getProperty('tef.webapp') -> '{}'", tefUrl);
        return tefUrl;
    }

}
