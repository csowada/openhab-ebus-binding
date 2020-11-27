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

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.smarthome.io.console.Console;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandUtils;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.core.EBusConnectorEventListener;
import de.csdev.ebus.core.EBusControllerException;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.core.IEBusController.ConnectionStatus;
import de.csdev.ebus.service.parser.IEBusParserListener;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusConsoleSendReceiver extends EBusConnectorEventListener implements IEBusParserListener {

    private EBusClient client;
    private Integer queueId;
    private Console console;

    public EBusConsoleSendReceiver(EBusClient client, Console console) {
        this.client = client;
        this.console = console;
    }

    public void send(byte[] data) throws EBusControllerException, EBusDataException {

        client.addEBusEventListener(this);
        client.addEBusParserListener(this);

        queueId = client.addToSendQueue(EBusCommandUtils.prepareSendTelegram(data));
        console.printf("Send telegram with id %d, waiting for response ...\n", queueId);
    }

    public void dispose() {
        if (client != null) {
            client.removeEBusEventListener(this);
            client.removeEBusParserListener(this);
            queueId = null;
            client = null;
        }
    }

    @Override
    public void onTelegramResolved(IEBusCommandMethod commandChannel, Map<String, Object> result, byte[] receivedData,
            Integer sendQueueId) {

        if (queueId != null && queueId.equals(sendQueueId)) {

            console.printf("Status    : Successful send %s\n", sendQueueId);
            console.printf("Command ID: %s\n", EBusCommandUtils.getFullId(commandChannel));
            console.printf("Telegram  : %s\n", EBusUtils.toHexDumpString(receivedData).toString());

            console.println("");
            console.println("Received values:");
            for (Entry<String, Object> entry : result.entrySet()) {
                console.println(String.format(" %s: %s", entry.getKey(), entry.getValue().toString()));
            }

            dispose();
        }
    }

    @Override
    public void onTelegramResolveFailed(IEBusCommandMethod commandChannel, byte[] receivedData, Integer sendQueueId,
            String exceptionMessage) {

        if (queueId != null && queueId.equals(sendQueueId)) {

            console.printf("Status    : FAILED %s\n", sendQueueId);
            console.printf("Command ID: %s\n", EBusCommandUtils.getFullId(commandChannel));
            console.printf("Telegram  : %s\n", EBusUtils.toHexDumpString(receivedData).toString());
            console.printf("Error     : %s\n", exceptionMessage);

            dispose();
        }
    }

    @Override
    public void onTelegramException(EBusDataException exception, Integer sendQueueId) {
        if (queueId != null && queueId.equals(sendQueueId)) {

            console.printf("Status    : FAILED %s\n", sendQueueId);
            console.printf("Error     : %s\n", exception.getMessage());

            dispose();
        }
    }

    @Override
    public void onConnectionException(Exception e) {
        dispose();
    }

    @Override
    public void onConnectionStatusChanged(ConnectionStatus status) {
        dispose();
    }
}
