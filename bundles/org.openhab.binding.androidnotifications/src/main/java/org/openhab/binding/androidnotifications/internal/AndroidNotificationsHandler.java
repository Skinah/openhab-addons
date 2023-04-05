/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import static org.openhab.binding.androidnotifications.internal.AndroidNotificationsBindingConstants.CHANNEL_1;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Base64;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * The {@link AndroidNotificationsHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class AndroidNotificationsHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(AndroidNotificationsHandler.class);
    private final HttpClient httpClient;
    private EventLoopGroup serversLoopGroup = new NioEventLoopGroup();
    private @Nullable ServerBootstrap serverBootstrap;
    private @Nullable ChannelFuture serverFuture = null;
    String ipAddress = "";
    String tvIP = "http://192.168.1.85:7676";

    private @Nullable AndroidNotificationsConfiguration config;

    public AndroidNotificationsHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    private AndroidNotificationsHandler getHandle() {
        return this;
    }

    public String sendGetRequest(String url) {
        Request request = httpClient.newRequest(tvIP + url);
        request.timeout(3, TimeUnit.SECONDS);
        request.method(HttpMethod.GET);
        request.header(HttpHeader.ACCEPT_ENCODING, "gzip");
        logger.trace("Sending WLED GET:{}", url);
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("WLED request failed with %d: %s", contentResponse.getStatus(),
                        contentResponse.getReason());
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: WLED was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        }
        return errorReason;
    }

    public String sendRAWPostRequest() {
        // byte[] bytes = "Hello, World!".getBytes("UTF-8");
        String encoded = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGP6zwAAAgcBApocMXEAAAAASUVORK5CYII="; // =
                                                                                                                         // Base64.getEncoder().encodeToString(bytes);
        // String encoded =
        // "\"iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGP6zwAAAgcBApo\"\r\n"
        // + "\"cMXEAAAAASUVORK5CYII=\"\r\n" + " ";
        byte[] decoded = Base64.getDecoder().decode(encoded);

        "--myBoundary\r\n"
                + "Content-Disposition: form-data; name=\"filename\"; filename=\"icon.png\"\r\n"
                + "Content-Type: application/octet-stream" + "\r\n" + "Expires: 0\r\n" + "\r\n"
                + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGP6zwAAAgcBApocMXEAAAAASUVORK5CYII="
                + "\r\n" + "--myBoundary" + "\r\n"
                + "Content-Disposition: form-data; name=\"msg\"; filename=\"msg\"" + "\r\n" + "\r\n"
                + "Tell me if this works please" + "\r\n" + "--myBoundary" + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"; filename=\"title\"\r\n" + "\r\n"
                + "Home Assistant\r\n" + "--myBoundary--\r\n"


        logger.info("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(tvIP);
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        request.content(new StringContentProvider(content), "multipart/form-data; boundary=myBoundary");
        request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(content.length()));
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.info("Response back:{}", contentResponse.getContentAsString());
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("Android Notification request failed with %d: %s",
                        contentResponse.getStatus(), contentResponse.getReason());
            }
        } catch (InterruptedException e) {
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: WLED was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        }
        logger.info("Returned Message:{}", errorReason);
        return errorReason;
    }

    public String sendPostRequest(String content) {
        logger.info("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(tvIP);
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        request.content(new StringContentProvider(content), "multipart/form-data; boundary=myBoundary");
        request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(content.length()));
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.info("Response back:{}", contentResponse.getContentAsString());
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("Android Notification request failed with %d: %s",
                        contentResponse.getStatus(), contentResponse.getReason());
            }
        } catch (InterruptedException e) {
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: WLED was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        }
        logger.info("Returned Message:{}", errorReason);
        return errorReason;
    }

    public String sendJSONPostRequest(String content) {
        logger.info("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(tvIP);
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        request.content(new StringContentProvider(content), "application/json");
        request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(content.length()));
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.info("Response back:{}", contentResponse.getContentAsString());
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("Android Notification request failed with %d: %s",
                        contentResponse.getStatus(), contentResponse.getReason());
            }
        } catch (InterruptedException e) {
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: WLED was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        }
        logger.info("Returned Message:{}", errorReason);
        return errorReason;
    }

    public String getLocalIpAddress() {
        ipAddress = "";
        try {
            for (Enumeration<NetworkInterface> enumNetworks = NetworkInterface.getNetworkInterfaces(); enumNetworks
                    .hasMoreElements();) {
                NetworkInterface networkInterface = enumNetworks.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr
                        .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().toString().length() < 18
                            && inetAddress.isSiteLocalAddress()) {
                        ipAddress = inetAddress.getHostAddress().toString();
                        logger.debug("Possible NIC/IP match found:{}", ipAddress);
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return ipAddress;
    }

    @SuppressWarnings("null")
    public void startStreamServer(boolean start) {

        if (!start) {
            serversLoopGroup.shutdownGracefully();
            serverBootstrap = null;
        } else {
            if (serverBootstrap == null) {
                String hostIp = getLocalIpAddress();
                try {
                    serversLoopGroup = new NioEventLoopGroup();
                    serverBootstrap = new ServerBootstrap();
                    serverBootstrap.group(serversLoopGroup);
                    serverBootstrap.channel(NioServerSocketChannel.class);
                    // IP "0.0.0.0" will bind the server to all network connections//
                    serverBootstrap.localAddress(new InetSocketAddress("0.0.0.0", 7676));
                    serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 25, 0));
                            socketChannel.pipeline().addLast("HttpServerCodec", new HttpServerCodec());
                            socketChannel.pipeline().addLast("ChunkedWriteHandler", new ChunkedWriteHandler());
                            socketChannel.pipeline().addLast("streamServerHandler",
                                    new StreamServerHandler(getHandle()));
                        }
                    });
                    serverFuture = serverBootstrap.bind().sync();
                    serverFuture.await(4000);
                    logger.debug("File server for camera at {} has started on port {} for all NIC's.", ipAddress, 7676);
                } catch (Exception e) {
                    logger.error(
                            "Exception occured when starting the streaming server. Try changing the SERVER_PORT to another number: {}",
                            e.getMessage());
                }
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_1.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(AndroidNotificationsConfiguration.class);
        updateStatus(ThingStatus.ONLINE);
        startStreamServer(true);
    }

    @Override
    public void dispose() {
        startStreamServer(false);
    }
}
