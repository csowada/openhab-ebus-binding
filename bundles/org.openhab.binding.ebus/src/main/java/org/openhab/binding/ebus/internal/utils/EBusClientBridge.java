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
package org.openhab.binding.ebus.internal.utils;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.COMMAND;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.DRIVER_JSERIALCOMM;
import static org.openhab.binding.ebus.internal.EBusBindingConstants.VALUE_NAME;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ebus.internal.EBusBindingConstants;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandException;
import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusCommandMethod.Method;
import de.csdev.ebus.command.IEBusCommandMethod.Type;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.EBusControllerException;
import de.csdev.ebus.core.EBusEbusdController;
import de.csdev.ebus.core.EBusLowLevelController;
import de.csdev.ebus.core.IEBusController;
import de.csdev.ebus.core.connection.EBusEmulatorConnection;
import de.csdev.ebus.core.connection.EBusJSerialCommConnection;
import de.csdev.ebus.core.connection.EBusSerialNRJavaSerialConnection;
import de.csdev.ebus.core.connection.EBusTCPConnection;
import de.csdev.ebus.core.connection.IEBusConnection;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusClientBridge {

    private final Logger logger = LoggerFactory.getLogger(EBusClientBridge.class);

    private @Nullable IEBusController controller;

    private @Nullable IEBusConnection connection;

    private EBusClient client;

    /**
     * @param configuration
     */
    public EBusClientBridge(EBusCommandRegistry commandRegistry) {
        client = new EBusClient(commandRegistry);
    }

    /**
     * @param hostname
     * @param port
     */
    public void setTCPConnection(String hostname, int port) {

        IEBusConnection conn = new EBusTCPConnection(hostname, port);

        // load the eBus core element
        controller = new EBusLowLevelController(conn);
        this.connection = conn;
    }

    /**
     *
     * @param hostname
     * @param port
     */
    public void setEbusdConnection(String hostname, int port) {
        logger.debug("Set ebusd controller ...");
        controller = new EBusEbusdController(hostname, port);
    }

    /**
     * @param serialPort
     */
    public void setSerialConnection(String serialPort, String type) {

        IEBusConnection conn = null;

        if (StringUtils.equals(serialPort, "emulator")) {
            conn = new EBusEmulatorConnection();

        } else {
            try {
                if (StringUtils.equals(type, DRIVER_JSERIALCOMM)) {
                    conn = new EBusJSerialCommConnection(serialPort);
                } else {
                    conn = new EBusSerialNRJavaSerialConnection(serialPort);
                }
            } catch (NoClassDefFoundError e) {
                throw new IllegalStateException("Unable to load required serial driver class: " + e.getMessage());
            }
        }

        // load the eBus core element
        controller = new EBusLowLevelController(conn);
        this.connection = conn;
    }

    /**
     *
     * @param connection
     */
    public void setSerialConnection(IEBusConnection connection) {
        // load the eBus core element
        this.connection = connection;
        controller = new EBusLowLevelController(connection);
    }

    /**
     * @return
     */
    public EBusClient getClient() {
        return client;
    }

    public @Nullable IEBusController getController() {
        return client.getController();
    }

    /**
     * @return
     */
    public boolean isConnectionValid() {
        if (controller instanceof EBusEbusdController) {
            return true;
        }

        return connection != null;
    }

    /**
     * @param telegram
     * @return
     * @throws EBusControllerException
     */
    public Integer sendTelegram(ByteBuffer telegram) throws EBusControllerException {
        byte[] byteArray = EBusUtils.toByteArray(telegram);
        return client.addToSendQueue(byteArray);
    }

    /**
     * @param telegram
     * @return
     * @throws EBusControllerException
     */
    public Integer sendTelegram(byte[] telegram) throws EBusControllerException {
        return client.addToSendQueue(telegram);
    }

    /**
     * @param thing
     * @param channel
     * @param command
     * @return
     * @throws EBusTypeException
     * @throws EBusCommandException
     */
    public ByteBuffer generateSetterTelegram(Thing thing, Channel channel, Command command)
            throws EBusTypeException, EBusCommandException {
        String slaveAddress = (String) thing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);
        String collectionId = thing.getThingTypeUID().getId();

        Map<String, String> properties = channel.getProperties();

        String commandId = properties.getOrDefault(COMMAND, "");
        String valueName = properties.get(VALUE_NAME);

        if (StringUtils.isEmpty(commandId) || StringUtils.isEmpty(valueName)) {
            throw new EBusCommandException("Channel has no additional eBUS information!");
        }

        Byte target = EBusUtils.toByte(slaveAddress);

        if (target == null) {
            throw new EBusCommandException("");
        }

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getCommandMethodById(collectionId,
                commandId, Method.SET);

        if (commandMethod == null) {
            throw new EBusCommandException(String.format("Unable to find setter command with id %s", commandId));
        }

        // use master address for master-master telegrams
        if (commandMethod.getType().equals(Type.MASTER_MASTER)) {
            Byte newTarget = EBusUtils.getMasterAddress(target);
            if (newTarget != null) {
                target = newTarget;
            }
        }

        HashMap<@Nullable String, @Nullable Object> values = new HashMap<>();

        if (command instanceof State) {
            State state = (State) command;

            if (state instanceof OnOffType) {
                OnOffType onOff = state.as(OnOffType.class);
                values.put(valueName, onOff == OnOffType.ON);

            } else if (state instanceof DecimalType) {
                DecimalType decimalValue = state.as(DecimalType.class);
                if (decimalValue != null) {
                    values.put(valueName, decimalValue.toBigDecimal());
                }
            } else {
                String stateValue = state.toString();
                if (stateValue != null) {
                    DecimalType decimalValue = new DecimalType(stateValue);
                    values.put(valueName, decimalValue.toBigDecimal());
                }
            }
        }

        return client.buildTelegram(commandMethod, target, values);
    }

    /**
     * @param collectionId
     * @param commandId
     * @param type
     * @param targetThing
     * @return
     * @throws EBusTypeException
     * @throws EBusCommandException
     */
    public ByteBuffer generatePollingTelegram(String collectionId, String commandId, IEBusCommandMethod.Method type,
            Thing targetThing) throws EBusTypeException, EBusCommandException {
        String slaveAddress = (String) targetThing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getCommandMethodById(collectionId,
                commandId, type);

        if (commandMethod == null) {
            throw new EBusCommandException("Unable to find command method {0} {1} {2} !", type, commandId, collectionId);
        }

        if (!commandMethod.getType().equals(IEBusCommandMethod.Type.MASTER_SLAVE)) {
            throw new EBusCommandException("Polling is only available for master-slave commands!");
        }

        if (StringUtils.isEmpty(slaveAddress)) {
            throw new EBusCommandException("Unable to poll, Thing has no slave address defined!");
        }

        Byte target = EBusUtils.toByte(slaveAddress);

        if (target == null) {
            throw new EBusCommandException("Unable to convert slave address to byte!");
        }

        return client.buildTelegram(commandMethod, target, null);
    }

    /**
     * @param masterAddress
     */
    public void initClient(Byte masterAddress) {
        // connect the high level client
        IEBusController ctlr = this.controller;
        if (ctlr != null) {
            client.connect(ctlr, masterAddress);
        }
    }

    /**
     *
     */
    public void startClient() {
        IEBusController ctlr = this.controller;
        if (ctlr != null) {
            ctlr.start();
        }
    }

    /**
     *
     */
    public void stopClient() {
        IEBusController ctlr = this.controller;
        if (ctlr != null && !ctlr.isInterrupted()) {
            ctlr.interrupt();
        }
    }
}
