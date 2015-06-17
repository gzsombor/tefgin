package hu.qualysoft.tefgin.tefclient

import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.security.cert.CertificateException

class TrustAll implements X509TrustManager {
	
	override getAcceptedIssuers() {
		null
	}
	
	override checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}
	
	override checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
	}
	
}