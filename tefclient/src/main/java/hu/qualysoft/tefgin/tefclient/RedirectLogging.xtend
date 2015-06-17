package hu.qualysoft.tefgin.tefclient

import org.apache.http.client.RedirectStrategy
import org.apache.http.HttpRequest
import org.apache.http.HttpResponse
import org.apache.http.protocol.HttpContext
import org.apache.http.ProtocolException

class RedirectLogging implements RedirectStrategy {
	val RedirectStrategy delegate

	new(RedirectStrategy delegate) {
		this.delegate = delegate
	}	
	override getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
		val result = delegate.getRedirect(request, response, context)
		println("redirecting "+request.requestLine.uri + " to "+ result)
		result
	}
	
	override isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
		delegate.isRedirected(request, response, context)
	}
	
	 
}