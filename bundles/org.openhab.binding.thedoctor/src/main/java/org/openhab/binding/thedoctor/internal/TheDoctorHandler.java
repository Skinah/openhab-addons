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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    private long days = 0;
    private long refHeap = 0;
    private long maxHeap = 0;
    private long maxRam = 0;
    private double maxTemp = 0;
    SystemInfo systemInfo = new SystemInfo();

    public TheDoctorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    private long checkHeap(boolean clean) {
        if (clean) {
            System.gc();
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
        // maxMemory is the -Xmx value, totalMemory is the heaps allocated size which can be lower.
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 100
                / Runtime.getRuntime().maxMemory();
    }

    private void longCheck() {
        long cleanedHeap = checkHeap(true);
        if (refHeap == 0) {
            refHeap = cleanedHeap;
            if (cleanedHeap < 20) {
                logger.info(
                        "You have a bigger heap size then needed as it is only {}% full after garbage collection. Consider decreasing your Java -Xmx size",
                        cleanedHeap);
            }
        } else if (cleanedHeap > refHeap + 4) {
            logger.warn("Heap has increased from {} to {} and may indicate a memory leak if this number keeps growing",
                    refHeap, cleanedHeap);
        }
        updateState(CHANNEL_CLEANED_HEAP, new QuantityType<>(cleanedHeap, Units.PERCENT));
        LocalDateTime now = LocalDateTime.now();
        long diff = ChronoUnit.DAYS.between(startDateTime, now);
        if (diff > days) {
            days = diff;
            // reset the max variables so each day you get the warnings again
            maxHeap = 0;
            maxRam = 0;
            maxTemp = 0;
        }
    }

    private void checkHealth() {
        long totalPhysicalMemory = systemInfo.getHardware().getMemory().getTotal();
        long availableMemory = systemInfo.getHardware().getMemory().getAvailable();
        long ram = (totalPhysicalMemory - availableMemory) * 100 / totalPhysicalMemory;// % Used
        long heap = checkHeap(false);
        if (heap > maxHeap) {
            maxHeap = heap;
            if (heap > 95) {
                logger.warn("BAD : Heap is {}% full, consider increasing your Java -Xmx size", heap);
            }
        }
        logger.debug("GOOD: Heap is only {}% full, and ranges from {}% to {}%", heap, refHeap, maxHeap);
        if (ram > 80) {
            if (ram > maxRam) {
                maxRam = ram;
                if (maxHeap < 80) {
                    logger.info(
                            "BAD : RAM is now {}% full but you have a bigger heap size then is needed, consider decreasing your Java -Xmx size",
                            ram);
                } else {
                    logger.info("BAD : RAM is now {}% full", ram);
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
                logger.warn("BAD : CPU temperature is {} and may cause instability. Do you have a heatsink and fan?",
                        temperature);
            } else {
                logger.debug("BAD : CPU temperature is {} and may cause instability. Do you have a heatsink and fan?",
                        temperature);
            }
        } else {
            logger.debug("GOOD: CPU temperature is {}", temperature);
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
        updateStatus(ThingStatus.ONLINE);
        ProcessorIdentifier processorIdentifier = systemInfo.getHardware().getProcessor().getProcessorIdentifier();
        logger.info("The processor is :{}", processorIdentifier.getIdentifier());
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
