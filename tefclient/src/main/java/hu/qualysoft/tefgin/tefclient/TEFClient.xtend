package hu.qualysoft.tefgin.tefclient

import hu.qualysoft.tefgin.base.json.Booking
import hu.qualysoft.tefgin.common.Utils
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset
import org.apache.http.Consts
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.message.BasicNameValuePair
import org.slf4j.LoggerFactory
import hu.qualysoft.tefgin.base.json.PositionList

class TEFClient {
	val log = LoggerFactory.getLogger(this.getClass);

	private BaseClient client

	new(BaseClient client) {
		this.client = client
	}

	val Domain = "www.auto-partner.net"
	val SecureHost = "https://" + Domain
	val Host = "http://" + Domain
	
	val TefHost = Utils.getTefUrl()

	var String sessionId = ""

	def login(String name, String password) {
		val postUrl = loginPostUrl
		sessionId = getJSessionId(postUrl)

		val builder = RequestBuilder.post
		builder.uri = SecureHost + postUrl
		builder.entity = new UrlEncodedFormEntity(
			#{new BasicNameValuePair("j_username", name), 
				new BasicNameValuePair("j_password", password)
			},
			Consts.UTF_8
		)
		val request = builder.build

		val document = client.request(request,
			[ response |
				val newLocation = response.getFirstHeader("Location")?.value
				if (newLocation != null) {
					sessionId = getJSessionId(newLocation)
				}
				BaseClient.convertToDocument(response, request.URI.toString)
			])

		val loginForm = document.getElementById("loginForm")
		if (loginForm != null) {
			println("LOGIN FAILED (login form still present : " + loginForm + ")")
			false
		} else {
			println("OK, LOGIN SUCCEEDED ")
			openMenuPage
			redirectToTef
			if (client.hasCookieForDomain(TefDomain)) {
				true
			} else {
				false
			}
		}
	}
	
	def TefDomain() {
		new URL(TefHost).host
	}

	def getJSessionId(String url) {
		val idx = url.indexOf(";jsessionid=")
		if (idx != -1) {
			val end = url.indexOf('?', idx)
			if (end != -1) {
				url.substring(idx, end)
			} else {
				url.substring(idx)
			}
		}
	}

	def loginPostUrl() {
		val document = client.getAsDocument(SecureHost + "/portal/at?PnSec=false")
		val form = document.getElementById("form")
		form.attr("action")
	}

	def openMenuPage() {
		val request = RequestBuilder.get.setUri(Host + "/portal/at/menu" + sessionId).setHeader("Referer",
			Host + "/portal/at" + sessionId + "?PnSec=false").build
		val document = client.requestAsString(request)

		//println("document : " + document)
	}

	def redirectToTef() {
		val request = RequestBuilder.get.setUri(Host + "/portal/at/thirdparty" + sessionId).addParameter("cmd", "1589").
			addParameter("companyId", "90008").setHeader("Referer", Host + "/portal/at/menu" + sessionId).build
		val document = client.requestAsString(request)

		//println("document : " + document)
	}

	def allPage() {
		val current = client.getAsString(TefHost + "/web/recording/recording")
		//println("page:" + current)
	}

	def getTimingsAt(LocalDate date) {
		val time = toTefTimeFormat(date)
		val request = RequestBuilder.get.setUri(TefHost + "/web/recording/recording:timelist/"+time +"/1").addParameter("randomId", Math.random.toString).
			addHeader("X-Requested-With","XMLHttpRequest").build
		log.info("request to {}", request.URI)
		val json = client.requestAsJson(request)
		try {
			log.info("JSON at {} is {}, type is: {}", date ,json, (if (json != null) {json.getClass} else{ null}))
			val b = new Booking(json)
			b.askedTime = date
			b
		} catch (Exception e) {
			log.warn("Unable to parse :{}", json, e)
			throw e
		}
	}
	
	private def toTefTimeFormat(LocalDate date) {
		date.atTime(12, 0, 0).toInstant(ZoneOffset.UTC).epochSecond * 1000L
	}
	
	def getPositionListAt(LocalDate date) {
		val time = toTefTimeFormat(date)
		val request = RequestBuilder.get.setUri(TefHost + "/web/recording/recording:positionList/"+time +"/1").addParameter("randomId", Math.random.toString).
			addHeader("X-Requested-With","XMLHttpRequest").build
		log.info("request to {}", request.URI)
		val json = client.requestAsJson(request)
		try {
			log.info("JSON at {} is {}, type is: {}", date ,json, (if (json != null) {json.getClass} else{ null}))
			
			val b = new PositionList(json)
			b
		} catch (Exception e) {
			log.warn("Unable to parse :{}", json, e)
			throw e
		}
		
		// https://tef.porscheinformatik.com/tef5/web/recording/recording:positionList/1430863200000/1
	}
	
	def logout() {
		client.clearCookies
		sessionId = null
	}
	
	def isLoggedIn() {
		sessionId != null && sessionId.trim.length > 0
	}
}
