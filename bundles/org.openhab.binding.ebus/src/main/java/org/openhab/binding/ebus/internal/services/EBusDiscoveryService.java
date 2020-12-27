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

import static org.openhab.binding.ebus.internal.EBusBindingConstants.BINDING_ID;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ebus.internal.EBusBindingConstants;
import org.openhab.binding.ebus.internal.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.internal.handler.IEBusBridgeHandler;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.core.EBusConsts;
import de.csdev.ebus.service.device.EBusDeviceTableService;
import de.csdev.ebus.service.device.IEBusDevice;
import de.csdev.ebus.service.device.IEBusDeviceTableListener;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusDiscoveryService extends AbstractDiscoveryService implements IEBusDeviceTableListener {

    private final Logger logger = LoggerFactory.getLogger(EBusDiscoveryService.class);

    private IEBusBridgeHandler bridgeHandle;

    private boolean disableDiscovery = false;

    private static final String REPRESENTATION_PROPERTY = "ebusRepresentationId";

    public EBusDiscoveryService(EBusBridgeHandler bridgeHandle) throws IllegalArgumentException {
        super(new HashSet<>(Arrays.asList(bridgeHandle.getThing().getThingTypeUID())), 20, false);

        this.bridgeHandle = bridgeHandle;
        bridgeHandle.getLibClient().getClient().addEBusDeviceTableListener(this);
    }

    /**
     * @return
     */
    @Nullable
    private EBusDeviceTableService getDeviceTableService() {
        return bridgeHandle.getLibClient().getClient().getDeviceTableService();
    }

    @Override
    protected void startScan() {
        logger.debug("Starting eBUS discovery scan ...");

        EBusDeviceTableService deviceTableService = getDeviceTableService();
        if (deviceTableService != null) {
            deviceTableService.inquiryDeviceExistence();
        }
    }

    @Override
    @Activate
    public void activate(@Nullable Map<String, Object> configProperties) {
        super.activate(configProperties);
        logger.debug("Start eBUS discovery service ...");

        disableDiscovery = Boolean.getBoolean(System.getProperty("EBUS_DISABLE_DISCOVERY", "false").toLowerCase());

        if (disableDiscovery) {
            logger.warn("eBUS Discovery Service is DISABLED! You will not receive any new Things in your Inbox.");
            logger.warn("!!! This is a temporary switch that will be removed later !!!");
        }
    }

    @Override
    @Deactivate
    public void deactivate() {

        logger.debug("Stopping eBUS discovery service ...");

        removeOlderResults(new Date().getTime());

        try {
            bridgeHandle.getLibClient().getClient().removeEBusDeviceTableListener(this);
        } catch (Exception e) {
            // okay, maybe not set
        }
    }

    /**
     * @param device
     * @param collection
     */
    private void updateDiscoveredThing(IEBusDevice device, IEBusCommandCollection collection) {
        String masterAddress = EBusUtils.toHexDumpString(device.getMasterAddress());
        String slaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());

        String id = slaveAddress;

        if (StringUtils.isEmpty(id)) {
            logger.debug("No slave address for device available! {}", device.toString());
            return;
        }

        ThingTypeUID thingTypeUID = new ThingTypeUID(BINDING_ID, collection.getId());
        ThingUID thingUID = new ThingUID(thingTypeUID, bridgeHandle.getThing().getUID(), id);

        Map<String, Object> properties = new HashMap<>();
        properties.put(EBusBindingConstants.MASTER_ADDRESS, masterAddress);
        properties.put(EBusBindingConstants.SLAVE_ADDRESS, slaveAddress);

        // add represenation property for correct discovery
        properties.put(REPRESENTATION_PROPERTY, collection.getId() + "-" + slaveAddress);

        // not nice from the api, one time Map<String, String> another time <String, Object>
        Map<String, String> deviceProperties = new HashMap<>();
        updateThingProperties(device, deviceProperties);
        properties.putAll(deviceProperties);

        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(thingUID).withThingType(thingTypeUID)
                .withProperties(properties).withBridge(bridgeHandle.getThing().getUID())
                .withRepresentationProperty(String.format("%s (%s)", collection.getLabel(), slaveAddress))
                .withRepresentationProperty(REPRESENTATION_PROPERTY)
                .withLabel(String.format("%s (%s)", collection.getLabel(), slaveAddress)).build();

        thingDiscovered(discoveryResult);
    }

    @Override
    public void onEBusDeviceUpdate(@Nullable TYPE type, @Nullable IEBusDevice device) {
        if (device != null && type != null && !type.equals(TYPE.UPDATE_ACTIVITY)) {
            if (!disableDiscovery) {
                EBusClient client = bridgeHandle.getLibClient().getClient();

                Collection<IEBusCommandCollection> commandCollections = client.getCommandCollections();
                IEBusCommandCollection commonCollection = client.getCommandCollection(EBusConsts.COLLECTION_STD);

                if (commonCollection != null) {
                    // update common thing
                    updateDiscoveredThing(device, commonCollection);

                    // search for collection with device id
                    String deviceStr = EBusUtils.toHexDumpString(device.getDeviceId()).toString();
                    for (final IEBusCommandCollection collection : commandCollections) {
                        if (collection.getIdentification().contains(deviceStr)) {
                            logger.debug("Discovered eBUS device {} ...", collection.getId());

                            updateDiscoveredThing(device, collection);

                        }
                    }
                }
            }

            // update already initialized eBUS nodes
            updateInitializedThings(device);
        }
    }

    /**
     * Update already initialized things
     *
     * @param type
     * @param device
     */
    private void updateInitializedThings(IEBusDevice device) {
        String deviceSlaveAddress = EBusUtils.toHexDumpString(device.getSlaveAddress());

        for (Thing thing : bridgeHandle.getThing().getThings()) {
            String slaveAddress = (String) thing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);

            if (deviceSlaveAddress.equals(slaveAddress)) {

                Map<String, String> properties = new HashMap<>();
                properties.putAll(thing.getProperties());
                updateThingProperties(device, properties);
                thing.setProperties(properties);
            }
        }
    }

    /**
     * @param device
     * @param properties
     */
    private void updateThingProperties(IEBusDevice device, Map<String, String> properties) {
        if (device.getDeviceId() != null && device.getDeviceId().length == 5) {
            properties.put(Thing.PROPERTY_MODEL_ID, EBusUtils.toHexDumpString(device.getDeviceId()).toString());
        } else {
            properties.remove(Thing.PROPERTY_MODEL_ID);
        }

        if (device.getHardwareVersion() != null) {
            properties.put(Thing.PROPERTY_HARDWARE_VERSION, device.getHardwareVersion().toPlainString());
        } else {
            properties.remove(Thing.PROPERTY_HARDWARE_VERSION);
        }

        if (device.getSoftwareVersion() != null) {
            properties.put(Thing.PROPERTY_FIRMWARE_VERSION, device.getSoftwareVersion().toPlainString());
        } else {
            properties.remove(Thing.PROPERTY_FIRMWARE_VERSION);
        }

        if (device.getManufacturerName() != null) {
            properties.put(Thing.PROPERTY_VENDOR, device.getManufacturerName());
        } else {
            properties.remove(Thing.PROPERTY_VENDOR);
        }
    }
}
