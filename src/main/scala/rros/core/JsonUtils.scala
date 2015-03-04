package rros.core

import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization._
////////////////////////////////////////////////////////////////////////////////
/**
 * Created by namnguyen on 3/4/15.
 */
object JsonUtils {
  implicit val format = org.json4s.DefaultFormats
  def serialize(any:AnyRef):String = write(any)
  def deserialize(input:String) = parse(input)
}
////////////////////////////////////////////////////////////////////////////////