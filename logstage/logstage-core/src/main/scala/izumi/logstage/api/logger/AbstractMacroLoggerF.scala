package izumi.logstage.api.logger

import izumi.logstage.macros.LogIOMacroMethods._
import logstage.AbstractLogIO

import scala.language.experimental.macros

trait AbstractMacroLoggerF[F[_]] { this: AbstractLogIO[F] =>

  /** Aliases for [[logstage.AbstractLogIO#log]] that look better in Intellij */
  final def trace(message: String): F[Unit] = macro scTraceMacro[F]
  final def debug(message: String): F[Unit] = macro scDebugMacro[F]
  final def info(message: String): F[Unit] = macro scInfoMacro[F]
  final def warn(message: String): F[Unit] = macro scWarnMacro[F]
  final def error(message: String): F[Unit] = macro scErrorMacro[F]
  final def crit(message: String): F[Unit] = macro scCritMacro[F]
}
