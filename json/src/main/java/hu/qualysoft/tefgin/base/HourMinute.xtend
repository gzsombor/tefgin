package hu.qualysoft.tefgin.base

class HourMinute {
	val long time;
	
	new(long time) {
		this.time = time
	}
	
	def getHour() {
		time / 60
	}
	
	def getMinute() {
		time % 60
	}
	
	override toString() {
		val min = minute
		"" + hour + ':' + (if (min < 10) { '0' + minute } else  {minute })
	}
	
	def operator_plus(HourMinute other) {
		new HourMinute(time + other.time)
	}
	
	def operator_minus(HourMinute other) {
		new HourMinute(time - other.time)
	}

	static def valueOf(long n) {
		new HourMinute(n)
	}
	
	def getAllMinutes() {
		time
	}

	
	static def valueOf(Number n) {
		if (n != null) {
			new HourMinute(n.longValue)
		} else {
			null
		}
	}
}