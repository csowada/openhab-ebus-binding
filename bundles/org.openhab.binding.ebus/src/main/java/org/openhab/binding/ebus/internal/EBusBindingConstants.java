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
package org.openhab.binding.ebus.internal;

import java.net.URI;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link EBusBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusBindingConstants {

    private EBusBindingConstants() {
        throw new IllegalStateException("Utility class");
    }

    public static final String BINDING_ID = "ebus";
    public static final String BINDING_PID = "binding.ebus";

    // bridge
    public static final ThingTypeUID THING_TYPE_EBUS_BRIDGE = new ThingTypeUID(BINDING_ID, "bridge");

    // ebus thing properties
    public static final String MASTER_ADDRESS = "masterAddress";
    public static final String SLAVE_ADDRESS = "slaveAddress";
    public static final String POLLING = "polling";

    // properties for ebus connection
    public static final String SERIAL_PORT = "serialPort";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String PORT = "port";

    public static final String DRIVER_RAW = "raw";
    public static final String DRIVER_EBUSD = "ebusd";
    public static final String NETWORK_DRIVER = "networkDriver";

    public static final String SERIAL_PORT_DRIVER = "serialPortDriver";
    public static final String ADVANCED_LOGGING = "advancedLogging";

    public static final String DRIVER_BUILDIN = "buildin";
    public static final String DRIVER_NRJAVASERIAL = "nrjavaserial";
    public static final String DRIVER_JSERIALCOMM = "jserialcomm";

    public static final String CONFIGURATION_URL = "configurationUrl";
    public static final String CONFIGURATION_URL1 = "configurationUrl1";
    public static final String CONFIGURATION_URL2 = "configurationUrl2";
    public static final String CONFIGURATION_BUNDLE_URL = "configurationBundleUrl";

    // properties to map ebus core configurations
    // public static final String COLLECTION = "collection";
    public static final String COMMAND = "command";
    public static final String METHOD = "method";
    public static final String VALUE_NAME = "valueName";

    public static final String FILTER_ACCEPT_MASTER = "filterAcceptMaster";
    public static final String FILTER_ACCEPT_SLAVE = "filterAcceptSlave";
    public static final String FILTER_ACCEPT_BROADCAST = "filterAcceptBroadcasts";

    // channel group id
    public static final String METRICS = "metrics";

    // channel ids
    public static final String RECEIVED_TELEGRAMS = "receivedTelegrams";
    public static final String FAILED_TELEGRAMS = "failedTelegrams";
    public static final String RESOLVED_TELEGRAMS = "resolvedTelegrams";
    public static final String UNRESOLVED_TELEGRAMS = "unresolvedTelegrams";
    public static final String FAILED_RATIO = "failedRatio";
    public static final String UNRESOLVED_RATIO = "unresolvedRatio";
    public static final String SEND_RECEIVE_ROUNDTRIP_TIME = "sendReceiveRoundtripTime";

    // configuration uris

    public static final URI CONFIG_DESCRIPTION_URI_NODE = URI.create("thing-type:" + BINDING_ID + ":nodeConfig");

    public static final URI CONFIG_DESCRIPTION_URI_POLLING_CHANNEL = URI
            .create("channel-type:" + BINDING_ID + ":pollingChannel");

    public static final URI CONFIG_DESCRIPTION_URI_NULL_CHANNEL = URI
            .create("channel-type:" + BINDING_ID + ":nullChannel");

    // item types
    public static final String ITEM_TYPE_NUMBER = "Number";
    public static final String ITEM_TYPE_STRING = "String";
    public static final String ITEM_TYPE_SWITCH = "Switch";
    public static final String ITEM_TYPE_DATETIME = "DateTime";

    public static final String ITEM_TYPE_TEMPERATURE = "Number:Temperature";
}
