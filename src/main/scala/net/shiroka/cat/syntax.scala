package net.shiroka.cat
import scala.concurrent.{ Future, ExecutionContext }

package object syntax {
  implicit class Tap[T](self: T) {
    def tap[U](f: T => U): T = { f(self); self }
  }

  implicit class TapFailure[T](self: Future[T]) {
    def tapFailure[U](f: Throwable => U)(implicit ec: ExecutionContext) =
      self.recover { case err: Throwable => { f(err); err } }
  }
}
