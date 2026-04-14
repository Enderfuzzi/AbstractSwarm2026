/*
AbstractSwarm agent interface for Scala
Copyright (C) 2021  Lars Hadidi (lahadidi@uni-mainz.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


import java.util.HashMap
import java.util.List

object SmartAgent {
  def evaluate(me: Agent, others: HashMap[Agent, Any], stations: List[Station], time: Long, station: Station): Double = {
    println("EVALUATE")
    return 0
  }

  def communicate(me: Agent, others: HashMap[Agent, Any], stations: List[Station], time: Long, defaultData: Array[Any]): Any = {
    println("COMMUNICATE")
    return null
  }

  def reward(me: Agent, others: HashMap[Agent, Any], stations: List[Station], time: Long, value: Double): Unit = {
    println("REWARD")
  }
}
