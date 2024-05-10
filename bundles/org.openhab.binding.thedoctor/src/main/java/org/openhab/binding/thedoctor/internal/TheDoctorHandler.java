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
package org.openhab.binding.thedoctor.internal;

import static org.openhab.binding.thedoctor.internal.TheDoctorBindingConstants.CHANNEL_CLEANED_HEAP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;

/**
 * The {@link TheDoctorHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Matthew Skinner - Initial contribution
 */
@NonNullByDefault
public class TheDoctorHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(TheDoctorHandler.class);
    private TheDoctorConfiguration config = new TheDoctorConfiguration();
    private @Nullable ScheduledFuture<?> pollingFuture = null;
    private @Nullable ScheduledFuture<?> longPollingFuture = null;
    private LocalDateTime startDateTime = LocalDateTime.now();
    private boolean linux = false;
    private boolean macOS = false;
    private boolean windows = false;
    private boolean rasberry = false;
    private long leakValueWarned = 0;
    private long days = 0;
    private long refHeap = 0;
    private long maxHeap = 0;
    private long maxRam = 0;
    private double maxTemp = 0;
    SystemInfo systemInfo = new SystemInfo();

    public TheDoctorHandler(Thing thing) {
        super(thing);
    }

    private class CommandThread extends Thread {
        private @Nullable Process process = null;
        private List<String> commandArrayList = new ArrayList<>();

        CommandThread(String command) {
            Collections.addAll(commandArrayList, command.trim().split("\\s+"));
            setDaemon(true);
            run();
        }

        @Override
        public void run() {
            try {
                process = Runtime.getRuntime().exec(commandArrayList.toArray(new String[commandArrayList.size()]));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line = null;
                while ((line = errorReader.readLine()) != null) {
                    logger.trace("Error:{}", line);
                }
                // Read the output from the command
                while ((line = outputReader.readLine()) != null) {
                    logger.trace("{}", line);
                }
            } catch (IOException e) {
                logger.warn("An IO error occurred trying to start FFmpeg: {}", e.getMessage());
            } finally {
                Process localProcess = process;
                if (localProcess != null) {
                    localProcess.destroyForcibly();
                    process = null;
                }
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    private long checkHeap(boolean clean) {
        if (clean) {
            System.gc();
        }
        // try {
        // Thread.sleep(500);
        // } catch (InterruptedException e) {
        // }
        // maxMemory is the -Xmx value, totalMemory is the heaps allocated size which can be lower.
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100
                / Runtime.getRuntime().maxMemory();
    }

    private void longCheck() {
        long cleanedHeap = checkHeap(true);
        if (refHeap == 0) { // first run only
            refHeap = cleanedHeap;
            if (cleanedHeap < 18) {
                logger.info(
                        "You have a bigger heap size then needed as it is only {}% full after garbage collection. Consider decreasing your Java -Xmx size unless you are still adding new things and bindings.",
                        cleanedHeap);
            }
        } else if (cleanedHeap > refHeap + 5 && cleanedHeap > leakValueWarned) {
            logger.warn(
                    "Heap has increased from {} to {} and may indicate a memory leak if this number keeps growing. This binding has a channel you can use to watch the heap with.",
                    refHeap, cleanedHeap);
            leakValueWarned = cleanedHeap;
        }
        updateState(CHANNEL_CLEANED_HEAP, new QuantityType<>(cleanedHeap, Units.PERCENT));
        LocalDateTime now = LocalDateTime.now();
        int processes = systemInfo.getOperatingSystem().getProcessCount();
        int threads = Thread.activeCount();
        logger.debug("Process/Task count {}", processes);
        logger.debug("Thread count {}", threads);
        long diff = ChronoUnit.DAYS.between(startDateTime, now);
        if (diff > days) {// Another 24 hours has passed since binding started.
            days = diff;
            // reset the max variables so each day you get the warnings again
            maxHeap = 0;
            maxRam = 0;
            maxTemp = 0;
        }
    }

    private void checkPiHealth() {
        Path filePath = Path.of("/sys/devices/platform/soc/soc:firmware/get_throttled");
        try {
            String content = Files.readString(filePath);
            int code = Integer.decode("0x" + content.strip());

            if (code != 0) {
                logger.warn("BAD : Full Pi throttle code is {}", code);
                if ((code & (1L << 0)) != 0) {
                    logger.warn(
                            "BAD : Your Pi's power supply or cable is not good enough to supply power without an under-voltage event occuring.");
                }
                if ((code & (1L << 1)) != 0) {
                    logger.warn("BAD : Pi, Arm frequency capped");
                }
                if ((code & (1L << 2)) != 0) {
                    logger.warn("BAD : Pi, Currently throttled");
                }
                if ((code & (1L << 3)) != 0) {
                    logger.warn("BAD : Pi, Soft temperature limit active");
                }
                if ((code & (1L << 16)) != 0) {
                    logger.warn(
                            "BAD : Your Pi's power supply or cable was not good enough to supply power without an under-voltage event occuring.");
                }
                if ((code & (1L << 17)) != 0) {
                    logger.warn("BAD : Pi, Arm frequency cap has occurred ");
                }
                if ((code & (1L << 18)) != 0) {
                    logger.warn("BAD : Pi, Throttling has occurred");
                }
                if ((code & (1L << 19)) != 0) {
                    logger.warn("BAD : Pi, Soft temperature limit has occurred");
                }
            } else {
                logger.debug("GOOD: Pi is not reporting any current throttle conditions.");
            }

        } catch (IOException e) {
            logger.debug("BAD : Could not read {}", filePath);
        }
    }

    private void checkHealth() {
        if (rasberry && linux) {
            checkPiHealth();
        }
        long totalPhysicalMemory = systemInfo.getHardware().getMemory().getTotal();
        long availableMemory = systemInfo.getHardware().getMemory().getAvailable();
        long ram = (totalPhysicalMemory - availableMemory) * 100 / totalPhysicalMemory;// % Used
        long heap = checkHeap(false);
        if (heap > maxHeap) {
            maxHeap = heap;
            if (heap > 98) {
                if (refHeap > 50) {
                    logger.warn(
                            "BAD : Heap is {}% full, consider increasing your Java -Xmx size as your cleaned heap is {}%",
                            heap, refHeap);
                } else {
                    logger.debug("BAD : Heap is {}% full, consider increasing your Java -Xmx size if this occurs often",
                            heap);
                }
            } else {
                logger.debug("GOOD: Heap is only {}% full, and ranges from {}% to {}%", heap, refHeap, maxHeap);
            }
        } else {
            logger.debug("GOOD: Heap is only {}% full, and ranges from {}% to {}%", heap, refHeap, maxHeap);
        }

        if (ram > 80) {
            if (ram > maxRam) {
                maxRam = ram;
                if (maxHeap < 80) {
                    logger.warn(
                            "BAD : RAM is now {}% full, but you can improve that by decreasing your Java -Xmx size as your heap appears to be larger than needed.",
                            ram);
                } else {
                    logger.warn("BAD : RAM is now {}% full", ram);
                }
            } else {
                logger.debug("BAD : RAM is now {}% full", ram);
            }
        } else {
            logger.debug("GOOD: RAM is {}% full", ram);
        }

        double temperature = systemInfo.getHardware().getSensors().getCpuTemperature();
        // throttles occur at pi3 70, pi4 82
        if (temperature > 60) {
            if (temperature > maxTemp) {
                maxTemp = temperature;
                logger.warn("BAD : CPU temperature is {}c and may cause instability. Do you have a heatsink and fan?",
                        temperature);
            } else {
                logger.debug("BAD : CPU temperature is {}c and may cause instability. Do you have a heatsink and fan?",
                        temperature);
            }
        } else {
            logger.debug("GOOD: CPU temperature is {}c", temperature);
        }
        double cpuVoltage = systemInfo.getHardware().getSensors().getCpuVoltage();
        if (cpuVoltage > 0) {
            logger.debug("GOOD: CPU voltage is {}Volts", cpuVoltage);
        }
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        config = getConfigAs(TheDoctorConfiguration.class);
        ProcessorIdentifier processorIdentifier = systemInfo.getHardware().getProcessor().getProcessorIdentifier();
        if (processorIdentifier.getModel().contains("Raspberry Pi")) {
            rasberry = true;
            logger.info("Will include health checks for your:{}", processorIdentifier.getModel());// cat
                                                                                                  // /proc/cpuinfo
        } else {
            logger.info("The CPU model is:{}", processorIdentifier.getModel());// cat /proc/cpuinfo
        }
        String os = System.getProperty("os.name");
        if (os != null) {
            switch (os) {
                case "Linux":
                    linux = true;
                    break;
                case "OS X":
                case "macOS":
                case "Mac OS X":
                    macOS = true;
                    break;
                case "Windows":
                    windows = true;
                default:
                    logger.info("Unknown OS:{}, please report this message so your OS can be handled correctly", os);
            }
        }
        updateStatus(ThingStatus.ONLINE);
        pollingFuture = scheduler.scheduleWithFixedDelay(this::checkHealth, 0, config.refresh, TimeUnit.SECONDS);
        longPollingFuture = scheduler.scheduleWithFixedDelay(this::longCheck, 1, 30, TimeUnit.MINUTES);
    }

    @Override
    public void dispose() {
        Future<?> future = pollingFuture;
        if (future != null) {
            future.cancel(true);
            pollingFuture = null;
        }
        future = longPollingFuture;
        if (future != null) {
            future.cancel(true);
            longPollingFuture = null;
        }
    }
}
