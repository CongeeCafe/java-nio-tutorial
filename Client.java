import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client {
    /**
     * Usage: download: client <host> <port> G <key> <file name>
     *        upload: client <host> <port> P <key> <file name>
     */
    public static void main(String args[]) throws Exception {
        // Get command line data
        String  host = args[0];
        int     port = Integer.parseInt(args[1]);
        char    mode = args[2].charAt(0);
        String  key = args[3];
        String  filename = args[4];

        FileOutputStream out = null;
        FileChannel file = null;

        if (mode == 'G') {
            out = new FileOutputStream(filename);
            file = out.getChannel();
        }

        // Connect to server
        SocketChannel channel = SocketChannel.open();
        channel.connect(new InetSocketAddress(host, port));

        // Send key value
        String sendData = key + command;
        ByteBuffer buffer = ByteBuffer.wrap(sendData.getBytes(StandardCharsets.US_ASCII));
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }

        ByteBuffer responseBuffer = ByteBuffer.allocate(512);

        // Read will block until message arrives on channel
        while (channel.read(responseBuffer) >= 0 || responseBuffer.position() != 0) {
            // Download data
            if (mode == 'G') {
                responseBuffer.flip();

                while (responseBuffer.hasRemaining()) {
                    file.write(responseBuffer);
                }

                responseBuffer.compact();
            }
            // Upload data
            else if (mode == 'P') {
                Path fileLocation = Paths.get("./" + filename);
                FileChannel fileChannel = FileChannel.open(fileLocation);
                ByteBuffer dataBuffer = ByteBuffer.allocate(512);

                while (fileChannel.read(dataBuffer) > 0) {
                    dataBuffer.flip();

                    while (dataBuffer.hasRemaining()) {
                        channel.write(dataBuffer);
                    }

                    dataBuffer.compact();
                }
                break;
            }
        }

        if (mode == 'G') {
            file.close();
            out.close();
        }

        channel.close();
    }
}
