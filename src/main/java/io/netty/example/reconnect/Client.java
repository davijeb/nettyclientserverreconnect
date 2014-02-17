package io.netty.example.reconnect;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Client {

    private final String host;
    private final int port;

    // Sleep 5 seconds before a reconnection attempt.
    static final int RECONNECT_DELAY = 1;
    // Reconnect when the server sends nothing for 10 seconds.
    private static final int READ_TIMEOUT = 1;

    private Channel ch;

    private ClientInitializer handler;

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
        handler = new ClientInitializer(this);
    }

    private Bootstrap configureBootstrap(Bootstrap b) {
        return configureBootstrap(b, new NioEventLoopGroup());
    }

    Bootstrap configureBootstrap(Bootstrap b, EventLoopGroup g) {

        handler = new ClientInitializer(this);

        b.group(g)
                .channel(NioSocketChannel.class)
                .remoteAddress(host, port)
                .handler(handler);

        return b;
    }

    public void run() throws Exception {

        if(ch !=null)
            ch.closeFuture().sync();

        try {
            Bootstrap b = configureBootstrap(new Bootstrap());

            // Start the connection attempt.
            ch = b.connect(host, port).sync().channel();

            // Read commands from the stdin.
            ChannelFuture lastWriteFuture = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            for (; ; ) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                // Sends the received line to the server.
                lastWriteFuture = ch.writeAndFlush(line + "\r\n");
                ch.flush();

                // If user typed the 'bye' command, wait until the server closes
                // the connection.
                if ("bye".equals(line.toLowerCase())) {
                    ch.closeFuture().sync();
                    break;
                }
            }

            // Wait until all messages are flushed before closing the channel.
            if (lastWriteFuture != null) {
                System.out.println("Sync called");
                lastWriteFuture.sync();
            }
        } finally {

        }
    }

    public static void main(String[] args) throws Exception {
        new Client("localhost", 8432).run();
    }
}
