
package hack;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamHttpServer {
    
    private final HttpServer server;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final BlockingQueue<RawJpegBytes> rawJpegQueue;
    
    public StreamHttpServer(BlockingQueue<RawJpegBytes> rawJpegQueue) {
        this.rawJpegQueue = rawJpegQueue;
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 8080),10);
            server.setExecutor(executor);
            server.createContext("/", new HandleRootDirectory());
            server.createContext("/mjpeg", new HandleMJpeg());
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public void stopServer() {
        executor.shutdownNow();
        server.stop(1);
    }
    
    private class HandleRootDirectory implements HttpHandler{
        public void handle(HttpExchange httpexcg) throws IOException {
            Headers headersToSend = httpexcg.getResponseHeaders();
            headersToSend.add("Content-Type", "text/html");
            headersToSend.add("Accept-Ranges", "none");
            headersToSend.add("Cache-Control", "no-cache");
            httpexcg.sendResponseHeaders(200, 0);
            OutputStream os = httpexcg.getResponseBody();
            
            String response = "<html><head><title>Test HTTP Server</title>" +
                   "</head><body>\n" +
                   "<h1>Hello, World!</h1>\n" +
                   "<a href=\"/mjpeg\">MJPEG stream</a>\n" +
                   "</body></html>\n";
            
            os.write(response.getBytes(StandardCharsets.ISO_8859_1));
            os.flush();
            os.close();
        }
    }
    
    private class HandleMJpeg implements HttpHandler{
        public void handle(HttpExchange httpexcg) throws IOException {
            Headers headersToSend = httpexcg.getResponseHeaders();
            headersToSend.add("Content-Type", "multipart/x-mixed-replace; boundary=myboundary");
            headersToSend.add("Accept-Ranges", "none");
            headersToSend.add("Cache-Control", "no-cache");
            httpexcg.sendResponseHeaders(200, 0);
            OutputStream os = httpexcg.getResponseBody();
            
            try {

                while (!Thread.interrupted()) {
                    byte[] nextImg = rawJpegQueue.take().bytes;
                    
                    String header =
                        "--myboundary\r\n" +
                        "Content-Type:image/jpeg\r\n" +
                        "Content-Length:" + nextImg.length + "\r\n\r\n";

                    os.write(header.getBytes(StandardCharsets.ISO_8859_1));
                    os.write(nextImg);
                    os.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
                    os.flush();

                }
            } catch (InterruptedException e) {
            } finally {
                try { os.close(); } catch (IOException e) {}
            }
        }
    }
};
