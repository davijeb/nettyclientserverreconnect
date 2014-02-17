package io.netty.example.reconnect;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a client-side channel.
 */
public class ClientHandler extends SimpleChannelInboundHandler<String> {

    private long startTime = -1;
    private final Client client;


    public ClientHandler(Client client) {
        this.client = client;
    }

    private static final Logger logger = Logger.getLogger(
            ClientHandler.class.getName());


    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.err.println(msg);
    }
//
//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        if (!(evt instanceof IdleStateEvent)) {
//            return;
//        }
//
//        IdleStateEvent e = (IdleStateEvent) evt;
//        if (e.state() == IdleState.READER_IDLE) {
//            // The connection was OK but there was no traffic for last period.
//            println("Disconnecting due to no inbound traffic");
//            ctx.close();
//        }
//    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx)
            throws Exception {
        println("Sleeping for: " + Client.RECONNECT_DELAY + 's');

        final EventLoop loop = ctx.channel().eventLoop();

        loop.schedule(new Runnable() {
            @Override
            public void run() {
                println("Reconnecting to: " + ctx.channel().remoteAddress());
                try {
                    client.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, Client.RECONNECT_DELAY, TimeUnit.SECONDS);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) throws Exception {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }
        println("Connected to: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        println("Disconnected from: " + ctx.channel().remoteAddress());
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.log(
                Level.WARNING,
                "Unexpected exception from downstream.", cause);
        ctx.close();
    }

    void println(String msg) {
        if (startTime < 0) {
            System.err.format("[SERVER IS DOWN] %s%n", msg);
        } else {
            System.err.format("[UPTIME: %5ds] %s%n", (System.currentTimeMillis() - startTime) / 1000, msg);
        }
    }
}

