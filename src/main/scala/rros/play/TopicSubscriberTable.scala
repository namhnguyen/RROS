package rros.play

/**
 * TopicSubscriberTable stores topic -> List[actorRef] which each Actor referenced by actorRef
 * manages each websocket connection
 *
 * Created by namnguyen on 4/5/15.
 */
trait TopicSubscriberTable {
  def subscribe(topic:String,actorPath:String):Boolean
  def unsubscribe(topic:String,actorPath:String):Boolean
  def getActorPaths(topic:String):Set[String]
  def removeTopic(topic:String):Boolean
}

case class TopicSubscriberTableImpl() extends TopicSubscriberTable{
  private val map =
    scala.collection.mutable.HashMap[String,scala.collection.mutable.HashSet[String]]()

  override def subscribe(topic: String, actorPath: String): Boolean = this.synchronized {
    if (!map.contains(topic)){
      map += ((topic,scala.collection.mutable.HashSet[String]()))
    }
    if (!map.get(topic).get.contains(actorPath)) {
      map.get(topic).get += actorPath
      true
    } else false
  }

  override def unsubscribe(topic: String, actorPath: String): Boolean = this.synchronized {
    if(map.contains(topic)){
      if (map.get(topic).get.contains(actorPath)) {
        map.get(topic).get -= actorPath
        true
      } else false
    } else {
      false
    }
  }

  override def removeTopic(topic: String): Boolean = this.synchronized {
    if (map.contains(topic)) {
      map -= topic
      true
    } else false
  }

  override def getActorPaths(topic: String): Set[String] = {
    if (map.contains(topic)) map.get(topic).get.toSet
    else Set()
  }
}
