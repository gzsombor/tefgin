package hu.qualysoft.tefgin.base.json

import com.google.gson.JsonArray
import com.google.gson.JsonElement

class PositionList {
	private JsonArray array;
	
	new(JsonElement elem) {
		if (elem instanceof JsonArray) {
			array = elem
		} else {
			array = new JsonArray
			array.add(elem)
		}
	}
	
	def size() {
		array.size
	}
	
	def get(int index) {
		new PositionListItem(array.get(index))
	}
	
}