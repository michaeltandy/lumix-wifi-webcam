
package hack;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.Set;

public class Main {
    
    public static void main(String[] args) throws IOException {
        String cameraIp = "192.168.0.6";
        if (args.length > 0)
            cameraIp = args[0];
        
        Selector selector = Selector.open();
        
        DatagramChannel datagramChannel = DatagramChannel.open();
        DatagramSocket datagramSocket = datagramChannel.socket();
        datagramSocket.bind(new InetSocketAddress(49199));
        datagramSocket.setReceiveBufferSize(15*35000);
        datagramChannel.configureBlocking(false);
        datagramChannel.register(selector, SelectionKey.OP_READ);
        
        int bufferSize = 50000;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        
        final BlockingQueue<RawJpegBytes> rawJpegQueue = new SynchronousQueue<>();
        
        final BasicLumixControl blc = new BasicLumixControl(cameraIp);
        blc.start();
        
        final FfmpegOut ffmpeg = new FfmpegOut(rawJpegQueue);
        ffmpeg.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                blc.stop();
                ffmpeg.stop();
                System.out.println("Stopped.");
            }
        });
        
        long startTime = System.currentTimeMillis();
        while (true) {
            while (selector.select(1000) == 0 && selector.selectedKeys().isEmpty()) {
                System.out.println("Select Timeout at "+ System.currentTimeMillis());
            }
            
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            for (SelectionKey sk : readyKeys) {
                if (sk.isReadable()) {
                    DatagramChannel dc = (DatagramChannel)sk.channel();
                    dc.receive(buffer);
                }
            }
            readyKeys.clear();
            
            buffer.flip();
            RawJpegBytes jpeg = findJpeg(buffer);
            buffer.clear();

            if (jpeg != null) {
                rawJpegQueue.offer(jpeg);
            }
        }
    }
    
    public static RawJpegBytes findJpeg(ByteBuffer udp) {
        int len = udp.remaining();
        if (len < 1000) {
            System.out.println("Weird short packet??");
            return null;
        } else if ((udp.getShort(len-2)&0xFFFF) != 0xFFD9) {
            System.out.println("Trouble spotting EOI marker");
            return null;
        } else {
            int jpegStartIdx = 32+(udp.getShort(30)&0xFFFF);
            if ((udp.getShort(jpegStartIdx)&0xFFFF) != 0xFFD8) {
                System.out.println("Trouble spotting SOI marker? Expected it at "+jpegStartIdx);
                return null;
            } else {
                byte[] jpegBytes = new byte[len-jpegStartIdx];
                udp.position(jpegStartIdx);
                udp.get(jpegBytes);
                return new RawJpegBytes(jpegBytes);
            }
        }
    }
}
