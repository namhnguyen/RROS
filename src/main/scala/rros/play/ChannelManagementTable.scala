package rros.play

/**
 * Created by namnguyen on 3/11/15.
 */
trait ChannelManagementTable {
  def register(endPointId:String,actorPath:String):Unit
  def get(endPointId:String):Option[String]

  /**
   * Only unregister if same endPointId and same actorPath
   * @param endPointId
   * @param actorPath
   */
  def unregisterIfExists(endPointId:String,actorPath:String):Unit
  def clearAll():Unit
}

class ChannelManagementTableImpl extends ChannelManagementTable{
  private val map =
    scala.collection.mutable.HashMap[String,String]()

  override def register(endPointId: String, actorPath: String): Unit = this.synchronized {
    map += ((endPointId, actorPath))
  }

  override def get(endPointId: String): Option[String] = this.synchronized {
    map.get(endPointId)
  }


  override def unregisterIfExists(endPointId: String,actorPath:String): Unit =
    this.synchronized {
      val somePath = map.get(endPointId)
      if (somePath.isDefined && somePath.get==actorPath)
        map -= endPointId
    }

  override def clearAll(): Unit = this.synchronized { map.clear() }
}