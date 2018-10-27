package com.github.pshirshov.izumi.functional.bio

import scalaz.zio.{ExitResult, IO, Schedule}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.higherKinds

trait BIO[R[+ _, + _]] extends BIOInvariant[R] {
  override type Or[+E, +V] = R[E, V]
  override type Just[+V] = R[Nothing, V]

  @inline override def map[E, A, B](r: R[E, A])(f: A => B): R[E, B]

  @inline override def point[V](v: => V): R[Nothing, V]

  @inline override def fail[E](v: => E): R[E, Nothing]

  @inline override def terminate(v: => Throwable): R[Nothing, Nothing]

  @inline override def redeem[E, A, E2, B](r: R[E, A])(err: E => R[E2, B], succ: A => R[E2, B]): R[E2, B]

  @inline override def maybe[V](v: => Either[Throwable, V]): R[Nothing, V] = {
    v match {
      case Left(f) =>
        terminate(f).asInstanceOf[R[Nothing, V]]
      case Right(r) =>
        point(r)
    }
  }

  @inline override def now[A](a: A): R[Nothing, A]

  @inline override def syncThrowable[A](effect: => A): R[Throwable, A]

  @inline override def sync[A](effect: => A): R[Nothing, A]

  @inline override def flatMap[E, A, E1 >: E, B](r: R[E, A])(f0: A => R[E1, B]): R[E1, B]

  override final val unit: R[Nothing, Unit] = now(())

  @inline override def void[E, A](r: R[E, A]): R[E, Unit]

  @inline override def leftMap[E, A, E2](r: R[E, A])(f: E => E2): R[E2, A]

  @inline override def bimap[E, A, E2, B](r: R[E, A])(f: E => E2, g: A => B): R[E2, B]

  @inline override def fromEither[E, V](v: => Either[E, V]): R[E, V]

  @inline override def bracket[E, A, B](acquire: R[E, A])(release: A => R[Nothing, Unit])(use: A => R[E, B]): R[E, B]

  @inline override def sandboxWith[E, A, E2, B](r: R[E, A])(f: R[Either[List[Throwable], E], A] => R[Either[List[Throwable], E2], B]): R[E2, B]
}


object BIO extends BIOSyntax {

  def apply[R[+ _, + _] : BIO]: BIO[R] = implicitly

  implicit object BIOZio extends BIOAsync[IO] {
    @inline override def bracket[E, A, B](acquire: IO[E, A])(release: A => IO[Nothing, Unit])(use: A => IO[E, B]): IO[E, B] =
      IO.bracket(acquire)(v => release(v))(use)

    @inline override def sleep(duration: Duration): IO[Nothing, Unit] = IO.sleep(duration)

    @inline override def sync[A](effect: => A): IO[Nothing, A] = IO.sync(effect)

    @inline override def now[A](a: A): IO[Nothing, A] = IO.now(a)

    @inline override def syncThrowable[A](effect: => A): IO[Throwable, A] = IO.syncThrowable(effect)

    @inline override def fromEither[L, R](v: => Either[L, R]): IO[L, R] = IO.fromEither(v)

    @inline override def point[R](v: => R): IO[Nothing, R] = IO.point(v)

    @inline override def void[E, A](r: IO[E, A]): IO[E, Unit] = r.void

    @inline override def terminate(v: => Throwable): IO[Nothing, Nothing] = IO.terminate(v)

    @inline override def fail[E](v: => E): IO[E, Nothing] = IO.fail(v)

    @inline override def map[E, A, B](r: IO[E, A])(f: A => B): IO[E, B] = r.map(f)

    @inline override def leftMap[E, A, E2](r: IO[E, A])(f: E => E2): IO[E2, A] = r.leftMap(f)

    @inline override def bimap[E, A, E2, B](r: IO[E, A])(f: E => E2, g: A => B): IO[E2, B] = r.bimap(f, g)

    @inline override def flatMap[E, A, E1 >: E, B](r: IO[E, A])(f0: A => IO[E1, B]): IO[E1, B] = r.flatMap(f0)

    @inline override def redeem[E, A, E2, B](r: IO[E, A])(err: E => IO[E2, B], succ: A => IO[E2, B]): IO[E2, B] = r.redeem(err, succ)

    @inline override def sandboxWith[E, A, E2, B](r: IO[E, A])(f: IO[Either[List[Throwable], E], A] => IO[Either[List[Throwable], E2], B]): IO[E2, B] =
      r.sandboxWith(f)

    @inline override def retryOrElse[A, E, A2 >: A, E2](r: IO[E, A])(duration: FiniteDuration, orElse: => IO[E2, A2]): IO[E2, A2] =
      r.retryOrElse[A2, Any, Duration, E2](Schedule.duration(duration), {
        (_: Any, _: Any) =>
          orElse
      })

    @inline override def widen[E, A, E1 >: E, A1 >: A](r: IO[E, A]): IO[E1, A1] = r

    @inline override def async[E, A](register: (Either[E, A] => Unit) => Unit): IO[E, A] = {
      IO.async[E, A] {
        cb =>
          register {
            case Right(v) =>
              cb(ExitResult.Completed(v))
            case Left(t) =>
              cb(ExitResult.Failed(t))
          }

      }
    }

    override def timeout[E, A](r: IO[E, A])(duration: Duration): IO[E, Option[A]] = {
      r.timeout(Option.empty[A])(Some(_))(duration)
    }

    override def race[E, A](r1: IO[E, A])(r2: IO[E, A]): IO[E, A] = {
      r1.race(r2)
    }
  }

}





