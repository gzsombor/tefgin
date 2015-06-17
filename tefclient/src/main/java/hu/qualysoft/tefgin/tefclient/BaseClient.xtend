package hu.qualysoft.tefgin.tefclient

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import hu.qualysoft.tefgin.common.NetworkConfiguration
import java.io.InputStreamReader
import java.io.Reader
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.AbstractExecutionAwareRequest
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.BasicCookieStore
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.SystemDefaultRoutePlanner
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory

class BaseClient {

	static val DEBUG = false
	
	val log = LoggerFactory.getLogger(this.getClass);
	

	protected CloseableHttpClient httpClient
	BasicCookieStore cookieStore

	RequestConfig config
	BasicCredentialsProvider credentialProvider
	
	public new() {
        this.cookieStore = new BasicCookieStore
        this.credentialProvider = new BasicCredentialsProvider
		this.httpClient = HttpClientBuilder.create.setDefaultCookieStore(cookieStore).
			 //setRedirectStrategy(new RedirectLogging(DefaultRedirectStrategy.INSTANCE)).
		 	setSslcontext(createSSLContext).setDefaultCredentialsProvider(credentialProvider).
		 	setRoutePlanner(new SystemDefaultRoutePlanner(null)).		 	
		 	build
	}
	
	def setProxy(String host, int port, String schema) {
		val builder = RequestConfig.custom
		builder.proxy = new HttpHost(host, port, schema)
		config = builder.build
	}
	
	def setAuthentication(String host, int port, String username, String password) {
        credentialProvider.setCredentials(new AuthScope(host, port),new UsernamePasswordCredentials(username, password))
	}
	
	def setAuthentication(NetworkConfiguration config) {
		if (config != null && config.hasProxy && config.hasProxyAuthentication) {
			setAuthentication(config.host, config.port, config.username, config.password)
		}
	}
	
	def clearCookies() {
		cookieStore.clear
	}
	
	def <X> X get(String url, (CloseableHttpResponse)=>X responseBuilder) {
		request(new HttpGet(url), responseBuilder)
	}

	def <X> X request(HttpUriRequest req, (CloseableHttpResponse)=>X responseBuilder) {
		request(req, responseBuilder, null)
	}
	
	private def setup(HttpUriRequest request) {
		if (config != null) {
			if (request instanceof HttpRequestBase) {
				request.config = config
			}
		}
	}

	def <X> X request(HttpUriRequest request, (CloseableHttpResponse)=>X responseBuilder, HttpContext ctx) {
		try {
			setup(request)
			log.debug("sending request to " + request.URI)
			val response = httpClient.execute(request, ctx)
			// if (DEBUG) println("current cookies: " + cookieStore.cookies)
			responseBuilder.apply(response)
		} finally {
			if (request instanceof AbstractExecutionAwareRequest) {
				request.reset
			}
		}
	}
	
	def hasCookieForDomain(String domain) {
		cookieStore.cookies.findFirst[ ck | ck.domain.endsWith(domain)] != null
	}
	
	def static JsonElement convertJson(HttpResponse response) {
		new JsonParser().parse(wrapReader(response.entity))
	}
	
	def static Document convertToDocument(HttpResponse response, String uri) {
		Jsoup.parse(response.entity.content, encoding(response.entity, "UTF-8"), uri)
	} 

	def static String convertToString(HttpResponse response) {
		EntityUtils.toString(response.entity)
	}
	
	def Document requestAsDocument(HttpUriRequest uriRequest) {
		request(uriRequest, [ response | convertToDocument(response, uriRequest.URI.toString) ])
	}

	def JsonElement requestAsJson(HttpUriRequest uriRequest) {
		request(uriRequest, [ response |convertJson(response)])
	}
	
	def requestAsString(HttpUriRequest uriRequest) {
		request(uriRequest, [ response |convertToString(response)])
	}

	def getAsDocument(String url) {
		requestAsDocument(new HttpGet(url))
	}
	
	def getAsString(String url) {
		requestAsString(new HttpGet(url))
	}

	def JsonElement getAsJson(String url) {
		requestAsJson(new HttpGet(url))
	}

	def <X> X post(HttpPost postMethod, (CloseableHttpResponse)=>X responseBuilder) {
		try {
			log.debug("posting request to " + postMethod.URI)
			val response = httpClient.execute(postMethod)
			// if (DEBUG) println("current cookies: " + cookieStore.cookies)
			responseBuilder.apply(response)
		} finally {
			postMethod.releaseConnection
		}
	}

	private static def Reader wrapReader(HttpEntity entity) {
		new InputStreamReader(entity.content, encoding(entity, "UTF-8"));
	}

	private static def String encoding(HttpEntity entity, String defEncoding) {
		val contentEncoding = entity.contentEncoding?.value
		if (contentEncoding === null) {
			val value = entity.contentType.value
			if (value !== null) {
				val index = value.toLowerCase.indexOf("charset=")
				if (index != -1) {
					return value.substring(index + "charset=".length)
				}
			}
			return defEncoding
		}
		return contentEncoding
	}
	
	static def createSSLContext() {
		val TrustManager[] trustAllCerts = #[new TrustAll]
		
		val sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        sc
	}


	public static def setupLogging() {
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
		// System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http","DEBUG");
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.headers","DEBUG"); 
		System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.impl.client","DEBUG"); 
	} 
	
}
