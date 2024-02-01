/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.androidnotifications.internal.androidtvnotifications;

import static org.openhab.binding.androidnotifications.internal.AndroidNotificationsBindingConstants.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.imageio.ImageIO;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.ByteBufferContentProvider;
import org.eclipse.jetty.client.util.MultiPartContentProvider;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.androidnotifications.internal.StreamServerHandler;
import org.openhab.binding.androidnotifications.internal.action.AndroidNotificationActions;
import org.openhab.core.OpenHAB;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * The {@link AndroidTvNotificationsDisplayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class AndroidTvNotificationsDisplayHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final HttpClient httpClient;
    private @Nullable ScheduledFuture<?> pollingFuture = null;
    private EventLoopGroup serversLoopGroup = new NioEventLoopGroup();
    private @Nullable ServerBootstrap serverBootstrap;
    private @Nullable ChannelFuture serverFuture = null;
    public String baseUrlAndPort = "";
    private String openHABipAddress = "";
    private String position = "0";
    private String fontSize = "0";

    private AndroidTvNotificationsDisplayConfiguration config = new AndroidTvNotificationsDisplayConfiguration();

    public AndroidTvNotificationsDisplayHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    private AndroidTvNotificationsDisplayHandler getHandle() {
        return this;
    }

    public String sendGetRequest(String url) {
        Request request;
        String errorReason = "";
        request = httpClient.newRequest(baseUrlAndPort + url);
        request.header("User-Agent", null);// remove the auto added Jetty agent header
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 13; CPH2195 Build/TP1A.220905.001)");
        request.header("Host", config.address + ":" + config.port);
        request.header("Connection", "Keep-Alive");
        request.header("Accept-Encoding", "gzip");
        request.header("content-length", "0");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.GET);

        logger.debug("Sending TV GET:{}", url);

        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("TV request failed with %d: %s", contentResponse.getStatus(),
                        contentResponse.getContentAsString());
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: TV was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        }
        return errorReason;
    }

    public String sendPostRequest(String url, String content) {
        Request request;
        String errorReason = "";

        request = httpClient.newRequest(baseUrlAndPort + url);

        request.header("User-Agent", null);// remove the auto added Jetty agent header
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 13; CPH2195 Build/TP1A.220905.001)");
        request.header("Host", config.address + ":" + config.port);
        request.header("Connection", "Keep-Alive");
        // request.header("content-length", "0");
        request.timeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        request.method(HttpMethod.POST);
        request.content(new StringContentProvider(content), "application/json");
        logger.trace("Sending TV POST:{} JSON:{}", url, content);

        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("TV request failed with %d: %s", contentResponse.getStatus(),
                        contentResponse.getContentAsString());
            }
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: TV was not reachable on your network";
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
        String content = "--myBoundary\r\n"
                + "Content-Disposition: form-data; name=\"filename\"; filename=\"icon.png\"\r\n"
                + "Content-Type: application/octet-stream" + "\r\n" + "Expires: 0\r\n" + "\r\n"
                + "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGP6zwAAAgcBApocMXEAAAAASUVORK5CYII="
                + "\r\n" + "--myBoundary" + "\r\n" + "Content-Disposition: form-data; name=\"msg\"; filename=\"msg\""
                + "\r\n" + "\r\n" + "Tell me if this works please" + "\r\n" + "--myBoundary" + "\r\n"
                + "Content-Disposition: form-data; name=\"title\"; filename=\"title\"\r\n" + "\r\n"
                + "Home Assistant\r\n" + "--myBoundary--\r\n";

        logger.trace("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(baseUrlAndPort + "/");
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        // request.header("Content-Type", "multipart/form-data; boundary=--myBoundary");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Host", "192.168.1.85:7676");
        request.header("Accept-Encoding", "gzip");
        request.content(new StringContentProvider(content), "multipart/form-data; boundary=--myBoundary");
        request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(content.length()));
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());
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
        logger.debug("Returned error message from sending to the TV:{}", errorReason);
        return errorReason;
    }

    public String sendPostMultipartRequest() {
        Request request = httpClient.POST(baseUrlAndPort + "/");
        request.timeout(5, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", null);// remove the auto added Jetty agent header
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        // request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        MultiPartContentProvider multiPart = new MultiPartContentProvider();
        // multiPart.addFieldPart("hash",
        // new StringContentProvider("\r\n" + "rS3ebOjDbBM7vkoNJibCHWWRyLT9N7Giw1ZFUkt+GZY="), null);
        multiPart.addFieldPart("type", new StringContentProvider("0"), null);
        multiPart.addFieldPart("title", new StringContentProvider("38AYmqUlyr0FtXfosV18jg=="), null);
        multiPart.addFieldPart("msg", new StringContentProvider(
                "AqHdWviqhWicqsrYH/MIpLezWE0uJSqs3fRwB4zrzapVgw3CmjRuPocdI7m7fXqAektRqYRKn/wECbujzAU8c4JAor6KnwLCnbAS8FsYT1DlRcHKgUhPAW2jC1KdL2vTWBfnlhOKX4q6u/THNt1ALQ=="),
                null);
        multiPart.addFieldPart("duration", new StringContentProvider("6"), null);
        multiPart.addFieldPart("fontsize", new StringContentProvider("0"), null);
        multiPart.addFieldPart("position", new StringContentProvider("0"), null);
        multiPart.addFieldPart("width", new StringContentProvider("0"), null);
        multiPart.addFieldPart("bkgcolor", new StringContentProvider("#607d8b"), null);
        multiPart.addFieldPart("transparency", new StringContentProvider("0"), null);
        multiPart.addFieldPart("offset", new StringContentProvider("0"), null);
        multiPart.addFieldPart("offsety", new StringContentProvider("0"), null);
        multiPart.addFieldPart("app", new StringContentProvider("Notifications for Android TV"), null);
        multiPart.addFieldPart("demo", new StringContentProvider("true"), null);
        multiPart.addFieldPart("force", new StringContentProvider("true"), null);
        multiPart.addFilePart("filename", "icon.png",
                new ByteBufferContentProvider(getIconData("qualityofservice-1.png")), null);
        // multiPart.addFieldPart(null, new StringContentProvider("\r\n\r\n"), null);

        // request.content(multiPart, "multipart/form-data; boundary="+multiPart.);

        request.content(multiPart);
        request.header(HttpHeader.CONTENT_LENGTH, Long.toString(multiPart.getLength()));
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());

                return contentResponse.getContentAsString();
            } else {
                errorReason = String.format("Android Notification request failed with %d: %s",
                        contentResponse.getStatus(), contentResponse.getContentAsString());
            }
        } catch (InterruptedException e) {
            errorReason = String.format("InterruptedException: %s", e.getMessage());
        } catch (TimeoutException e) {
            errorReason = "TimeoutException: WLED was not reachable on your network";
        } catch (ExecutionException e) {
            errorReason = String.format("ExecutionException: %s", e.getMessage());
        }
        logger.info("Returned Error Message:{}", errorReason);
        return errorReason;
    }

    public String sendPostMultipartRequest(String content) {
        logger.trace("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(baseUrlAndPort + "/");
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        MultiPartContentProvider multiPart = new MultiPartContentProvider();
        multiPart.addFieldPart("--myBoundary", new StringContentProvider(content), null);
        multiPart.addFilePart("icon", "qualityofservice-1.png",
                new ByteBufferContentProvider(getIconData("qualityofservice-1.png")), null);

        request.content(multiPart, "multipart/form-data; boundary=myBoundary");
        // request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(content.length()));
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());

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
        logger.info("Returned Error Message:{}", errorReason);
        return errorReason;
    }

    public String sendPostRequest(String content) {
        logger.trace("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(baseUrlAndPort + "/");
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        request.content(new StringContentProvider(content), "multipart/form-data; boundary=SwA1705307557152SwA");
        request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(content.length()));
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());
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
        logger.info("Returned Error Message:{}", errorReason);
        return errorReason;
    }

    private ByteBuffer getIconData(String filename) {
        String configDirectory = OpenHAB.getConfigFolder();
        File file = new File(new File(configDirectory, "icons/classic/"), filename);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            BufferedImage bi = ImageIO.read(file);
            ImageIO.write(bi, "png", out);
        } catch (IOException e) {
            logger.info("exception reading png icon file");
        }
        return ByteBuffer.wrap(out.toByteArray());
    }

    public String sendJSONPostRequestWithPNG(String url, String filename) {
        String configDirectory = OpenHAB.getConfigFolder();
        File file = new File(new File(configDirectory, "icons/classic/"), filename);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            BufferedImage image = ImageIO.read(file);
            ImageIO.write(image, "png", outputStream);
        } catch (IOException e) {
            return "exception";
        }
        String encodedImageFile;
        try {
            encodedImageFile = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return "exception";
        }
        String content = "{'filename': ('icon.png', '" + encodedImageFile
                + "', 'application/octet-stream',{'Expires': '0'}), 'msg': 'It WORKS', 'title': 'Title text', 'fontsize': 0, 'bkgcolor':'#4CAF50'}";
        logger.trace("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(baseUrlAndPort + url);
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        request.content(new StringContentProvider(content), "application/json");
        // request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(content.length()));
        request.header(HttpHeader.CONTENT_LENGTH, Integer.toString(9921));

        // request.header(HttpHeader.CONTENT_LENGTH,
        // Integer.toString(getIconData("qualityofservice-1.png").remaining()));
        // code example here to learn from
        // https://www.dynamsoft.com/codepool/how-to-implement-a-java-websocket-server-for-image-transmission-with-jetty.html
        // mSession.getRemote().sendBytes(getIconData("qualityofservice-1.png"));
        // out.close();
        // byteBuffer.clear();

        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());
                request.content(new ByteBufferContentProvider(getIconData("qualityofservice-1.png")));
                request.header(HttpHeader.CONTENT_LENGTH,
                        Integer.toString(getIconData("qualityofservice-1.png").remaining()));
                request.send();
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
        logger.info("Returned Error Message:{}", errorReason);
        return errorReason;
    }

    public String sendQueryRequest(String url) {
        Request request = httpClient.POST(baseUrlAndPort + url);
        request.timeout(3, TimeUnit.SECONDS);
        request.header("Authorization", "Basic Og==");
        request.header("Connection", "Keep-Alive");
        request.header("Host", "192.168.1.85:7676");
        request.header("User-Agent", "Dalvik/2.1.0 (Linux; U; Android 12; CPH2195 Build/SKQ1.210216.001)");
        request.header("Accept-Encoding", "gzip, deflate, br");
        request.header("Accept", "*/*");
        request.header(HttpHeader.CONTENT_LENGTH, "0");
        String errorReason = "";
        try {
            ContentResponse contentResponse = request.send();
            if (contentResponse.getStatus() == 200) {
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());
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
        logger.warn("TV returned error message:{}", errorReason);
        return errorReason;
    }

    // do not mess with this till another one works better
    public String sendJSONPostRequest(String url, String content) {
        logger.trace("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(baseUrlAndPort + url);
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
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());
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
        logger.info("Returned Error Message:{}", errorReason);
        return errorReason;
    }

    // do not mess with this till another one works better
    public String sendJSONPostNone(String url) {
        String content = "{\r\n" + "\"duration\": 30,\r\n" + "\"position\": 0,\r\n"
                + "\"title\": \"From openHAB Binding\",\r\n" + "\"titleColor\": \"#0066cc\",\r\n"
                + "\"titleSize\": 20,\r\n"
                + "\"message\": \"Please consider sponsering or tipping Skinah for making this work.\",\r\n"
                + "\"messageColor\": \"#000000\",\r\n" + "\"messageSize\": 14,\r\n"
                + "\"backgroundColor\": \"#ffffff\",\r\n" + "}\r\n" + "";
        logger.trace("Sending {} bytes to TV. Message:{}", Integer.toString(content.length()), content);
        Request request = httpClient.POST(baseUrlAndPort + url);
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
                logger.trace("Response back from TV:{}", contentResponse.getContentAsString());
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
        logger.info("Returned Error Message:{}", errorReason);
        return errorReason;
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> enumNetworks = NetworkInterface.getNetworkInterfaces(); enumNetworks
                    .hasMoreElements();) {
                NetworkInterface networkInterface = enumNetworks.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr
                        .hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getHostAddress().toString().length() < 18
                            && inetAddress.isSiteLocalAddress()) {
                        openHABipAddress = inetAddress.getHostAddress().toString();
                        logger.debug("Possible NIC/IP match found:{}", openHABipAddress);
                    }
                }
            }
        } catch (SocketException ex) {
        }
        return openHABipAddress;
    }

    @SuppressWarnings("null")
    public void startStreamServer(boolean start) {
        getLocalIpAddress();
        if (!start) {
            serversLoopGroup.shutdownGracefully();
            serverBootstrap = null;
        } else {
            if (serverBootstrap == null) {
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
                            socketChannel.pipeline().addLast("idleStateHandler", new IdleStateHandler(60, 180, 0));
                            socketChannel.pipeline().addLast("HttpServerCodec", new HttpServerCodec());
                            socketChannel.pipeline().addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
                            socketChannel.pipeline().addLast("streamServerHandler",
                                    new StreamServerHandler(getHandle()));
                        }
                    });
                    serverFuture = serverBootstrap.bind().sync();
                    serverFuture.await(4000);
                    logger.info(
                            "A File server has started at openHAB's IP {}:7676 you can tell your mobile phone app that this is a TV and send messages directly to openHAB.",
                            openHABipAddress);
                } catch (Exception e) {
                    logger.error("Exception occured when starting the streaming server: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (!(command instanceof RefreshType)) {
            switch (channelUID.getId()) {
                case CHANNEL_DISPLAY_NOTIFICATIONS:
                    if (command instanceof OnOffType) {
                    }
                    break;
                case CHANNEL_SEND_TEST_NOTIFICATION:
                    if (command instanceof OnOffType) {
                        sendQueryRequest("/show?title=openHAB&msg=TheTestMessageWorks&fontsize=" + fontSize
                                + "&position=" + position);
                    }
                    break;
                case CHANNEL_SEND_NOTIFICATION:
                    if (command instanceof StringType) {
                        try {
                            sendQueryRequest("/show?title=openHAB&msg=" + URLEncoder.encode(command.toString(), "UTF-8")
                                    + "&fontsize=" + fontSize + "&position=" + position);
                        } catch (UnsupportedEncodingException e) {
                            logger.warn("UnsupportedEncodingException:{}", e.getMessage());
                        }
                    }
                    break;
                case CHANNEL_FONT_SIZE:
                    if (command instanceof StringType) {
                        fontSize = command.toString();
                    }
                    break;
                case CHANNEL_DISPLAY_POSITION:
                    if (command instanceof StringType) {
                        position = command.toString();
                    }
                    break;
            }
        }
    }

    @Override
    public void initialize() {
        config = getConfigAs(AndroidTvNotificationsDisplayConfiguration.class);
        if (config.address.contains("://")) {
            logger.warn("Address needs to be a pure IP or hostname that does not include http/s");
            baseUrlAndPort = config.address + ":" + config.port;
        } else {
            baseUrlAndPort = "http://" + config.address + ":" + config.port;
        }
        startStreamServer(true);
        pollingFuture = scheduler.scheduleWithFixedDelay(this::pollState, 0, config.pollTime, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        startStreamServer(false);
        Future<?> future = pollingFuture;
        if (future != null) {
            future.cancel(true);
            pollingFuture = null;
        }
    }

    private void pollState() {
        String result = sendGetRequest("/available");
        if (result.startsWith("OK")) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, result);
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(AndroidNotificationActions.class);
    }

    public boolean sendText(String messageID, @Nullable String title, @Nullable String message,
            @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration) {
        try {
            return "OK".equals(sendQueryRequest("/show?title=" + URLEncoder.encode(title, "UTF-8") + "&msg="
                    + URLEncoder.encode(message, "UTF-8") + "&fontsize=" + fontSize + "&position=" + position));
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException:{}", e.getMessage());
        }
        return false;
    }

    public boolean sendVideo(String messageID, @Nullable String title, @Nullable String message, String videoURL,
            @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration) {
        try {
            return "OK".equals(sendQueryRequest(
                    "/show?title=openHAB&msg=" + URLEncoder.encode("Video is NOT implemented yet", "UTF-8")
                            + "&fontsize=" + fontSize + "&position=" + position));
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException:{}", e.getMessage());
        }
        return false;
    }

    public boolean sendImage(String messageID, @Nullable String title, @Nullable String message, String imageURL,
            @Nullable String largeIcon, @Nullable String smallIcon, @Nullable String smallIconColor,
            @Nullable String corner, @Nullable Integer duration) {
        try {
            return "OK".equals(sendQueryRequest(
                    "/show?title=openHAB&msg=" + URLEncoder.encode("Image is NOT implemented yet", "UTF-8")
                            + "&fontsize=" + fontSize + "&position=" + position));
        } catch (UnsupportedEncodingException e) {
            logger.warn("UnsupportedEncodingException:{}", e.getMessage());
        }
        return false;
    }

    public boolean sendFixedNotification(String messageID, @Nullable String messageColor, @Nullable String message,
            @Nullable String icon, @Nullable String iconColor, @Nullable String borderColor,
            @Nullable String backgroundColor, @Nullable Integer expiration, @Nullable String shape,
            @Nullable Boolean visible) {
        logger.info("Feature only supported in TvOverlay app");
        return false;
    }
}
