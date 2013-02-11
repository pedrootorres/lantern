package org.lantern;

import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;

import org.lantern.HttpRequestProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fulfills requests to local proxy w/o routing through remote Lantern proxies.
 */
public class LocalExitRequestProcessor implements HttpRequestProcessor {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private DispatchingProxyRelayHandler proxyDispatcher;
    private ClientSocketChannelFactory clientSocketChannelFactory;
    private LanternKeyStoreManager ksm;

    public LocalExitRequestProcessor(DispatchingProxyRelayHandler proxyDispatcher) {
        this.proxyDispatcher = proxyDispatcher;
        this.clientSocketChannelFactory = 
            proxyDispatcher.getClientSocketChannelFactory();
        this.ksm = proxyDispatcher.getKeyStoreManager();
    }

    public boolean processRequest(final Channel browserToProxyChannel,
            ChannelHandlerContext ctx, MessageEvent ev) {
        HttpRequest req = (HttpRequest)ev.getMessage();
        log.debug("processing request to: {}", req.getUri());
        ChannelFuture channelFuture = openOutgoingChannel(
            browserToProxyChannel, req
        );
        return true;
    }

    public ChannelFuture openOutgoingChannel(final Channel browserToProxyChannel,
           final HttpRequest req) {

        ClientBootstrap cb = new ClientBootstrap(
            clientSocketChannelFactory
        );
        ChannelPipeline p = cb.getPipeline();
        final LanternClientSslContextFactory sslFactory =
            new LanternClientSslContextFactory(this.ksm);
        SSLEngine engine = sslFactory.getClientContext().createSSLEngine();
        engine.setUseClientMode(true);
        //p.addLast("ssl", new SslHandler(engine));
        p.addLast("encoder", new HttpRequestEncoder());
        p.addLast(
            "handler", 
            new SimpleChannelUpstreamHandler() {
                @Override
                public void messageReceived(ChannelHandlerContext ctx, 
                        MessageEvent ev) {
                    browserToProxyChannel.write(ev.getMessage());
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ev) {
                    log.debug("caught exception in exit client handler: {}", ev);
                }
            }
        );

        ChannelFuture connectFuture = cb.connect(this.uriToAddress(req.getUri()));
       
        connectFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture connectFuture) 
                throws Exception {
                if (connectFuture.isSuccess()) {
                    log.debug("connected via exit handler");
                    browserToProxyChannel.setReadable(true);
                    connectFuture.getChannel().write(req);
                } else {
                    log.debug("connect via exit handler failed");
                    browserToProxyChannel.close();
                }
            }
        });
        return connectFuture;
    }

    public boolean processChunk(ChannelHandlerContext ctx, MessageEvent ev) {
        log.debug("processing chunk?!");
        return false;
    }

    public void close() {
        log.debug("closing...");
    }

    private InetSocketAddress uriToAddress(String uri) {
        URL url;
        try {
            url = new URL(uri);
        } catch(MalformedURLException e) {
            log.error("malformed url: {}", uri);
            return null;
        }
        String host = url.getHost();
        Integer port = url.getPort();
        if(port < 0) {
            if(url.getProtocol().matches("^https")) {
                port = 443;
            } else {
                port = 80;
            }
        }
        return new InetSocketAddress(host, port);
    }

}
