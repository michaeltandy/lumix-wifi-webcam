
package hack;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.Scanner;

public class BasicLumixControl {
    private final String cameraIP;
    
    private Thread keepAlive = null;
    private boolean keepAliveActive = false;
    
    public BasicLumixControl(String cameraIP) {
        this.cameraIP = cameraIP;
    }
    
    public void start() {
        getUrlContents("http://" + cameraIP + "/cam.cgi?mode=accctrl&type=req_acc&value=0&value2=lumix-wifi-webcam");
        getUrlContents("http://" + cameraIP + "/cam.cgi?mode=camcmd&value=recmode");
        getUrlContents("http://" + cameraIP + "/cam.cgi?mode=startstream&value=49199&value2=10");
        keepAliveActive = true;
        keepAlive = new Thread(new KeepAliveRunnable(), "HTTP message thread");
        keepAlive.setDaemon(false);
        keepAlive.start();
    }
    
    public void stop() {
        keepAliveActive = false;
        if (keepAlive != null) {
            keepAlive.interrupt();
        }
        getUrlContents("http://" + cameraIP + "/cam.cgi?mode=stopstream");
    }
    
    private class KeepAliveRunnable implements Runnable {
        @Override
        public void run() {
            try {
                int counter = 0;
                while (keepAliveActive) {
                    counter++;
                    getUrlContents("http://" + cameraIP + "/cam.cgi?mode=getstate");
                    if (counter % 5 == 0) {
                        getUrlContents("http://" + cameraIP + "/cam.cgi?mode=camctrl&type=touch&value=500/500&value2=on");
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // Normal - shutting down.
            }
        }
    }
    
    private String getUrlContents(String url) {
        try {
            URLConnection uc = new URL(url).openConnection();
            InputStream is = uc.getInputStream();
            Scanner s = new Scanner(is).useDelimiter("//A");
            String result = s.hasNext() ? s.next() : "";
            is.close();
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    /*public static void main(String[] args) {
        BasicLumixControl blc = new BasicLumixControl("192.168.0.6");
        blc.start();
        try { Thread.sleep(60*1000); } catch (InterruptedException e) {}
        blc.stop();
    }*/

}
