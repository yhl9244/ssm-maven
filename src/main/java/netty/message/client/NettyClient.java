package netty.message.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import netty.message.client.handler.LoginResponseHandler;
import netty.message.client.handler.MessageResponseHandler;
import netty.message.codec.PacketDecoder;
import netty.message.codec.PacketEncoder;
import netty.message.codec.Spliter;
import netty.message.protocol.request.MessageRequestPacket;
import netty.message.util.LoginUtil;

import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class NettyClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8000;
    private static final int MAX_RETRY = 5;

    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();

        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new Spliter());
                        ch.pipeline().addLast(new PacketDecoder());
                        ch.pipeline().addLast(new LoginResponseHandler());
                        ch.pipeline().addLast(new MessageResponseHandler());
                        ch.pipeline().addLast(new PacketEncoder());
                    }
                });
        connect(bootstrap,HOST,PORT,MAX_RETRY);
    }

    private static void connect(Bootstrap bootstrap, String host, int port, int maxRetry) {
        bootstrap.connect(host,port).addListener(new GenericFutureListener<Future<? super Void>>() {
            @Override
            public void operationComplete(Future<? super Void> future) throws Exception {
                if(future.isSuccess()){
                    System.out.println(new Date() + ": 连接成功，启动控制台线程……");
                    Channel channel = ((ChannelFuture) future).channel();
                    startConsoleThread(channel);
                }else if(maxRetry == 0){
                    System.err.println("重试次数已用完，放弃连接！");
                }else{
                    int order = (MAX_RETRY - maxRetry) + 1;
                    int delay = 1>>order;
                    System.err.println(new Date() + ": 连接失败，第" + order + "次重连……");
                    bootstrap.config().group().schedule(()->{
                        connect(bootstrap,host,port, maxRetry-1);
                    }, delay, TimeUnit.SECONDS);
                }
            }
        });
    }

    private static void startConsoleThread(Channel channel) {
        new Thread(()->{
            while (!Thread.interrupted()){
                //if(LoginUtil.hasLogin(channel)){
                    System.out.println("输入消息发送至服务端: ");
                    Scanner sc = new Scanner(System.in);
                    String line = sc.nextLine();
                    channel.writeAndFlush(new MessageRequestPacket(line));
                //}
            }
        }).start();
    }
}
