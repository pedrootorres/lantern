package org.lantern;

import java.io.IOException;

import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.InetSocketAddress;

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
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
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
        Boolean tunnel = false;
        if(req.getMethod() == HttpMethod.CONNECT) {
            tunnel = true;
        }
        ChannelFuture channelFuture = openOutgoingChannel(
            ctx, browserToProxyChannel, req, tunnel
        );
        return true;
    }

    public ChannelFuture openOutgoingChannel(
            final ChannelHandlerContext ctx, 
            final Channel browserToProxyChannel, 
            final HttpRequest req, final Boolean tunnel) {

        ClientBootstrap cb = new ClientBootstrap(
            clientSocketChannelFactory
        );
        InetSocketAddress remoteAddress = this.uriToAddress(req.getUri());
        log.debug("got remote address: {} {}", remoteAddress.getHostName(), remoteAddress.getPort());

        final ChannelFuture connectFuture = cb.connect(remoteAddress);
        ChannelPipeline p = connectFuture.getChannel().getPipeline();

        if(tunnel) {
            ctx.getPipeline().remove("decoder");
            ctx.getPipeline().remove("handler");
            ctx.getPipeline().addLast("handler", new SimpleChannelUpstreamHandler() {
                @Override
                public void messageReceived(ChannelHandlerContext ctx,
                        MessageEvent ev) {
                    log.debug("inbound msg rcvd on outbound channel");
                    connectFuture.getChannel().write(ev.getMessage());
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ev) {
                    log.debug("caught exception in exit outbound channel: {}", ev);
                }
            });
        } else {
            p.addLast("encoder", new HttpRequestEncoder());
        }
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

       
        connectFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture connectFuture) 
                throws Exception {
                if (connectFuture.isSuccess()) {
                    log.debug("connected via exit handler");
                    browserToProxyChannel.setReadable(true);
                    if(tunnel) {
                        log.debug("sending 200 response to CONNECT request (start tunneling)");
                        HttpResponse resp = new DefaultHttpResponse(
                            HttpVersion.HTTP_1_0, HttpResponseStatus.OK
                        );
                        resp.setHeader("Proxy-agent", "LanternLocal/0.0");
                        browserToProxyChannel.write(resp);
                        // we need this encoder to send the 200,
                        // after that, we need to drop it.
                        ctx.getPipeline().remove("encoder");
                    } else {
                        log.debug("writing http request to dst");
                        connectFuture.getChannel().write(req);
                    }
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

    private InetSocketAddress uriToAddress(String uriString) {
        URI uri;
        if(!uriString.substring(0, 4).matches("http")) {
            uriString = "https://" + uriString;
        }
        log.debug("parsing uri: {}", uriString);
        try {
            uri = new URI(uriString);
        } catch(URISyntaxException e) {
            log.error("malformed uri: {}, {}", uriString, e);
            return null;
        }
        String host = uri.getHost();
        Integer port = uri.getPort();
        if(port < 0) {
            if(uri.getScheme().matches("^https")) {
                port = 443;
            } else {
                port = 80;
            }
        }
        return new InetSocketAddress(host, port);
    }

}
