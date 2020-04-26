
package hack;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;


public class FfmpegOut {
    private final BlockingQueue<RawJpegBytes> rawJpegQueue;
    private Process process = null;
    
    private Thread sendThread = null;
    private boolean isStopped = true;
    
    public FfmpegOut(BlockingQueue<RawJpegBytes> rawJpegQueue) {
        this.rawJpegQueue = rawJpegQueue;
        
    }
    
    public void start() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        
        processBuilder.command("ffmpeg","-f","mjpeg","-r","30","-i","pipe:","-f","v4l2","-pix_fmt","yuv420p","/dev/video10");
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        this.process = processBuilder.start();
        
        this.isStopped = false;
        this.sendThread = new Thread(new ForwardRunnable(), "Forward to ffmpeg");
        this.sendThread.setDaemon(true);
        this.sendThread.start();
    }
    
    public void stop() {
        this.isStopped = true;
        if (sendThread != null) {
            sendThread.interrupt();
            try { sendThread.join(100); } catch (InterruptedException e) {}
        }
        if (process != null)
            process.destroy();
    }
    
    private class ForwardRunnable implements Runnable {
        @Override
        public void run() {
            try (OutputStream os = process.getOutputStream()) {
                while (!isStopped) {
                    RawJpegBytes jpeg = rawJpegQueue.take();
                    os.write(jpeg.bytes);
                    os.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // Normal - shutting down.
            }
        }
    }

}
