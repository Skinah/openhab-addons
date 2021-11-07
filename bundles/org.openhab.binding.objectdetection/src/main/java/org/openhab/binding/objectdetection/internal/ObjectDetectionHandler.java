/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.objectdetection.internal;

import static org.openhab.binding.objectdetection.internal.ObjectDetectionBindingConstants.CHANNEL_1;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpStatus;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ObjectDetectionHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author MatthewSkinner - Initial contribution
 */
@NonNullByDefault
public class ObjectDetectionHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(ObjectDetectionHandler.class);
    private ObjectDetectionConfiguration config = new ObjectDetectionConfiguration();
    private final HttpClient httpClient;
    String classifierName = "haarcascade_eye";
    CascadeClassifier classifier = new CascadeClassifier(classifierName);

    public ObjectDetectionHandler(Thing thing) {
        super(thing);
        httpClient = new HttpClient();
        httpClient.setConnectTimeout(5000);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (CHANNEL_1.equals(channelUID.getId())) {
            if (command instanceof RefreshType) {
                // TODO: handle data refresh
            }
        }
    }

    private void snapshot(final URI uri) {
        // Try to get image from camera
        Request request = httpClient.newRequest(uri);
        request.accept("image/jpeg");
        ContentResponse response;
        try {
            response = request.send();

            // Error occurred?
            if (response.getStatus() != HttpStatus.OK_200) {
                throw new HttpResponseException(response.getReason(), response);
            }

            // Get image from HTTP response
            byte[] imageData = response.getContent();

            // Publish raw image?
            // if (isLinked(CHANNEL_IMAGE)) {
            // updateState(imageChannelUID, new RawType(imageData, "image/jpeg"));
            // }

            // Convert image to gray-scale
            Mat imageDataMat = new Mat(imageData);
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
        }
        // Mat grayScaleImage = imdecode(imageDataMat, CV_LOAD_IMAGE_GRAYSCALE);

        // Detect face in image
        RectVector faces = new RectVector();
        // classifier.detectMultiScale(grayScaleImage, faces);

        // Try to find known face in image
        for (int i = 0; i < faces.size(); i++) {

            // Extract face from image
            // Rect faceRect = faces.get(i);
            // Mat face = new Mat(grayScaleImage, faceRect);

            IntPointer label = new IntPointer(1);
            DoublePointer score = new DoublePointer(1);

            File file = new File("/var/lib/openhab2/etc/org.openhab.binding.facerecognition.models.xml");
            boolean facesTrained = file.exists();

            // Faces trained yet?
            if (facesTrained) {

                // Try to recognize user
                // classifier.predict(face, label, score);
            } else {
                logger.info("No faces trained yet");

                // Set default unknown user values
                // label.put(UNKNOWN_USER_LABEL);
                // score.put(Double.MAX_VALUE);
            }
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        config = getConfigAs(ObjectDetectionConfiguration.class);
        updateStatus(ThingStatus.ONLINE);
        classifier = new CascadeClassifier(classifierName);
        // url = new URL(
        // "https://raw.github.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_alt.xml");
        // classifierName = file.getAbsolutePath();

        // We can "cast" Pointer objects by instantiating a new object of the desired class.
        // CascadeClassifier classifier = new CascadeClassifier(classifierName);
        // if (classifier == null) {
        // logger.warn("Error loading classifier file {}", classifierName);
        // }

        // Read configuration
        try {
            URI camera = new URI("http://192.168.1.82:8080/ipcamera/192168160/ipcamera.jpg");
        } catch (URISyntaxException e1) {
        }
        // String username = (String) getConfig().get("username");
        // String password = (String) getConfig().get("password");
        // interval = (BigDecimal) getConfig().get("interval");
        // threshold = (BigDecimal) getConfig().get("threshold");

        // Load models file
        File file = new File(
                "https://raw.github.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_alt.xml");

        if (file.exists()) {
            classifier.load(
                    "https://raw.github.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_alt.xml");
        }

        // Load face cascade
        classifier = new CascadeClassifier(classifierName);

        // Add credentials for HTTP Digest and Basic authentication
        // AuthenticationStore auth = httpClient.getAuthenticationStore();
        // auth.clearAuthentications();
        // auth.addAuthentication(new BasicAuthentication(camera, Authentication.ANY_REALM, username, password));
        // auth.addAuthentication(new DigestAuthentication(camera, Authentication.ANY_REALM, username, password));

        try {
            httpClient.start();
        } catch (java.lang.Exception e) {
        }

        // Start making periodically images
        // job = service.scheduleAtFixedRate(() -> snapshot(camera), 0, interval.longValue(),
        // TimeUnit.MILLISECONDS);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing {}::{}", getThing().getLabel(), getThing().getUID());

        try {
            httpClient.stop();
        } catch (java.lang.Exception e) {
            logger.error("{}", e.getMessage());
        }

        classifier.close();
    }
}
