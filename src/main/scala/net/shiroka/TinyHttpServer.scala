package net.shiroka
import java.net.InetSocketAddress
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}

object TinyHttpServer {
  def serve(port: Int): Unit = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext("/", new HttpHandler() {
      def handle(he: HttpExchange): Unit = sendResponse(he, "ok\n")
    })
    server.setExecutor(null)
    server.start()
    println("HTTP server started")
    //println("Hit any key to exit...")
    //System.in.read()
    //server.stop(0)
  }

  private def sendResponse(he: HttpExchange, response: String): Unit = {
    he.sendResponseHeaders(200, response.length)
    val os = he.getResponseBody
    try { os.write(response.getBytes) }
    finally { os.close() }
  }
}
