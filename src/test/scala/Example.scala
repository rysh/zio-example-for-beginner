import org.scalatest.FunSpec

class Example extends FunSpec {

  import zio.{DefaultRuntime, ZIO}
  import scala.concurrent.Future
  import scala.util.Try

  it("基本的な使い方") {

    // for式で利用する。programもZIO型
    val program = for {
      // 外部ライブラリや既存のコードからFuture,Either,Tryなどの値を受け取ったとする
      futureResult       <- ZIO.fromFuture(implicit ec => /* call */ Future("1"))
      eitherResult       <- ZIO.fromEither( /* call */ Right("2"))
      tryResult          <- ZIO.fromTry( /* call */ Try("3"))
      futureEitherResult <- ZIO.fromFuture(implicit ec => /* call */ Future(Right("4"))).flatMap(ZIO.fromEither(_))
    } yield s"$futureResult-$eitherResult-$tryResult-$futureEitherResult"

    // 最後にRuntimeから実行する
    val runtime = new DefaultRuntime {}
    val result  = runtime.unsafeRun(program)

    assert(result === "1-2-3-4")
  }

  it("並行処理") {

    val program: ZIO[Any, Throwable, String] = for {
      ((futureResult, eitherResult), tryResult) <- ZIO
        // 外部ライブラリや既存のコードからFuture,Either,Tryなどの値を受け取ったとする
        .fromFuture(implicit ec => /* call */ Future("1"))
        // zipParで並行処理
        .zipPar(ZIO.fromEither( /* call */ Right("2")))
        .zipPar(ZIO.fromTry( /* call */ Try("3")))
    } yield s"$futureResult-$eitherResult-$tryResult"

    val runtime = new DefaultRuntime {}
    val result  = runtime.unsafeRun(program)

    assert(result === "1-2-3")
  }

  it("FutureやTryを使わず直接ZIOを使う") {

    val program = for {
      (futureResult, tryResult) <- ZIO { "1" }
        .zipPar(ZIO { (2 / 0).toString }.catchAll(_ => ZIO.succeed("2")))
    } yield s"$futureResult-$tryResult"

    // 最後にRuntimeから実行する
    val runtime = new DefaultRuntime {}
    val result  = runtime.unsafeRun(program)

    assert(result === "1-2")
  }
}
