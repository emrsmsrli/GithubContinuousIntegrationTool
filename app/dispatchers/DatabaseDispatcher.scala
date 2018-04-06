package dispatchers

import akka.actor.ActorSystem
import javax.inject.{Inject, Singleton}
import play.api.libs.concurrent.CustomExecutionContext

@Singleton
class DatabaseDispatcher @Inject()(actorSystem: ActorSystem)
    extends CustomExecutionContext(actorSystem, "database.dispatcher")