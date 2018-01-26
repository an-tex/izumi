package com.github.pshirshov.izumi.logstage.model

object Log {
  sealed trait Level extends Ordered[Level] {
    protected def asInt: Int
    override def compare(that: Level): Int = this.asInt - that.asInt
  }

  object Level {
    case object Debug extends Level {
      protected val asInt = 10
    }
    case object Info extends Level {
        protected val asInt = 20
    }
    case object Warn extends Level {
      protected val asInt = 30
    }
    case object Error extends Level {
      protected val asInt = 40
    }
  }


  trait CustomContext {
    def values: Map[String, Any]
  }

  case object EmptyCustomContext extends CustomContext {
    override def values: Map[String, Any] = Map.empty
  }


  case class StaticContext(id: String) extends AnyVal
  case class StaticExtendedContext(id: StaticContext, file: String, line: Int)
  case class ThreadData(threadName: String, threadId: Long)
  case class DynamicContext(level: Level, threadData: ThreadData)
  case class Context(static: StaticExtendedContext, dynamic: DynamicContext, customContext: CustomContext)


  case class NamedArgument(name : String, value : Any)

  case class Entry(message: Message, context: Context)


  case class Message(template: StringContext, args: List[(String, Any)]) {

    import com.github.pshirshov.izumi.fundamentals.collections.SeqEx._

    def argsMap: Map[String, Set[Any]] = args.toMultimap
  }

}


