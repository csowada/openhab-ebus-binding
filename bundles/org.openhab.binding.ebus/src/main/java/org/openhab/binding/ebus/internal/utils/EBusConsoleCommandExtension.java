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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.ebus.internal.handler.EBusBridgeHandler;
import org.openhab.binding.ebus.internal.handler.EBusHandler;
import org.openhab.binding.ebus.internal.things.EBusTypeProviderException;
import org.openhab.binding.ebus.internal.things.IEBusTypeProvider;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelDefinition;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ThingType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import de.csdev.ebus.client.EBusClient;
import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.core.EBusControllerException;
import de.csdev.ebus.core.EBusDataException;
import de.csdev.ebus.core.EBusEbusdController;
import de.csdev.ebus.core.EBusLowLevelController;
import de.csdev.ebus.core.IEBusController;
import de.csdev.ebus.core.connection.EBusEmulatorConnection;
import de.csdev.ebus.core.connection.IEBusConnection;
import de.csdev.ebus.service.device.EBusDeviceTable;
import de.csdev.ebus.utils.EBusConsoleUtils;
import de.csdev.ebus.utils.EBusUtils;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
@Component(service = { ConsoleCommandExtension.class }, immediate = true)
public class EBusConsoleCommandExtension implements ConsoleCommandExtension {

    private static final String CMD = "ebus";

    private static final String SUBCMD_LIST = "list";

    private static final String SUBCMD_SEND = "send";

    private static final String SUBCMD_DEVICES = "devices";

    private static final String SUBCMD_RESOLVE = "resolve";

    private static final String SUBCMD_RELOAD = "reload";

    private static final String SUBCMD_UPDATE = "update";

    private static final String SUBCMD_CHANNELS = "channels";

    @NonNullByDefault({})
    @Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    private ThingRegistry thingRegistry;

    @NonNullByDefault({})
    @Reference(policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    private IEBusTypeProvider typeProvider;

    @Override
    public String getCommand() {
        return CMD;
    }

    @Override
    public String getDescription() {
        return "eBUS commands";
    }

    /**
     * @return
     */
    private Collection<EBusBridgeHandler> getAllEBusBridgeHandlers() {
        Collection<EBusBridgeHandler> result = new ArrayList<>();
        ThingRegistry thingRegistry = this.thingRegistry;

        if (thingRegistry != null) {
            for (Thing thing : thingRegistry.getAll()) {
                if (thing.getHandler() instanceof EBusBridgeHandler) {
                    EBusBridgeHandler handler = (EBusBridgeHandler) thing.getHandler();
                    if (handler != null) {
                        result.add(handler);
                    }
                }
            }
        }

        return result;
    }

    private void listChannels(String[] args, Console console) {
        Collection<ThingType> thingTypes = typeProvider.getThingTypes(null);

        for (ThingType thingType : thingTypes) {
            String format = String.format("** %-45s | ID: %-20s **", "Type: " + thingType.getLabel(),
                    thingType.getUID().getId());

            console.println("");
            console.println(StringUtils.repeat("*", format.length()));
            console.println(format);
            console.println(StringUtils.repeat("*", format.length()));

            List<ChannelGroupDefinition> channelGroupDefinitions = thingType.getChannelGroupDefinitions();
            for (ChannelGroupDefinition channelGroupDefinition : channelGroupDefinitions) {

                if (typeProvider != null) {

                    ChannelGroupType channelGroupType = typeProvider
                            .getChannelGroupType(channelGroupDefinition.getTypeUID(), null);

                    if (channelGroupType != null) {
                        console.println("\n  ChannelGroupType " + channelGroupType.getUID().getId());

                        console.println(String.format("\n  %-60s | %-40s",
                                channelGroupType.getUID().getId() + "#" + channelGroupType.getUID().getId(),
                                channelGroupType.getLabel()));

                        List<ChannelDefinition> channelDefinitions = channelGroupType.getChannelDefinitions();

                        for (ChannelDefinition channelDefinition : channelDefinitions) {

                            console.println(String.format("    -> %-55s | %-40s",
                                    channelGroupType.getUID().getId() + "#" + channelDefinition.getId(),
                                    channelDefinition.getLabel()));

                        }
                    }
                }
            }
        }
    }

    /**
     * @param args
     * @param console
     * @throws EBusControllerException
     */
    @SuppressWarnings({"java:S3776"})
    private void list(String[] args, Console console) throws EBusControllerException {
        console.println(String.format("%-40s | %-40s | %-10s", "Thing UID", "Label", "Type"));
        console.println(String.format("%-40s-+-%-40s-+-%-10s", StringUtils.repeat("-", 40), StringUtils.repeat("-", 40),
                StringUtils.repeat("-", 10)));

        ThingRegistry thingRegistry = this.thingRegistry;

        if (thingRegistry != null) {
            for (Thing thing : thingRegistry.getAll()) {
                if (thing.getHandler() instanceof EBusBridgeHandler || thing.getHandler() instanceof EBusHandler) {
                    String type = "node";

                    if (thing.getHandler() instanceof EBusBridgeHandler) {
                        EBusBridgeHandler bridge = (EBusBridgeHandler) thing.getHandler();

                        if (bridge == null) {
                            throw new IllegalStateException("eBus bridge handler is not available!");
                        }

                        IEBusController controller = bridge.getLibClient().getClient().getController();

                        if (controller instanceof EBusLowLevelController) {
                            IEBusConnection connection = ((EBusLowLevelController) controller).getConnection();

                            if (connection instanceof EBusEmulatorConnection) {
                                type = "bridge (emulator)";
                            } else {
                                type = "bridge";
                            }
                        } else if (controller instanceof EBusEbusdController) {
                            type = "bridge (ebusd)";
                        }

                    }

                    console.println(String.format("%-40s | %-40s | %-10s", thing.getUID(), thing.getLabel(), type));
                }
            }
        }
    }

    /**
     * @param args
     * @param console
     * @param bridge
     */
    private void devices(String[] args, Console console, @Nullable EBusBridgeHandler bridge) {
        if (bridge == null) {
            for (EBusBridgeHandler handler : getAllEBusBridgeHandlers()) {
                EBusClient client = handler.getLibClient().getClient();
                EBusDeviceTable deviceTable = client.getDeviceTable();

                Collection<IEBusCommandCollection> collections = client.getCommandCollections();

                console.print(EBusConsoleUtils.getDeviceTableInformation(collections, deviceTable));
            }

        } else {
            EBusClient client = bridge.getLibClient().getClient();
            EBusDeviceTable deviceTable = client.getDeviceTable();
            Collection<IEBusCommandCollection> collections = client.getCommandCollections();

            console.print(EBusConsoleUtils.getDeviceTableInformation(collections, deviceTable));

        }
    }

    /**
     * @param data
     * @param console
     */
    private void resolve(byte[] data, Console console) {
        EBusCommandRegistry registry = typeProvider.getCommandRegistry();

        if (registry == null) {
            throw new RuntimeException("Unable to get the command registry!");
        }

        console.println(EBusConsoleUtils.analyzeTelegram(registry, data));
    }

    @Override
    @SuppressWarnings({"java:S3776"})
    public void execute(String[] args, Console console) {
        try {
            if (args.length == 0) {
                list(args, console);
                return;
            }

            if (StringUtils.equals(args[0], SUBCMD_LIST)) {
                list(args, console);
                return;

            } else if (StringUtils.equals(args[0], SUBCMD_CHANNELS)) {
                listChannels(args, console);

            } else if (StringUtils.equals(args[0], SUBCMD_DEVICES)) {

                if (args.length == 2) {
                    devices(args, console, getBridge(args[1], console));
                } else {
                    devices(args, console, null);
                }

            } else if (StringUtils.equals(args[0], SUBCMD_RESOLVE)) {
                resolve(EBusUtils.toByteArray(args[1]), console);

            } else if (StringUtils.equals(args[0], SUBCMD_SEND)) {
                EBusBridgeHandler bridge = null;

                if (args.length == 3) {
                    bridge = getBridge(args[2], console);
                } else {
                    bridge = getFirstBridge(console);
                }

                if (bridge != null) {
                    EBusClient client = bridge.getLibClient().getClient();
                    byte[] data = EBusUtils.toByteArray(args[1]);

                    EBusConsoleSendReceiver sendReceiver = new EBusConsoleSendReceiver(client, console);

                    // send and wait for the result, self-removing from listener
                    try {
                        sendReceiver.send(data);
                    } catch (EBusDataException e) {
                        console.println("The send telegram is invalid! " + e.getMessage());
                    }
                }
            } else if (StringUtils.equals(args[0], SUBCMD_RELOAD)) {
                console.println("Reload all eBUS configurations ...");
                typeProvider.reload();

            } else if (StringUtils.equals(args[0], SUBCMD_UPDATE)) {
                Collection<EBusBridgeHandler> bridgeHandlers = getAllEBusBridgeHandlers();

                StringBuilder sb = new StringBuilder();
                sb.append("Refresh all available eBUS Things ...\n");
                sb.append("\n");
                sb.append(String.format("%-40s | %-40s | %-10s%n", "Thing UID", "Label", "Refreshed ?"));
                sb.append(String.format("%-40s-+-%-40s-+-%-10s%n", StringUtils.repeat("-", 40),
                        StringUtils.repeat("-", 40), StringUtils.repeat("-", 10)));

                for (EBusBridgeHandler bridgeHandler : bridgeHandlers) {
                    Bridge bridge = bridgeHandler.getThing();

                    for (Thing thing : bridge.getThings()) {
                        if (thing.getHandler() instanceof EBusHandler) {
                            EBusHandler handler = (EBusHandler) thing.getHandler();
                            if (handler != null) {
                                boolean status = handler.refreshThingConfiguration();
                                sb.append(String.format("%-40s | %-40s | %-10s%n", thing.getUID(), thing.getLabel(),
                                        status));
                            }
                        }
                    }
                }
                console.print(sb.toString());
            }
        } catch (EBusControllerException | EBusTypeProviderException e) {
            String message = e.getMessage();
            if(message != null) {
                console.print(message);
            }
        }
    }

    /**
     * @param console
     * @return
     */
    @Nullable
    private EBusBridgeHandler getFirstBridge(Console console) {
        Collection<EBusBridgeHandler> bridgeHandlers = getAllEBusBridgeHandlers();
        if (!bridgeHandlers.isEmpty()) {
            return bridgeHandlers.iterator().next();
        }

        console.println("Error: Unable to find an eBUS bridge");
        return null;
    }

    /**
     * @param bridgeUID
     * @param console
     * @return
     */
    @Nullable
    private EBusBridgeHandler getBridge(String bridgeUID, Console console) {
        ThingRegistry thingRegistry = this.thingRegistry;
        Thing thing = null;

        try {
            if (thingRegistry != null) {
                thing = thingRegistry.get(new ThingUID(bridgeUID));
            }
        } catch (IllegalArgumentException e) {
            console.println("Error: " + e.getMessage());
            return null;
        }

        if (thing == null) {
            console.println(String.format("Error: Unable to find a thing with thingUID %s", bridgeUID));
            return null;
        }

        if (!(thing.getHandler() instanceof EBusBridgeHandler)) {
            console.println(String.format("Error: Given thingUID %s is not an eBUS bridge!", bridgeUID));
            return null;
        }

        return (EBusBridgeHandler) thing.getHandler();
    }

    @Override
    public List<String> getUsages() {
        String line = "%s %s - %s";
        String line2 = "%s %s %s - %s";

        List<String> list = new ArrayList<>();
        list.add(String.format(line, CMD, SUBCMD_LIST, "lists all eBUS devices"));
        list.add(String.format(line2, CMD, SUBCMD_SEND, "\"<ebus telegram>\" [<bridgeUID>]",
                "sends a raw hex telegram to an eBUS bridge or if not set to first bridge"));
        list.add(String.format(line2, CMD, SUBCMD_DEVICES, "[<bridgeUID>]",
                "lists all devices connect to an eBUS bridge or list only a specific bridge"));
        list.add(String.format(line2, CMD, SUBCMD_RESOLVE, "\"<ebus telegram>\"", "resolves and analyze a telegram"));

        list.add(String.format(line, CMD, SUBCMD_RELOAD, "reload all defined json configuration files"));
        list.add(String.format(line, CMD, SUBCMD_UPDATE, "update all things to newest json configuration files"));

        list.add(String.format(line, CMD, SUBCMD_CHANNELS, "show all available things and it's channels"));

        return list;
    }
}
