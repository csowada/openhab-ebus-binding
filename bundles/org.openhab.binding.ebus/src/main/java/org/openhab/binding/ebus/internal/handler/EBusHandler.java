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
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.quantity.Temperature;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.ebus.internal.EBusHandlerConfiguration;
import org.openhab.binding.ebus.internal.utils.EBusBindingUtils;
import org.openhab.binding.ebus.internal.utils.EBusClientBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.EBusConsts;
import de.csdev.ebus.core.EBusControllerException;
import de.csdev.ebus.core.IEBusController.ConnectionStatus;
import de.csdev.ebus.utils.EBusDateTime;
import de.csdev.ebus.utils.EBusUtils;

/**
 * The {@link EBusHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(EBusHandler.class);

    private Map<ChannelUID, ByteBuffer> channelPollings = new HashMap<>();

    private Random random = new Random(12);

    private Map<ByteBuffer, ScheduledFuture<?>> uniqueTelegramPollings = new HashMap<>();

    private @Nullable EBusHandlerConfiguration configuration;

    /**
     * @param thing
     */
    public EBusHandler(Thing thing) {
        super(thing);
    }

    /**
     * Assign a value to a channel.
     *
     * @param channel
     * @param value
     */
    private void assignValueToChannel(Channel channel, @Nullable Object value) {

        String acceptedItemType = channel.getAcceptedItemType();

        State state = null;

        if (StringUtils.equals(acceptedItemType, ITEM_TYPE_NUMBER)) {
            if (value instanceof BigDecimal) {
                state = new DecimalType((BigDecimal) value);
            }

        } else if (StringUtils.equals(acceptedItemType, ITEM_TYPE_TEMPERATURE)) {
            if (value instanceof BigDecimal) {
                state = new QuantityType<Temperature>((BigDecimal) value, SIUnits.CELSIUS);
            }

        } else if (StringUtils.equals(acceptedItemType, ITEM_TYPE_STRING)) {
            if (value instanceof BigDecimal) {
                state = new StringType(((BigDecimal) value).toString());

            } else if (value instanceof String) {
                state = new StringType((String) value);

            } else if (value instanceof byte[]) {
                // show bytes as hex string
                state = new StringType(EBusUtils.toHexDumpString((byte[]) value).toString());
            }

        } else if (StringUtils.equals(acceptedItemType, ITEM_TYPE_SWITCH)) {
            if (value instanceof Boolean) {
                boolean isOn = ((Boolean) value).booleanValue();
                state = isOn ? OnOffType.ON : OnOffType.OFF;
            }

        } else if (StringUtils.equals(acceptedItemType, ITEM_TYPE_DATETIME)) {

            if (value instanceof EBusDateTime) {
                Calendar calendar = ((EBusDateTime) value).getCalendar();
                ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(calendar.toInstant(),
                        TimeZone.getDefault().toZoneId());

                state = new DateTimeType(zonedDateTime);
            }
        }

        if (state == null) {
            if (value == null) {
                state = UnDefType.NULL;
            } else {
                logger.warn("Unexpected datatype {} for channel {} [accepted type: {}] !",
                        value.getClass().getSimpleName(), channel.getChannelTypeUID(), acceptedItemType);
                state = UnDefType.UNDEF;
            }
        }

        updateState(channel.getUID(), state);
    }

    @Override
    public void channelLinked(@NonNull ChannelUID channelUID) {
        super.channelLinked(channelUID);

        logger.trace("channelLinked {}", channelUID);
        initializeChannelPolling(channelUID);
    }

    @Override
    public void channelUnlinked(@NonNull ChannelUID channelUID) {
        super.channelUnlinked(channelUID);

        logger.trace("channelUnlinked {}", channelUID);
        disposeChannelPolling(channelUID);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {

        // Cancel all polling jobs
        for (Entry<ByteBuffer, ScheduledFuture<?>> entry : uniqueTelegramPollings.entrySet()) {
            logger.info("Remove polling job for {}", EBusUtils.toHexDumpString(entry.getKey()));
            entry.getValue().cancel(true);
        }

        uniqueTelegramPollings.clear();
        channelPollings.clear();
    }

    /**
     * @param channelUID
     */
    private void disposeChannelPolling(ChannelUID channelUID) {

        if (channelPollings.containsKey(channelUID)) {
            ByteBuffer telegram = channelPollings.remove(channelUID);

            if (!channelPollings.containsValue(telegram)) {
                // remove last
                ScheduledFuture<?> future = uniqueTelegramPollings.remove(telegram);
                future.cancel(true);

                logger.debug("Cancel polling job for \"{}\" ...", channelUID);
            } else {
                logger.debug("Polling job still in use for \"{}\" ...", channelUID);
            }
        }
    }

    /**
     * @param channel
     * @return
     */
    private long getChannelPollingInterval(Channel channel) {

        final Map<@NonNull String, @NonNull String> properties = channel.getProperties();
        final Configuration thingConfiguration = thing.getConfiguration();
        final Configuration configuration = channel.getConfiguration();

        final String valueName = properties.get(VALUE_NAME);

        // a valid value for polling
        long pollingPeriod = 0;
        if (configuration.get(POLLING) instanceof Number) {
            pollingPeriod = ((Number) configuration.get(POLLING)).longValue();
        }

        // overwrite with global polling if not set
        if (pollingPeriod == 0) {
            // is global polling for this thing enabled?
            if (thingConfiguration.get(POLLING) instanceof Number) {
                pollingPeriod = ((Number) thingConfiguration.get(POLLING)).longValue();
            }
        }

        // skip channels starting with _ , this is a ebus command that starts with _
        if (StringUtils.startsWith(valueName, "_")) {
            pollingPeriod = 0l;
        }

        // only for linked channels
        if (!isLinked(channel.getUID())) {
            pollingPeriod = 0l;
        }

        return pollingPeriod;
    }

    /**
     * Generates the raw telegram for a channel
     *
     * @param channel
     * @return
     */
    @Nullable
    private ByteBuffer getChannelTelegram(Channel channel) {

        final EBusClientBridge libClient = getLibClient();

        final Map<@NonNull String, @NonNull String> properties = channel.getProperties();
        final String collectionId = thing.getThingTypeUID().getId();

        // final String collectionId = properties.get(COLLECTION);
        final String commandId = properties.get(COMMAND);

        try {
            return libClient.generatePollingTelegram(collectionId, commandId, IEBusCommandMethod.Method.GET, thing);

        } catch (EBusTypeException e) {
            logger.error("error!", e);
        }

        return null;
    }

    /**
     * Returns the eBUS core client
     *
     * @return
     */
    private EBusClientBridge getLibClient() {

        Bridge bridge = getBridge();
        if (bridge == null) {
            throw new RuntimeException("No eBUS bridge defined!");
        }

        EBusBridgeHandler handler = (EBusBridgeHandler) bridge.getHandler();
        if (handler != null) {
            return handler.getLibClient();
        }

        throw new RuntimeException("Unabke to get a eBUS Client from Backend");
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

        if (!(command instanceof RefreshType)) {
            Channel channel = thing.getChannel(channelUID.getId());

            if (channel != null) {
                try {
                    ByteBuffer telegram = getLibClient().generateSetterTelegram(thing, channel, command);
                    if (telegram != null) {
                        getLibClient().sendTelegram(telegram);
                    }
                } catch (EBusTypeException | EBusControllerException e) {
                    logger.error("error!", e);
                }

            }
        }
    }

    /**
     * Processes the received telegram with this handler.
     *
     * @param commandChannel
     * @param result
     * @param receivedData
     * @param sendQueueId
     */
    public void handleReceivedTelegram(IEBusCommandMethod commandChannel, Map<String, Object> result,
            byte[] receivedData, @Nullable Integer sendQueueId) {

        logger.debug("Handle received command by thing {} with id {} ...", thing.getLabel(), thing.getUID());

        for (Entry<String, Object> resultEntry : result.entrySet()) {

            logger.trace("Key {} with value {}", resultEntry.getKey(), resultEntry.getValue());

            ChannelUID channelUID = EBusBindingUtils.generateChannelUID(commandChannel.getParent(),
                    resultEntry.getKey(), thing.getUID());

            Channel channel = thing.getChannel(channelUID.getId());

            if (channel == null) {
                logger.debug("Unable to find the channel with channelUID {}", channelUID.getId());
                return;
            }

            assignValueToChannel(channel, resultEntry.getValue());
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.smarthome.core.thing.binding.BaseThingHandler#initialize()
     */
    @SuppressWarnings("null")
    @Override
    public void initialize() {

        configuration = getConfigAs(EBusHandlerConfiguration.class);

        Bridge bridge = getBridge();
        if (bridge == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge defined!");

        } else if (configuration != null && StringUtils.isEmpty(configuration.slaveAddress)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Slave address is not set!");

        } else if (bridge.getStatus() != ThingStatus.ONLINE) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);

        } else {
            updateStatus(ThingStatus.ONLINE);
            updateHandler();
        }
    }

    /**
     * Initialize the polling for a channel if used
     *
     * @param channelUID
     */
    private void initializeChannelPolling(ChannelUID channelUID) {

        Channel channel = thing.getChannel(channelUID.getId());

        if (channel == null) {
            return;
        }

        long pollingPeriod = getChannelPollingInterval(channel);

        // a valid value for polling ?
        if (pollingPeriod == 0) {
            return;
        }

        final EBusClientBridge libClient = getLibClient();
        final String commandId = channel.getProperties().get(COMMAND);

        if (StringUtils.isEmpty(commandId)) {
            logger.warn("Invalid channel uid {}", channelUID);
            logger.warn("Invalid channel {}", channel);
            return;
        }

        // compose the telegram
        final ByteBuffer telegram = getChannelTelegram(channel);

        // valid telegram ?
        if (telegram != null) {

            // polling for raw telegram already active?
            if (!channelPollings.containsValue(telegram)) {

                // random execution delay to prevent too many pollings at the same time (0-30s)
                int firstExecutionDelay = random.nextInt(30);

                // create a job to send this raw telegram every n seconds
                ScheduledFuture<?> job = scheduler.scheduleAtFixedRate(() -> {
                    logger.trace("Poll command \"{}\" with \"{}\" ...", channel.getUID(),
                            EBusUtils.toHexDumpString(telegram).toString());

                    try {
                        EBusClient client = libClient.getClient();
                        if (client.getController() != null
                                && client.getController().getConnectionStatus() == ConnectionStatus.CONNECTED) {
                            client.addToSendQueue(EBusUtils.toByteArray(telegram), 2);
                        } else {
                            logger.trace("Unable to send polling command due to a unconnected controller");
                        }

                    } catch (EBusControllerException e) {
                        logger.debug("Remove polling job for {} due to controller exception", channelUID);
                        this.disposeChannelPolling(channelUID);
                    }

                }, firstExecutionDelay, pollingPeriod, TimeUnit.SECONDS);

                // add this job to global list, so we can stop all later on.
                uniqueTelegramPollings.put(telegram, job);

                logger.info("Register polling for \"{}\" every {} sec. (initial delay {} sec.)", commandId,
                        pollingPeriod, firstExecutionDelay);
            } else {

                logger.info("Raw telegram already in use for polling, skip addition polling for \"{}\"!",
                        channel.getUID());
            }

            channelPollings.put(channel.getUID(), telegram);

        } else {
            logger.info("Unable to create raw polling telegram for \"{}\" !", commandId);
        }
    }

    /**
     * Refreshes the thing configuration
     *
     * @return
     */
    public boolean refreshThingConfiguration() {

        EBusClientBridge libClient = getLibClient();

        Map<@NonNull String, String> properties = thing.getProperties();
        String oldHash = properties.get("collectionHash");
        String collectionId = thing.getThingTypeUID().getId();
        IEBusCommandCollection collection = libClient.getClient().getCommandCollection(collectionId);

        if (StringUtils.isEmpty(collectionId)) {
            logger.error("Property \"collectionId\" not set for thing {}. Please re-create this thing.",
                    thing.getUID());
            return false;
        }

        if (collection == null) {
            logger.error(
                    "Unable to find configuration collection with id {}. It is possible that this collection has been renamed or removed from eBUS binding!",
                    collectionId);
            return false;
        }

        // new hash
        String newHash = EBusUtils.toHexDumpString(collection.getSourceHash()).toString();

        // check both hashs
        if (!StringUtils.equals(oldHash, newHash)) {
            logger.debug("eBUS configuration \"{}\"  has changed, update thing {} ...", collection.getId(),
                    thing.getUID());

            try {
                // just update the thing
                this.changeThingType(this.thing.getThingTypeUID(), this.thing.getConfiguration());

                // add the new hash
                this.updateProperty("collectionHash", newHash);

                return true;

            } catch (RuntimeException e) { // NOPMD - used in the openHAB core
                logger.error("Error: {}", e.getMessage());
            }
        }

        return false;
    }

    /**
     * Check if this handler supportes the given command. In this case this method returns true.
     *
     * @param receivedData
     * @param commandMethod
     * @return
     */
    @SuppressWarnings("null")
    public boolean supportsTelegram(byte[] receivedData, IEBusCommandMethod commandMethod) {

        final String collectionId = thing.getThingTypeUID().getId();
        if (!commandMethod.getParent().getParentCollection().getId().equals(collectionId)) {
            return false;
        }

        logger.trace("eBUS handler cfg {}", configuration);
        if (configuration == null) {
            return false;
        }

        byte sourceAddress = receivedData[0];
        byte destinationAddress = receivedData[1];

        Byte masterAddress = EBusUtils.toByte(configuration.masterAddress);
        Byte slaveAddress = EBusUtils.toByte(configuration.slaveAddress);

        boolean filterAcceptSource = BooleanUtils.isTrue(configuration.filterAcceptMaster);
        boolean filterAcceptDestination = BooleanUtils.isTrue(configuration.filterAcceptSlave);
        boolean filterAcceptBroadcast = BooleanUtils.isTrue(configuration.filterAcceptBroadcasts);

        // only interesting for broadcasts
        Byte masterAddressComp = masterAddress == null
                ? (slaveAddress != null ? EBusUtils.getMasterAddress(slaveAddress) : null)
                : masterAddress;

        // check if broadcast filter is set (default true)
        if (filterAcceptBroadcast && destinationAddress == EBusConsts.BROADCAST_ADDRESS) {
            if (masterAddressComp != null && sourceAddress == masterAddressComp) {
                return true;
            }
        }

        // check if source address filter is set
        if (filterAcceptSource && masterAddress != null) {
            if (masterAddress == sourceAddress) {
                return true;
            }
        }

        // check if destination address filter is set (default true)
        if (filterAcceptDestination) {

            if (EBusUtils.isMasterAddress(destinationAddress) && masterAddressComp != null
                    && destinationAddress == masterAddressComp) {
                // master-master telegram
                return true;

            } else if (slaveAddress != null && slaveAddress == destinationAddress) {
                // master-slave telegram
                return true;
            }
        }

        return false;
    }

    @Override
    public void thingUpdated(@NonNull Thing thing) {

        // Info: I expect no difference in the channel list without a restart!
        Thing currentThing = this.thing;

        this.thing = thing;

        for (Channel oldChannel : currentThing.getChannels()) {

            Channel newChannel = thing.getChannel(oldChannel.getUID().getId());
            logger.info("thingUpdated {}", oldChannel.getUID());
            if (newChannel != null
                    && !ObjectUtils.equals(oldChannel.getConfiguration(), newChannel.getConfiguration())) {
                logger.debug("Configuration for channel {} changed from {} to {} ...", oldChannel.getUID(),
                        oldChannel.getConfiguration(), newChannel.getConfiguration());
                updateChannelPolling(oldChannel.getUID());
            }
        }
    }

    /**
     * Update a channel and its polling
     *
     * @param channelUID
     */
    private void updateChannelPolling(ChannelUID channelUID) {
        disposeChannelPolling(channelUID);
        initializeChannelPolling(channelUID);
    }

    /**
     * Updates the handler incl. all pollings
     */
    public void updateHandler() {

        logger.info("(Re)Initialize all eBUS pollings for {} ...", thing.getUID());

        for (final Channel channel : thing.getChannels()) {
            updateChannelPolling(channel.getUID());
        }
    }
}
