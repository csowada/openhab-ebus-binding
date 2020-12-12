/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.ebus.internal.services;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.*;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.ebus.internal.handler.IEBusBridgeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.core.IEBusController;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusMetricsService {

    private final Logger logger = LoggerFactory.getLogger(EBusMetricsService.class);

    @Nullable
    private ScheduledFuture<?> metricsRefreshSchedule;

    private IEBusBridgeHandler bridge;

    public EBusMetricsService(IEBusBridgeHandler bridge) {
        this.bridge = bridge;
    }

    private EBusClient getBackendClient() {
        return bridge.getLibClient().getClient();
    }

    public void deactivate() {
        ScheduledFuture<?> metricsRefreshSchedule = this.metricsRefreshSchedule;
        if (metricsRefreshSchedule != null) {
            metricsRefreshSchedule.cancel(true);
            this.metricsRefreshSchedule = null;
        }
    }

    public void activate() {

        deactivate();

        metricsRefreshSchedule = bridge.getBindingScheduler().scheduleWithFixedDelay(new Runnable() {

            @Override
            public void run() {
                try {
                    de.csdev.ebus.service.metrics.EBusMetricsService metricsService = getBackendClient()
                            .getMetricsService();
                    IEBusController controller = getBackendClient().getController();

                    ThingUID thingUID = bridge.getThing().getUID();

                    bridge.updateState(new ChannelUID(thingUID, METRICS, RECEIVED_TELEGRAMS),
                            new DecimalType(metricsService.getReceived()));
                    bridge.updateState(new ChannelUID(thingUID, METRICS, FAILED_TELEGRAMS),
                            new DecimalType(metricsService.getFailed()));
                    bridge.updateState(new ChannelUID(thingUID, METRICS, RESOLVED_TELEGRAMS),
                            new DecimalType(metricsService.getResolved()));
                    bridge.updateState(new ChannelUID(thingUID, METRICS, UNRESOLVED_TELEGRAMS),
                            new DecimalType(metricsService.getUnresolved()));
                    bridge.updateState(new ChannelUID(thingUID, METRICS, FAILED_RATIO),
                            new DecimalType(metricsService.getFailureRatio()));
                    bridge.updateState(new ChannelUID(thingUID, METRICS, UNRESOLVED_RATIO),
                            new DecimalType(metricsService.getUnresolvedRatio()));

                    if (controller != null) {
                        bridge.updateState(new ChannelUID(thingUID, METRICS, SEND_RECEIVE_ROUNDTRIP_TIME),
                                new DecimalType((int) controller.getLastSendReceiveRoundtripTime() / 1000));
                    }

                } catch (Exception e) {
                    logger.error("error!", e);
                }
            }
        }, 0, 30, TimeUnit.SECONDS);
    }
}
