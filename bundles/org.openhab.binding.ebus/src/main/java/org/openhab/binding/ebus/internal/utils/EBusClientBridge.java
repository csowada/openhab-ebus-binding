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

import static org.openhab.binding.ebus.internal.EBusBindingConstants.*;

import java.nio.ByteBuffer;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.ebus.internal.EBusBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
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

    @Nullable
    private IEBusController controller;

    @Nullable
    private IEBusConnection connection;

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
        connection = new EBusTCPConnection(hostname, port);

        // load the eBus core element
        controller = new EBusLowLevelController(connection);
    }

    public void setEbusdConnection(String hostname, int port) {
        logger.debug("Set ebusd controller ...");
        controller = new EBusEbusdController(hostname, port);
    }

    /**
     * @param serialPort
     */
    public void setSerialConnection(String serialPort, String type) {
        if (StringUtils.equals(serialPort, "emulator")) {
            connection = new EBusEmulatorConnection();

        } else {
            if (StringUtils.equals(type, DRIVER_JSERIALCOMM)) {
                connection = new EBusJSerialCommConnection(serialPort);
            } else {
                connection = new EBusSerialNRJavaSerialConnection(serialPort);
            }
        }

        // load the eBus core element
        controller = new EBusLowLevelController(connection);
    }

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
        return client.addToSendQueue(EBusUtils.toByteArray(telegram));
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
     */
    @Nullable
    public ByteBuffer generateSetterTelegram(Thing thing, Channel channel, Command command) throws EBusTypeException {
        String slaveAddress = (String) thing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);
        String collectionId = thing.getThingTypeUID().getId();
        String commandId = channel.getProperties().get(COMMAND);
        String valueName = channel.getProperties().get(VALUE_NAME);

        if (StringUtils.isEmpty(commandId) || StringUtils.isEmpty(valueName)) {
            logger.error("Channel has no additional eBUS information!");
            return null;
        }

        byte target = EBusUtils.toByte(slaveAddress);

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getCommandMethodById(collectionId,
                commandId, Method.SET);

        if (commandMethod == null) {
            logger.error("Unable to find setter command with id {}", commandId);
            return null;
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
                DecimalType decimalValue = new DecimalType(state.toString());
                values.put(valueName, decimalValue.toBigDecimal());

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
     */
    @Nullable
    public ByteBuffer generatePollingTelegram(String collectionId, String commandId, IEBusCommandMethod.Method type,
            Thing targetThing) throws EBusTypeException {
        String slaveAddress = (String) targetThing.getConfiguration().get(EBusBindingConstants.SLAVE_ADDRESS);

        IEBusCommandMethod commandMethod = client.getConfigurationProvider().getCommandMethodById(collectionId,
                commandId, type);

        if (commandMethod == null) {
            logger.error("Unable to find command method {} {} {} !", type, commandId, collectionId);
            return null;
        }

        if (!commandMethod.getType().equals(IEBusCommandMethod.Type.MASTER_SLAVE)) {
            logger.warn("Polling is only available for master-slave commands!");
            return null;
        }

        if (StringUtils.isEmpty(slaveAddress)) {
            logger.warn("Unable to poll, Thing has no slave address defined!");
            return null;
        }

        byte target = EBusUtils.toByte(slaveAddress);

        return client.buildTelegram(commandMethod, target, null);
    }

    /**
     * @param masterAddress
     */
    public void initClient(Byte masterAddress) {
        // connect the high level client
        client.connect(controller, masterAddress);
    }

    /**
     *
     */
    public void startClient() {
        if (controller != null) {
            controller.start();
        }
    }

    /**
     *
     */
    @SuppressWarnings("null")
    public void stopClient() {
        if (controller != null && !controller.isInterrupted()) {
            controller.interrupt();
        }
    }
}
