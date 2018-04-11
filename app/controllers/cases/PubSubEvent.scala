package controllers.cases

case class PubSubEventMessage(attributes: Map[String, String], data: String, message_id: String)
case class PubSubEvent(subscription: String, message: PubSubEventMessage)