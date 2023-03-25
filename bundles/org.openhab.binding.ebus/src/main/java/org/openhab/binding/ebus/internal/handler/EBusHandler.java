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

import static org.openhab.binding.ebus.internal.EBusBindingConstants.COMMAND;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.ITEM_TYPE_DATETIME;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.ITEM_TYPE_NUMBER;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.ITEM_TYPE_STRING;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.ITEM_TYPE_SWITCH;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.ITEM_TYPE_TEMPERATURE;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.POLLING;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.VALUE_NAME;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.measure.quantity.Temperature;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ebus.internal.EBusHandlerConfiguration;
import org.openhab.binding.ebus.internal.utils.EBusBindingUtils;
import org.openhab.binding.ebus.internal.utils.EBusClientBridge;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandException;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.EBusConsts;
import de.csdev.ebus.core.EBusControllerException;
import de.csdev.ebus.core.IEBusController;
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

    @SuppressWarnings({"null"})
    private final Logger logger = LoggerFactory.getLogger(EBusHandler.class);

    private Map<ChannelUID, @Nullable ByteBuffer> channelPollings = new HashMap<>();

    private Random random = new Random(12);

    private Map<ByteBuffer, @Nullable ScheduledFuture<?>> uniqueTelegramPollings = new HashMap<>();

    /**
     * @param thing
     */
    public EBusHandler(Thing thing) {
        super(thing);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.core.thing.binding.BaseThingHandler#initialize()
     */
    @Override
    public void initialize() {

        logger.trace("initialize handler {}", this.thing.getUID());

        EBusHandlerConfiguration configuration = getConfigAs(EBusHandlerConfiguration.class);

        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("No bridge defined!");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "No bridge defined!");

        } else if (StringUtils.isEmpty(configuration.slaveAddress)) {
            logger.error("Slave address is not set!");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Slave address is not set!");

        } else if (bridge.getStatus() != ThingStatus.ONLINE) {
            logger.info("Bridge is not online yet ...");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);

        } else {
            logger.debug("Handler is now online ...");
            updateStatus(ThingStatus.ONLINE);
            updateAllChannelPollings();
        }

    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("Bridge status changed to {}.", bridgeStatusInfo.getStatus());
        super.bridgeStatusChanged(bridgeStatusInfo);

        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
            updateAllChannelPollings();
        } else {
            disposeAllChannelPollings();
        }
    }
    
    @Override
    public void channelLinked(ChannelUID channelUID) {
        super.channelLinked(channelUID);

        logger.trace("channelLinked {}", channelUID);
        initializeChannelPolling(channelUID);
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        super.channelUnlinked(channelUID);

        logger.trace("channelUnlinked {}", channelUID);
        disposeChannelPolling(channelUID);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openhab.core.thing.binding.BaseThingHandler#dispose()
     */
    @Override
    public void dispose() {

        logger.trace("dispose handler {}", this.thing.getUID());

        disposeAllChannelPollings();
    }

    private void disposeAllChannelPollings() {
        synchronized(uniqueTelegramPollings) {
            // Cancel all polling jobs
            for (Entry<ByteBuffer, @Nullable ScheduledFuture<?>> entry : uniqueTelegramPollings.entrySet()) {
                logger.debug("Remove polling job for {}", EBusUtils.toHexDumpString(entry.getKey()));

                ScheduledFuture<?> value = entry.getValue();
                if (value != null) {
                    value.cancel(true);
                }
            }
            uniqueTelegramPollings.clear();
        }
        channelPollings.clear();
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

    /**
     * @param channelUID
     */
    private void disposeChannelPolling(ChannelUID channelUID) {

        if (channelPollings.containsKey(channelUID)) {
            ByteBuffer telegram = channelPollings.remove(channelUID);

            if (!channelPollings.containsValue(telegram)) {
                // remove last
                ScheduledFuture<?> future = uniqueTelegramPollings.remove(telegram);
                if (future != null) {
                    future.cancel(true);
                }

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

        final Map<String, String> properties = channel.getProperties();
        final Configuration thingConfiguration = thing.getConfiguration();
        final Configuration configuration = channel.getConfiguration();

        @Nullable final String valueName = properties.get(VALUE_NAME);

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
                logger.trace("Set global polling period {} for channel {}", pollingPeriod, channel.getUID());
            }
        }

        // skip channels starting with _ , this is a ebus command that starts with _
        if (StringUtils.startsWith(valueName, "_")) {
            if (pollingPeriod > 0) {
                logger.trace("Set interval to 0 due to a leading _ in the valueName ...");
            }
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

        try {
            final EBusClientBridge libClient = getLibClient();

            final Map<String, String> properties = channel.getProperties();
            final String collectionId = thing.getThingTypeUID().getId();
            final @Nullable String commandId = properties.get(COMMAND);

            if(commandId != null) {
                return libClient.generatePollingTelegram(collectionId, commandId, IEBusCommandMethod.Method.GET, thing);
            }
            return null;

        } catch (EBusTypeException  e) {  
            logger.error("error!", e);
        } catch (EBusCommandException e) {
            // noop
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
            throw new IllegalStateException("No eBUS bridge defined!");
        }

        EBusBridgeHandler handler = (EBusBridgeHandler) bridge.getHandler();
        if (handler != null) {
            return handler.getLibClient();
        }

        throw new IllegalStateException("Unable to get an eBUS Client from Backend");
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.openhab.core.thing.binding.ThingHandler#handleCommand(org.openhab.core.thing.ChannelUID,
     * org.openhab.core.types.Command)
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        if (!(command instanceof RefreshType)) {
            Channel channel = thing.getChannel(channelUID.getId());

            if (channel != null) {
                try {
                    EBusClientBridge libClient = getLibClient();
                    ByteBuffer telegram = libClient.generateSetterTelegram(thing, channel, command);
                    libClient.sendTelegram(telegram);
                } catch (EBusTypeException | EBusControllerException | EBusCommandException e) {
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



    /**
     * Initialize the polling for a channel if used
     *
     * @param channelUID
     */
    private void initializeChannelPolling(ChannelUID channelUID) {

        // only for linked channels
        if (!isLinked(channelUID.getId())) {
            return;
        }

        Channel channel = thing.getChannel(channelUID.getId());

        if (channel == null) {
            logger.trace("Channel with id {} not found!", channelUID.getId());
            return;
        }

        long pollingPeriod = getChannelPollingInterval(channel);

        // a valid value for polling ?
        if (pollingPeriod == 0) {
            logger.trace("Channel with id {} has no pooling defined ...", channelUID.getId());
            return;
        }

        @Nullable final String commandId = channel.getProperties().get(COMMAND);
        
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
                ScheduledFuture<?> job = scheduler.scheduleWithFixedDelay(() -> {
                    logger.trace("Poll command \"{}\" with \"{}\" ...", channel.getUID(),
                            EBusUtils.toHexDumpString(telegram));

                    try {
                        EBusClientBridge libClient = EBusHandler.this.getLibClient();
                        IEBusController controller = libClient.getController();
                        EBusClient client = libClient.getClient();
                        if (controller != null && controller.getConnectionStatus() == ConnectionStatus.CONNECTED) {
                            client.addToSendQueue(EBusUtils.toByteArray(telegram), 2);
                        } else {
                            logger.trace("Unable to send polling command due to a unconnected controller");
                        }

                    } catch (EBusControllerException e) {
                        logger.debug("Remove polling job for {} due to controller exception", channelUID);
                        this.disposeChannelPolling(channelUID);
                    }

                }, firstExecutionDelay, pollingPeriod, TimeUnit.SECONDS);

                if (job != null) {
                    // add this job to global list, so we can stop all later on.
                    uniqueTelegramPollings.put(telegram, job);

                    logger.info("Register polling for \"{}\" every {} sec. (initial delay {} sec.)", commandId,
                            pollingPeriod, firstExecutionDelay);
                }

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

        Map<String, String> properties = thing.getProperties();
        @Nullable String oldHash = properties.get("collectionHash");
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
    public boolean supportsTelegram(byte[] receivedData, IEBusCommandMethod commandMethod) {

        final String collectionId = thing.getThingTypeUID().getId();
        if (!commandMethod.getParent().getParentCollection().getId().equals(collectionId)) {
            return false;
        }

        EBusHandlerConfiguration configuration = getConfigAs(EBusHandlerConfiguration.class);
        logger.trace("eBUS handler cfg {}", configuration);

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
    public void thingUpdated(Thing thing) {

        // Info: I expect no difference in the channel list without a restart!
        Thing currentThing = this.thing;

        this.thing = thing;

        for (Channel oldChannel : currentThing.getChannels()) {

            Channel newChannel = thing.getChannel(oldChannel.getUID().getId());
            logger.info("thingUpdated {}", oldChannel.getUID());
            if (newChannel != null
                    && !Objects.equals(oldChannel.getConfiguration(), newChannel.getConfiguration())) {
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
     * Updates all channel pollings
     */
    private void updateAllChannelPollings() {
        synchronized(uniqueTelegramPollings) {
            logger.info("(Re)Initialize all eBUS pollings for {} ...", thing.getUID());
            thing.getChannels().forEach(channel -> updateChannelPolling(channel.getUID()));
        }
    }
}
