package hu.qualysoft.tefgin.tefclient

import hu.qualysoft.tefgin.base.HourMinute
import hu.qualysoft.tefgin.common.Utils
import java.time.LocalDate
import java.time.temporal.ChronoField

class CollectStats {

	static val DEBUG = false

	def static void main(String[] args) {

		if(DEBUG) BaseClient.setupLogging

		val c = new BaseClient
		c.authentication = Utils.getNetworkConfiguration
		val tef = new TEFClient(c)
		if (args.length >= 2) {
			val userName = args.get(0)
			val password = args.get(1)
			println('''logging in with '«userName»' ... ''')			
			if (tef.login(userName, password)) {

				val now = LocalDate.now()
				var cal = now.with(ChronoField.DAY_OF_MONTH, 1)

				var sum = new HourMinute(0)
				var workingDays = 0

				while (cal.isBefore(now)) {
					val booking = tef.getTimingsAt(cal)
					if (booking.getComingTime(0) != null) {
						val wk = booking.workingTime
						println('''at  «cal» arrival: «booking.getComingTime(0)» leave: «booking.getGoingTime(0)», pause: «booking.pauseTime», working hour:«wk»''')
						sum = sum + wk
						workingDays = workingDays + 1
					} else {
						println("at " + cal + " - no work")
					}
					cal = cal.plusDays(1)

				}

				println('''Worked together: «sum» which is equal to «sum.hour / 8» days «(sum.hour % 8)» hours «sum.minute» minutes''')
				val overtime = sum - new HourMinute((workingDays * 8 * 60))
				println("Current overtime for this month is: " + overtime)
			}
		} else {
			println("Please specify username and password to log in auto-partner.net!")
		}
	}

}
