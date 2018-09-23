package util

import cats.effect.IO


object IOUtil {
  def putStrLn(message: Any): IO[Unit] = IO {
    println(message)
  }

  def runWithClosable[S <: AutoCloseable, T](stream: => S)
                                            (action: S => IO[T]): IO[T] =
    IO(stream).bracket(action) { openStream =>
      IO {
        openStream.close()
      }
    }
}
