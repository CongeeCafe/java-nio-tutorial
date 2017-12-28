import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Server {
    
    public static void main(String args[]) throws IOException, InterruptedException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        // Bind to random open port
        channel.bind(new InetSocketAddress(0));
        System.out.println("Addr: " + channel.getLocalAddress());

        channel.configureBlocking(false);

        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_ACCEPT);

        ByteBuffer keyBuffer = ByteBuffer.allocate(512);
        HashMap<String, SocketChannel> keyMap = new HashMap<>();
        HashMap<String, SocketChannel> channelMap = new HashMap<>();
        
        while (true) {
            // Blocks until connection attempt is made
            selector.select();

            Set selectedKeys = selector.selectedKeys();
            Iterator it = selectedKeys.iterator();
            
            while (it.hasNext()) {
                SelectionKey key = (SelectionKey) it.next();

                if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                    SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ);
                }
                else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                    SocketChannel sc = (SocketChannel) key.channel();

                    // Read data
                    keyBuffer.clear();

                    boolean closed = false;

                    int read = 0;
                    int readTotal = 0;
                    do {
                        read = sc.read(keyBuffer);
                        readTotal += read;
                        if (read < 0) {
                            closed = true;
                            break;
                        }
                    } while (read > 0);

                    if (closed) {
                        if (channelMap.get(sc.getRemoteAddress().toString()) != null) {
                            channelMap.get(sc.getRemoteAddress().toString()).close();
                            channelMap.remove(sc.getRemoteAddress().toString());
                        }
                        sc.close();
                        break;
                    }

                    String receivedMessage = new String(keyBuffer.array(), 0, readTotal);
                    char receivedCommand = (receivedMessage.charAt(0));
                    String receivedKey = (receivedMessage.substring(1, receivedMessage.length()));

                    // Transfer in progress
                    if (channelMap.get(sc.getRemoteAddress().toString()) != null) {
                        SocketChannel scReceiver = channelMap.get(sc.getRemoteAddress().toString());

                        // Receive and forward file packets
                        keyBuffer.flip();
                        while (keyBuffer.hasRemaining()) {
                            scReceiver.write(keyBuffer);
                        }
                        keyBuffer.compact();
                    }
                    // Init client upload
                    else if (receivedCommand == 'P') {
                        keyMap.put(receivedKey, sc);
                    }
                    // Init client download
                    else if (receivedCommand == 'G') {
                        SocketChannel scSender = keyMap.get(receivedKey);
                        keyMap.remove(receivedKey);
                        if (scSender == null) {
                            System.out.println("No matching uploader found " + receivedKey);
                            break;
                        }
                        keyBuffer.flip();
                        channelMap.put(scSender.getRemoteAddress().toString(), sc);
                        scSender.write(keyBuffer);
                    }

                }

                it.remove();
            }
        }
    }
}
