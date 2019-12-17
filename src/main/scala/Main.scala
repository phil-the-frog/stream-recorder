import java.io.File

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers
import akka.stream.Materializer
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration.Duration
import scala.io.StdIn
import scala.sys.process._


object Main {

  val config = ConfigFactory.load()                             // load config from application.conf from resources
  val saveDir = config.getString("dir")                   // the dir where to save the files
  def getTime() = DateTime.now.toIsoDateTimeString()    // temporarily name the file to the time

  def record_stream()={
    recording = true                // set the server state to true
    file_name = getTime()+".mp4"    // set the temp name of the file
    // record the file with streamlink
    process = Process(Seq("streamlink", "-o", file_name, config.getString("stream"), "best"),new File(saveDir)).run
  }

  def stop_recording()={
    recording = false               // change the state of the server
    try{
      process.destroy()             // stop streamlink
    }
    catch {
      case exception: Exception => println(exception.getMessage)
    }
    process = null                  // null process
  }

  def rename(name: String)={
    println(name+".mp4")
    println(file_name)
    val rename_cmd = Process(Seq("mv", file_name, name+".mp4"),new File(saveDir)).!!    // rename file_name to name
    file_name = ""               // reset file_name
  }

  // global variables to store info about the file we are recording to
  var recording = false         // state info about server
  var process:Process = null    // streamlink process
  var file_name = ""            // temp name of the file we just recorded
  def main(args: Array[String]): Unit = {

    // init Akka actor system and execution context
    implicit val system = ActorSystem("my-system")
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    // main route
    val route =
        get {                     // everything is get to make it easy
          concat(
          path("hello") {
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
          },
          path("pwd") {
            val p = Process(Seq("pwd"), new File(saveDir)).!!
            complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>$p</h1>"))
          },
          path("record") {    // start a recording
            if (!recording) {
              record_stream()
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Recording...</h1>"))
            }
            else
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Already Recording...</h1>"))
          },
          path("stop") {    // stop the current recording
            if (recording) {
              stop_recording()
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Stopped...</h1>"))
            }
            else
              complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Already Stopped...</h1>"))
          },
          path("rename") {  // change the name of the file we just recorded
            parameter("name".as[String]) { (name) =>
              if (!file_name.equals("")) {
                rename(name)
                complete((StatusCodes.Accepted, s"Name changed to $name"))
              }
              else
                complete((StatusCodes.Accepted, "Name change failed"))
            }
          }
          )
        }

    println(s"Server online at http://odroidxu4:8080/\nPress CTRL-C to stop...")
    // start the server
    val future = for { bindingFuture <- Http().bindAndHandle(route, "0.0.0.0", 8080)
                  waitOnFuture  <- Future.never}
      yield (waitOnFuture,bindingFuture)
    sys.addShutdownHook {                   // terminate any process that's running
      if(recording)
        process.destroy()
    }
    Await.ready(future, Duration.Inf)       // wait forever

    future
      .flatMap(_._2.unbind())               // trigger unbinding from the port
      .onComplete(_ => system.terminate())  // and shutdown when done
  }
}
