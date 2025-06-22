/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.androidnotifications.internal;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.androidnotifications.internal.androidtvnotifications.AndroidTvNotificationsDisplayHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

/**
 * The {@link StreamServerHandler} class is responsible for handling streams and sending any requested files to Openhabs
 * features.
 *
 * @author Matthew Skinner - Initial contribution
 */

@NonNullByDefault
public class StreamServerHandler extends ChannelInboundHandlerAdapter {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private AndroidTvNotificationsDisplayHandler handler;

    public StreamServerHandler(AndroidTvNotificationsDisplayHandler androidTvNotificationsDisplayHandler) {
        this.handler = androidTvNotificationsDisplayHandler;
    }

    @Override
    public void handlerAdded(@Nullable ChannelHandlerContext ctx) {
        logger.trace("Opening a StreamServerHandler.");
    }

    private void sendOk(ChannelHandlerContext ctx) throws IOException {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().add("Content-Type", "text/html");
        response.headers().add("Date", "Sat, 20 Jan 2024 11:38:14 GMT");
        response.headers().set("Connection", HttpHeaderValues.KEEP_ALIVE);
        response.headers().add("Content-Length", 2);
        ctx.channel().write(response);
        ByteBuf footerBbuf = Unpooled.copiedBuffer("OK", 0, 2, StandardCharsets.UTF_8);
        ctx.channel().writeAndFlush(footerBbuf);
    }

    private void sendEmptyOk(ChannelHandlerContext ctx) throws IOException {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().add("Content-Type", "text/html");
        response.headers().add("Date", "Sat, 20 Jan 2024 11:38:15 GMT");
        response.headers().set("Connection", HttpHeaderValues.KEEP_ALIVE);
        response.headers().add("Content-Length", 0);
        ctx.channel().writeAndFlush(response);
    }

    @Override
    public void channelRead(@Nullable ChannelHandlerContext ctx, @Nullable Object msg) throws Exception {
        if (ctx == null) {
            return;
        }
        try {
            if (msg instanceof HttpRequest) {
                HttpRequest httpRequest = (HttpRequest) msg;

                if ("GET".equalsIgnoreCase(httpRequest.method().toString())) {
                    // Some browsers send a query string after the path when refreshing a picture.
                    QueryStringDecoder queryStringDecoder = new QueryStringDecoder(httpRequest.uri());
                    logger.trace("Full URI is GET:{}", httpRequest.uri());
                    switch (httpRequest.uri()) {
                        case "/available":
                            logger.debug("From binding to TV/available:{}", handler.sendGetRequest("/available"));
                            sendOk(ctx);
                            break;
                        case "/id":
                            // logger.debug("From binding to TV/id:{}", handler.sendGetRequest("/id"));
                            // sendOk(ctx);
                            sendEmptyOk(ctx);
                            // handler.sendJSONPostRequestWithPNG("/notify", "qualityofservice-1.png");
                            handler.sendPostMultipartRequest();
                            break;
                        default:
                            sendEmptyOk(ctx);
                            logger.debug("Stream Server recieved unknown request {}:{}", httpRequest.method(),
                                    httpRequest.uri());
                    }

                } else if ("POST".equalsIgnoreCase(httpRequest.method().toString())) {
                    logger.trace("Recieved request {}:{}", httpRequest.method(), httpRequest.uri());
                    sendOk(ctx);
                } else {
                    logger.info("Stream Server recieved unknown request");
                }
            }
            if (msg instanceof HttpContent) {
                HttpContent content = (HttpContent) msg;
                // logger.trace("HttpContent:{}", content.content());
            }
            if (msg instanceof LastHttpContent) {
                logger.trace("From App:{}", msg);
                LastHttpContent lastContent = (LastHttpContent) msg;
                logger.trace("LastcontentUTF:{}", lastContent.content().toString(StandardCharsets.UTF_8));

                // handler.sendPostRequest(((LastHttpContent) msg).content().toString(StandardCharsets.UTF_8));

                // ByteBuf data = ((ByteBufHolder) msg).content();
                // logger.info("LastcontentUTF:{}", data.toString(StandardCharsets.UTF_8));

                // need a new channel for following to work
                // ByteBuf byteBuffer = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
                // FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                // content);
                // response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
                // response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
                // ctx.write(response);
                // ctx.flush();
            }
        } finally {
            ReferenceCountUtil.release(msg);
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(@Nullable ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void exceptionCaught(@Nullable ChannelHandlerContext ctx, @Nullable Throwable cause) throws Exception {
        if (ctx == null || cause == null) {
            return;
        }
        if (cause.toString().contains("Connection reset by peer")) {
            logger.warn("Connection reset by peer.");
        } else if (cause.toString().contains("An established connection was aborted by the software")) {
            logger.warn("An established connection was aborted by the software");
        } else if (cause.toString().contains("An existing connection was forcibly closed by the remote host")) {
            logger.warn("An existing connection was forcibly closed by the remote host");
        } else if (cause.toString().contains("(No such file or directory)")) {
            logger.warn("Could not find the requested file. This may happen if ffmpeg is still creating the file.");
        } else {
            logger.warn("Exception caught from stream server:{}", cause.getMessage());
        }
        ctx.close();
    }

    @Override
    public void userEventTriggered(@Nullable ChannelHandlerContext ctx, @Nullable Object evt) throws Exception {
        if (ctx == null) {
            return;
        }
        logger.warn("Stream Server:{}.", evt);
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.WRITER_IDLE) {
                logger.warn("Stream server detected an idle channel.");
                ctx.close();
            }
        }
    }

    @Override
    public void handlerRemoved(@Nullable ChannelHandlerContext ctx) {
        if (ctx == null) {
            return;
        }
        logger.trace("StreamServerHandler, handler removed.");
        ctx.close();
    }
}
