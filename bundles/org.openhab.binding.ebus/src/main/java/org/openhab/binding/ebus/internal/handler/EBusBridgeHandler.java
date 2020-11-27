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
package org.openhab.binding.ebus.internal.handler;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ebus.action.EBusActions;
import org.openhab.binding.ebus.internal.EBusBindingConstants;
import org.openhab.binding.ebus.internal.EBusBridgeHandlerConfiguration;
import org.openhab.binding.ebus.internal.EBusHandlerFactory;
import org.openhab.binding.ebus.internal.services.EBusMetricsService;
import org.openhab.binding.ebus.internal.things.IEBusTypeProvider;
import org.openhab.binding.ebus.internal.utils.EBusAdvancedLogging;
import org.openhab.binding.ebus.internal.utils.EBusClientBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.core.IEBusConnectorEventListener;
import de.csdev.ebus.core.IEBusController.ConnectionStatus;
import de.csdev.ebus.service.parser.IEBusParserListener;
import de.csdev.ebus.utils.EBusUtils;

/**
 * The {@link EBusBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBridgeHandler extends EBusBaseBridgeHandler
        implements IEBusParserListener, IEBusConnectorEventListener {

    private final Logger logger = LoggerFactory.getLogger(EBusBridgeHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections
            .singleton(EBusBindingConstants.THING_TYPE_EBUS_BRIDGE);

    private EBusClientBridge clientBridge;

    @NonNull
    private EBusHandlerFactory handlerFactory;

    private EBusAdvancedLogging advanceLogger;

    private EBusBridgeHandlerConfiguration configuration;

    private EBusMetricsService metricsService;

    @Override
    public Collection<@NonNull Class<? extends @NonNull ThingHandlerService>> getServices() {
        return Collections.singleton(EBusActions.class);
    }

    public EBusBridgeHandler(@NonNull Bridge bridge, IEBusTypeProvider typeProvider,
            EBusHandlerFactory handlerFactory) {

        super(bridge);

        // reference configuration
        this.handlerFactory = handlerFactory;
    }

    /**
     * Returns the eBUS core lib client
     *
     * @return
     */
    @Override
    @NonNull
    public EBusClientBridge getLibClient() {
        return clientBridge;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#initialize()
     */
    @Override
    public void initialize() {

        logger.trace("EBusBridgeHandler.initialize()");

        IEBusTypeProvider typeProvider = this.handlerFactory.getEBusTypeProvider();
        configuration = getConfigAs(EBusBridgeHandlerConfiguration.class);

        if (typeProvider == null) {
            throw new RuntimeException("Type provider not available!");
        }

        // initialize the ebus client wrapper
        EBusCommandRegistry registry = typeProvider.getCommandRegistry();

        if (registry == null) {
            throw new RuntimeException("Command Registry not available!");
        }

        clientBridge = new EBusClientBridge(registry);

        // add the discovery service
        handlerFactory.disposeDiscoveryService(this);
        handlerFactory.registerDiscoveryService(this);

        String ipAddress = null;
        BigDecimal port = null;
        String networkDriver = DRIVER_RAW;

        String serialPort = null;
        String serialPortDriver = DRIVER_NRJAVASERIAL;

        String masterAddressStr = null;

        @Nullable
        Byte masterAddress = (byte) 0xFF;

        try {
            ipAddress = configuration.ipAddress;
            port = configuration.port;
            networkDriver = configuration.networkDriver;

            masterAddressStr = configuration.masterAddress;
            serialPort = configuration.serialPort;
            serialPortDriver = configuration.serialPortDriver;

            if (configuration.advancedLogging.equals(Boolean.TRUE)) {
                logger.warn("Enable advanced logging for eBUS commands!");
                advanceLogger = new EBusAdvancedLogging();
            }

        } catch (Exception e) {
            logger.warn("Cannot set parameters!", e);
        }

        if (StringUtils.isNotEmpty(masterAddressStr)) {
            masterAddress = EBusUtils.toByte(masterAddressStr);
        }

        if (StringUtils.isNotEmpty(ipAddress) && port != null) {

            // use ebusd as high level driver
            if (networkDriver.equals(DRIVER_EBUSD) && ipAddress != null) {
                clientBridge.setEbusdConnection(ipAddress, port.intValue());
            } else if (ipAddress != null) {
                clientBridge.setTCPConnection(ipAddress, port.intValue());
            }
        }

        if (StringUtils.isNotEmpty(serialPort) && serialPort != null) {
            clientBridge.setSerialConnection(serialPort, serialPortDriver);
        }

        if (masterAddress != null && !EBusUtils.isMasterAddress(masterAddress)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "eBUS master address is not a valid master address!");

            return;
        }

        if (!clientBridge.isConnectionValid()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Network address and Port either Serial Port must be set!");

            return;
        }

        if (masterAddress == null) {
            throw new RuntimeException("Invalid (null) master address!");
        }

        clientBridge.initClient(masterAddress);

        // add before other listeners, better to read in logs
        if (advanceLogger != null) {
            clientBridge.getClient().addEBusParserListener(advanceLogger);
        }

        // add listeners
        clientBridge.getClient().addEBusEventListener(this);
        clientBridge.getClient().addEBusParserListener(this);

        // startMetricScheduler();
        metricsService = new EBusMetricsService(this);
        metricsService.activate();

        // start eBus controller
        clientBridge.startClient();
    }

    @Override
    public void dispose() {

        logger.trace("EBusBridgeHandler.dispose()");

        if (metricsService != null) {
            metricsService.deactivate();
            metricsService = null;
        }

        if (advanceLogger != null) {
            clientBridge.getClient().removeEBusParserListener(advanceLogger);
            advanceLogger.close();
            advanceLogger = null;
        }

        // remove discovery service
        handlerFactory.disposeDiscoveryService(this);

        if (clientBridge != null) {

            clientBridge.stopClient();
            clientBridge.getClient().dispose();

            clientBridge = null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * de.csdev.ebus.service.parser.IEBusParserListener#onTelegramResolved(de.csdev.ebus.command.IEBusCommandMethod,
     * java.util.Map, byte[], java.lang.Integer)
     */
    @Override
    public void onTelegramResolved(IEBusCommandMethod commandChannel, Map<String, Object> result, byte[] receivedData,
            Integer sendQueueId) {

        boolean noHandler = true;

        String source = EBusUtils.toHexDumpString(receivedData[0]);
        String destination = EBusUtils.toHexDumpString(receivedData[1]);

        logger.debug("Received telegram from address {} to {} with command {}", source, destination,
                commandChannel.getParent().getId());

        if (!this.isInitialized()) {
            logger.warn("eBUS bridge is not initialized! Unable to process resolved telegram!");
            return;
        }

        // loop over all child nodes
        for (Thing thing : getThing().getThings()) {

            EBusHandler handler = (EBusHandler) thing.getHandler();

            if (handler != null) {

                // check if this handler can process this telegram
                if (handler.supportsTelegram(receivedData, commandChannel)) {

                    // process
                    handler.handleReceivedTelegram(commandChannel, result, receivedData, sendQueueId);
                    noHandler = false;
                }
            }
        }

        if (noHandler) {
            logger.debug("No handler has accepted the command {} from {} to {} ...", commandChannel.getParent().getId(),
                    source, destination);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.IEBusConnectorEventListener#onTelegramException(de.csdev.ebus.core.EBusDataException,
     * java.lang.Integer)
     */
    @Override
    public void onTelegramException(EBusDataException exception, Integer sendQueueId) {
        logger.debug("eBUS telegram error; {}", exception.getLocalizedMessage());
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.IEBusConnectorEventListener#onConnectionException(java.lang.Exception)
     */
    @Override
    public void onConnectionException(Exception e) {

        metricsService.deactivate();

        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.smarthome.core.thing.binding.ThingHandler#handleCommand(org.eclipse.smarthome.core.thing.ChannelUID,
     * org.eclipse.smarthome.core.types.Command)
     */
    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, Command command) {
        // noop for bridge
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.IEBusConnectorEventListener#onTelegramReceived(byte[], java.lang.Integer)
     */
    @Override
    public void onTelegramReceived(byte[] receivedData, Integer sendQueueId) {
        Bridge bridge = getThing();

        if (bridge.getStatus() != ThingStatus.ONLINE) {

            // bring the bridge back online
            updateStatus(ThingStatus.ONLINE);

            // start the metrics scheduler
            metricsService.activate();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.service.parser.IEBusParserListener#onTelegramResolveFailed(de.csdev.ebus.command.
     * IEBusCommandMethod, byte[], java.lang.Integer, java.lang.String)
     */
    @Override
    public void onTelegramResolveFailed(IEBusCommandMethod commandChannel, byte[] receivedData, Integer sendQueueId,
            String exceptionMessage) {

        if (commandChannel == null) {
            if (logger.isTraceEnabled()) {
                logger.trace("Unknown telegram {}", EBusUtils.toHexDumpString(receivedData));
            }
        } else {
            logger.warn("Resolve error '{}' in {} from {} [data:{}]", exceptionMessage,
                    commandChannel.getParent().getLabel(), commandChannel.getParent().getParentCollection().getLabel(),
                    EBusUtils.toHexDumpString(receivedData));
        }
    }

    @Override
    public void onConnectionStatusChanged(ConnectionStatus status) {

        Bridge bridge = getThing();
        ThingStatus thingStatus = bridge.getStatus();

        if (status == ConnectionStatus.DISCONNECTED && thingStatus != ThingStatus.OFFLINE) {
            updateStatus(ThingStatus.OFFLINE);

        } else if (status == ConnectionStatus.CONNECTING && thingStatus != ThingStatus.UNKNOWN) {
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.NONE, "Connecting to eBUS ...");

        } else if (status == ConnectionStatus.CONNECTED && thingStatus != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);

        }
    }
}
