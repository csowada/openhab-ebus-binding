/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.action;

import java.nio.ByteBuffer;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ebus.internal.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.internal.utils.EBusClientBridge;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandException;
import de.csdev.ebus.command.EBusCommandUtils;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusCommandMethod.Method;
import de.csdev.ebus.command.datatypes.EBusTypeException;
import de.csdev.ebus.core.EBusControllerException;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@ThingActionsScope(name = "ebus")
@NonNullByDefault
public class EBusActions implements ThingActions {

    @NonNullByDefault({})
    private final Logger logger = LoggerFactory.getLogger(EBusActions.class);

    /** The eBUS bridge */
    private @Nullable EBusBridgeHandler handler;

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        this.handler = (EBusBridgeHandler) handler;
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return this.handler;
    }

    /**
     * Returns the current eBUS client or null
     *
     * @return The current eBUS client or null
     */
    public @Nullable EBusClient getEBusClient() {

        EBusBridgeHandler tmpHandler = this.handler;
        if (tmpHandler != null) {
            EBusClientBridge clientBridge = tmpHandler.getLibClient();
            return clientBridge.getClient();
        }

        return null;
    }

    /**
     * Static variant for classic rule files (DSL).
     * Sends a complete RAW HEX string telegram
     *
     * @param actions The EBusActions object
     * @param rawTelegram The RAW HEX string with the full telegram
     */
    public static void sendRawTelegram(@Nullable ThingActions actions, @Nullable String rawTelegram) {
        if (actions instanceof EBusActions) {
            ((EBusActions) actions).sendRawTelegram(rawTelegram);
        } else {
            throw new IllegalArgumentException("Instance is not an EBusActions class.");
        }
    }

    /**
     * Static variant for classic rule files (DSL).
     * Sends a command from the registry with values from the map or default/replace values
     *
     * @param actions The EBusActions object
     * @param collectionId The collection id
     * @param commandId The command id
     * @param destinationAddress THe destination as HEX byte
     * @param values Additional values as Map
     */
    public static void sendCommand(@Nullable ThingActions actions, @Nullable String collectionId,
            @Nullable String commandId, @Nullable String destinationAddress,
            @Nullable Map<@Nullable String, @Nullable Object> values) {
        if (actions instanceof EBusActions) {
            ((EBusActions) actions).sendCommand(collectionId, commandId, destinationAddress, values);
        } else {
            throw new IllegalArgumentException("Instance is not an EBusActions class.");
        }
    }

    /**
     * Sends a complete RAW HEX string telegram
     *
     * @param rawTelegram The RAW HEX string with the full telegram
     */
    @RuleAction(label = "Send a raw eBUS telegram", description = "Sends a raw telegram without modifiying any part")
    public void sendRawTelegram(
            @ActionInput(name = "rawTelegram", label = "Raw HEX telegram", description = "A complete HEX telegram from source address to CRC. If CRC is missing it will be calculated.") @Nullable String rawTelegram) {

        EBusClient client = getEBusClient();

        if (client == null || StringUtils.isEmpty(rawTelegram)) {
            return;
        }

        try {
            byte[] data = EBusUtils.toByteArray(rawTelegram);

            if (data.length > 0) {
                client.addToSendQueue(EBusCommandUtils.prepareSendTelegram(data));
            }

        } catch (EBusDataException | EBusControllerException e) {
            logger.error("error!", e);
        }
    }

    /**
     * Sends a command from the registry with values from the map or default/replace values
     *
     * @param collectionId The collection id
     * @param commandId The command id
     * @param destinationAddress THe destination as HEX byte
     * @param values Additional values as Map
     */
    @RuleAction(label = "Send an eBUS command", description = "Sends an eBUS command with values in a map")
    public void sendCommand(@ActionInput(name = "collectionId", label = "Collection ID") @Nullable String collectionId,
            @ActionInput(name = "commandId", label = "Command ID") @Nullable String commandId,
            @ActionInput(name = "destinationAddress", label = "Destination address (HEX)") @Nullable String destinationAddress,
            @ActionInput(name = "values", label = "Values as Map") @Nullable Map<@Nullable String, @Nullable Object> values) {

        if (collectionId == null || StringUtils.isEmpty(collectionId)) {
            throw new IllegalArgumentException("Parameter 'collectionId' is required!");
        }

        if (commandId == null || StringUtils.isEmpty(commandId)) {
            throw new IllegalArgumentException("Parameter 'commandId' is required!");
        }

        if (destinationAddress == null || StringUtils.isEmpty(destinationAddress)) {
            throw new IllegalArgumentException("Parameter 'destinationAddress' is required!");
        }

        EBusClient client = getEBusClient();

        if (client == null) {
            return;
        }

        IEBusCommandMethod method = client.getConfigurationProvider().getCommandMethodById(collectionId, commandId,
                Method.SET);

        // second try with a broadcast
        if (method == null) {
            client.getConfigurationProvider().getCommandMethodById(collectionId, commandId, Method.BROADCAST);
        }

        if (method == null) {
            throw new IllegalArgumentException(
                    String.format("Unable to find a SET or BROADCAST command with id %s.%s", collectionId, commandId));
        }

        Byte destionationAddressByte = EBusUtils.toByte(destinationAddress);
        if (destionationAddressByte == null) {
            throw new IllegalArgumentException("Invalid destination address!");
        }

        try {
            ByteBuffer buffer = client.buildTelegram(method, destionationAddressByte, values);
            client.addToSendQueue(EBusUtils.toByteArray(buffer));

        } catch (EBusTypeException | EBusControllerException | EBusCommandException e) {
            logger.error("error!", e);
        }
    }
}
