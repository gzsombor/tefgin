package hu.qualysoft.tefgin.base.json

import de.itemis.jsonized.Jsonized
import hu.qualysoft.tefgin.base.HourMinute
import java.time.LocalDate
import org.eclipse.xtend.lib.annotations.Accessors

@Jsonized('{
  "dayTimes" : [
    {
      "pause" : 30,
      "travelTime" : "",
      "bookingDay" : 1390777200000,
      "tefEmployeeId" : 13002954,
      "times" : [
        {
          "id" : 14452051,
          "timeGoingMinute" : 1103,
          "timeComingMinute" : 573
        }
      ],
      "status" : 2,
      "holidayStatus" : "",
      "isEditable" : false,
      "attendanceTime" : 462,
      "travelTimeTransf" : ""
    }
  ]
}
')
class Booking implements Comparable<Booking> {
	@Accessors
	var LocalDate askedTime

	def getBookingDay() {
		val dt = daytimes
		if (!dt.empty) {
			dt.get(0).bookingday
		} else {
			null
		}
	}

	def getComingTime(int idx) {
		try {
			HourMinute.valueOf(getTimeOf(idx)?.timecomingminute)
		} catch (ClassCastException e) {
			// sometimes timegoingminute is not filled, 
			// and comes through as an empty string, which cant be casted as Long
			null
		}
	}

	def getGoingTime(int idx) {
		try {
			HourMinute.valueOf(getTimeOf(idx)?.timegoingminute)
		} catch (ClassCastException e) {
			// sometimes timegoingminute is not filled, 
			// and comes through as an empty string, which cant be casted as Long
			null
		}
	}

	def getAttendanceTime() {
		val dt = daytimes
		if (!dt.empty) {
			dt.get(0).attendancetime
		} else {
			null
		}
	}

	def getStatus() {
		val dt = daytimes
		if (!dt.empty) {
			dt.get(0).status
		} else {
			null
		}
	}

	def getHolidayStatus() {
		val dt = daytimes
		if (!dt.empty) {
			dt.get(0).holidaystatus
		} else {
			null
		}
	}

	def getPauseTime() {
		val dt = daytimes
		if (dt != null && !dt.empty) {
			try {
				val p = dt.get(0).pause
				HourMinute.valueOf(p)
			} catch (ClassCastException e) {

				// sometimes empty String 
				null
			}
		} else {
			null
		}
	}

	def getWorkingTime() {
		val _goingTime = getGoingTime(0)
		val _pauseTime = pauseTime
		if (_goingTime != null) {
			val bruttoTime = _goingTime - getComingTime(0)
			if (_pauseTime != null) {
				bruttoTime - _pauseTime 
			} else {
				bruttoTime
			}
		} else {
			new HourMinute(0)
		}
	}
	
	def hasMultipleTimeRanges() {
		val dt = daytimes
		dt != null && !dt.empty && dt.get(0).times.size > 1
	}
	
	def getTimeSize() {
		val dt = daytimes
		if (dt != null && !dt.empty) { dt.get(0).times.size } else { 0 }
	}

	private def Times getTimeOf(int idx) {
		val dt = daytimes
		if (dt != null && !dt.empty && idx < dt.get(0).times.size) {
			val times = dt.get(0).times
			if (!times.empty) {
				return times.get(idx)
			}
		}
		null
	}

	override toString() {
		"Booking(coming:" + getComingTime(0) + ",going:" + getGoingTime(0) + ",attendance:" + attendanceTime + ",status:" + status +
			",holiday:" + holidayStatus + ",pause:" + pauseTime + ")"
	}

	override compareTo(Booking o) {
		val bd = askedTime
		if (bd == null) {
			-1
		} else {
			val obd = o.askedTime
			if (obd == null) {
				1
			} else {
				bd.compareTo(obd);
			}
		}
	}

}
