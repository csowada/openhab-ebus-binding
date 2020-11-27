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
package org.openhab.binding.ebus.internal.things;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelDefinitionBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupDefinition;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.ThingType;
import org.eclipse.smarthome.core.thing.type.ThingTypeBuilder;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.openhab.binding.ebus.internal.EBusBindingConfiguration;
import org.openhab.binding.ebus.internal.EBusBindingConstants;
import org.openhab.binding.ebus.internal.utils.EBusBindingUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusNestedValue;
import de.csdev.ebus.command.IEBusValue;
import de.csdev.ebus.command.datatypes.ext.EBusTypeBytes;
import de.csdev.ebus.command.datatypes.ext.EBusTypeDate;
import de.csdev.ebus.command.datatypes.ext.EBusTypeDateTime;
import de.csdev.ebus.command.datatypes.ext.EBusTypeString;
import de.csdev.ebus.command.datatypes.ext.EBusTypeTime;
import de.csdev.ebus.command.datatypes.std.EBusTypeBit;
import de.csdev.ebus.configuration.EBusConfigurationReaderExt;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@Component(service = { IEBusTypeProvider.class, ThingTypeProvider.class, ChannelTypeProvider.class,
        ChannelGroupTypeProvider.class }, configurationPid = BINDING_PID, immediate = true)
public class EBusTypeProviderImpl extends EBusTypeProviderBase implements IEBusTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(EBusTypeProviderImpl.class);

    @Nullable
    private EBusCommandRegistry commandRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private ConfigurationAdmin configurationAdmin;

    /**
     * Activating this component - called from DS.
     *
     * @param componentContext
     */
    @Activate
    public void activate(ComponentContext componentContext) {

        logger.trace("Loading eBUS Type Provider ...");

        commandRegistry = new EBusCommandRegistry(EBusConfigurationReaderExt.class, false);
        updateConfiguration(componentContext.getProperties());
    }

    /**
     * @param command
     * @param mainChannel
     * @param value
     * @return
     */
    @Nullable
    private ChannelDefinition createChannelDefinition(IEBusCommandMethod mainMethod, IEBusValue value) {

        ChannelType channelType = createChannelType(value, mainMethod);

        if (channelType != null) {

            logger.trace("Add channel {} for method {}", channelType.getUID(), mainMethod.getMethod());

            // add to global list
            channelTypes.put(channelType.getUID(), channelType);

            // store command id and value name
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(COMMAND, mainMethod.getParent().getId());
            properties.put(VALUE_NAME, value.getName());

            String id = EBusBindingUtils.formatId(value.getName());
            ChannelDefinitionBuilder builder = new ChannelDefinitionBuilder(id, channelType.getUID());

            return builder.withProperties(properties).withLabel(value.getLabel()).build();
        }

        return null;
    }

    /**
     * @param command
     * @param channelDefinitions
     * @return
     */
    private ChannelGroupDefinition createChannelGroupDefinition(IEBusCommand command,
            List<ChannelDefinition> channelDefinitions) {

        ChannelGroupTypeUID groupTypeUID = EBusBindingUtils.generateChannelGroupTypeUID(command);

        ChannelGroupType cgt = ChannelGroupTypeBuilder
                .instance(groupTypeUID, StringUtils.defaultIfEmpty(command.getLabel(), "-undefined-")).isAdvanced(false)
                .withCategory(command.getId()).withChannelDefinitions(channelDefinitions).withDescription("HVAC")
                .build();

        // add to global list
        channelGroupTypes.put(cgt.getUID(), cgt);

        String cgdid = EBusBindingUtils.generateChannelGroupID(command);

        return new ChannelGroupDefinition(cgdid, groupTypeUID, command.getLabel(), command.getId());
    }

    /**
     * @param value
     * @param mainChannel
     * @return
     */
    @Nullable
    private ChannelType createChannelType(IEBusValue value, IEBusCommandMethod mainMethod) {

        // only process valid entries
        if (StringUtils.isNotEmpty(value.getName()) && StringUtils.isNotEmpty(mainMethod.getParent().getId())) {

            ChannelTypeUID uid = EBusBindingUtils.generateChannelTypeUID(value);

            IEBusCommandMethod commandSetter = mainMethod.getParent().getCommandMethod(IEBusCommandMethod.Method.SET);

            boolean readOnly = commandSetter == null;
            boolean polling = mainMethod.getType().equals(IEBusCommandMethod.Type.MASTER_SLAVE);

            // create a option list if mapping is used
            List<StateOption> options = null;
            if (value.getMapping() != null && !value.getMapping().isEmpty()) {
                options = new ArrayList<StateOption>();
                for (Entry<String, String> mapping : value.getMapping().entrySet()) {
                    options.add(new StateOption(mapping.getKey(), mapping.getValue()));
                }
            }

            // default
            String itemType = EBusBindingConstants.ITEM_TYPE_NUMBER;

            if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeBit.TYPE_BIT)) {
                itemType = EBusBindingConstants.ITEM_TYPE_SWITCH;

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeDateTime.TYPE_DATETIME)) {
                itemType = EBusBindingConstants.ITEM_TYPE_DATETIME;

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeDate.TYPE_DATE)) {
                itemType = EBusBindingConstants.ITEM_TYPE_DATETIME;

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeTime.TYPE_TIME)) {
                itemType = EBusBindingConstants.ITEM_TYPE_DATETIME;

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeString.TYPE_STRING)) {
                itemType = EBusBindingConstants.ITEM_TYPE_STRING;

            } else if (ArrayUtils.contains(value.getType().getSupportedTypes(), EBusTypeBytes.TYPE_BYTES)) {
                itemType = EBusBindingConstants.ITEM_TYPE_STRING;

            } else if (options != null) {
                // options works only for string! or in not readOnly mode
                // itemType = "Number";
                itemType = EBusBindingConstants.ITEM_TYPE_STRING;
            }

            boolean advanced = value.getName().startsWith("_");
            String label = StringUtils.defaultIfEmpty(value.getLabel(), value.getName());
            String pattern = value.getFormat();

            StateDescriptionFragmentBuilder stateBuilder = StateDescriptionFragmentBuilder.create()
                    .withMinimum(value.getMin()).withMaximum(value.getMax()).withStep(value.getStep())
                    .withPattern(pattern).withReadOnly(readOnly);

            if (options != null) {
                stateBuilder.withOptions(options);
            }

            StateDescription state = stateBuilder.build().toStateDescription();

            URI configDescriptionURI = polling ? CONFIG_DESCRIPTION_URI_POLLING_CHANNEL
                    : CONFIG_DESCRIPTION_URI_NULL_CHANNEL;

            // apply new quantity extension
            if (itemType.equals(EBusBindingConstants.ITEM_TYPE_NUMBER) && label.contains("°C")) {
                label = label.replace("°C", "%unit%");
                itemType = EBusBindingConstants.ITEM_TYPE_TEMPERATURE;
            }

            if (StringUtils.isEmpty(itemType)) {
                logger.warn("Label type for {}/{} is undefined!", uid, label);
            }

            return ChannelTypeBuilder.state(uid, label, itemType).withConfigDescriptionURI(configDescriptionURI)
                    .isAdvanced(advanced).withStateDescription(state).build();
        }

        return null;
    }

    /**
     * @param collection
     * @param channelDefinitions
     * @param channelGroupDefinitions
     * @return
     */
    private ThingType createThingType(IEBusCommandCollection collection,
            @Nullable ArrayList<ChannelDefinition> channelDefinitions,
            List<ChannelGroupDefinition> channelGroupDefinitions) {

        ThingTypeUID thingTypeUID = EBusBindingUtils.generateThingTypeUID(collection);

        String label = collection.getLabel();
        String description = collection.getDescription();

        Map<String, String> properties = new HashMap<String, String>();
        properties.put("collectionHash", java.lang.String.valueOf(collection.hashCode()));

        ThingTypeBuilder builder = ThingTypeBuilder.instance(thingTypeUID, label)
                .withSupportedBridgeTypeUIDs(supportedBridgeTypeUIDs)
                .withChannelGroupDefinitions(channelGroupDefinitions)
                .withConfigDescriptionURI(CONFIG_DESCRIPTION_URI_NODE).withDescription(description)
                .withProperties(properties);

        if (channelDefinitions != null) {
            builder.withChannelDefinitions(channelDefinitions);
        }

        return builder.build();
    }

    /**
     * Deactivating this component - called from DS.
     *
     * @param componentContext
     */
    @Deactivate
    public void deactivate(ComponentContext componentContext) {

        logger.trace("Stopping eBUS Type Provider ...");

        channelGroupTypes.clear();
        channelTypes.clear();
        thingTypes.clear();

        if (commandRegistry != null) {
            commandRegistry.clear();
            commandRegistry = null;
        }
    }

    @Override
    public @Nullable EBusCommandRegistry getCommandRegistry() {
        return commandRegistry;
    }

    /**
     * @param url
     * @return
     */
    private boolean loadConfigurationBundleByUrl(EBusCommandRegistry commandRegistry, String url) {
        try {
            commandRegistry.loadCommandCollectionBundle(new URL(url));
            return true;

        } catch (MalformedURLException e) {
            logger.error("Error on loading configuration by url: {}", e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * @param configuration
     * @param url
     */
    private boolean loadConfigurationByUrl(EBusCommandRegistry commandRegistry, String url) {
        try {
            commandRegistry.loadCommandCollection(new URL(url));
            return true;

        } catch (MalformedURLException e) {
            logger.error("Error on loading configuration by url: {}", e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public boolean reload() {

        try {
            if (configurationAdmin != null) {
                Configuration configuration = configurationAdmin.getConfiguration(BINDING_PID, null);
                updateConfiguration(configuration.getProperties());
            }
            return true;

        } catch (IOException e) {
            logger.error("error!", e);
        }

        return false;
    }

    @Override
    public void update(List<IEBusCommandCollection> collections) {

        for (IEBusCommandCollection collection : collections) {
            if (StringUtils.isNotEmpty(collection.getId())) {
                updateCollection(collection);
            }
        }

        logger.debug("Generated all eBUS command collections ...");
    }

    /**
     * @param collection
     */
    private void updateCollection(IEBusCommandCollection collection) {

        // don't add empty command collections, in most cases template files
        if (collection.getCommands().isEmpty()) {
            logger.trace("eBUS command collection {} is empty, ignore ...", collection.getId());
            return;
        }

        List<ChannelGroupDefinition> channelGroupDefinitions = new ArrayList<>();

        for (IEBusCommand command : collection.getCommands()) {

            List<ChannelDefinition> channelDefinitions = new ArrayList<>();
            List<IEBusValue> list = new ArrayList<>();

            Collection<IEBusCommandMethod.Method> commandChannelTypes = command.getCommandChannelMethods();

            IEBusCommandMethod mainMethod = null;
            if (commandChannelTypes.contains(IEBusCommandMethod.Method.GET)) {
                mainMethod = command.getCommandMethod(IEBusCommandMethod.Method.GET);

            } else if (commandChannelTypes.contains(IEBusCommandMethod.Method.BROADCAST)) {
                mainMethod = command.getCommandMethod(IEBusCommandMethod.Method.BROADCAST);

            } else if (commandChannelTypes.contains(IEBusCommandMethod.Method.SET)) {
                logger.warn("eBUS command {} only contains a setter channel!", command.getId());
                mainMethod = command.getCommandMethod(IEBusCommandMethod.Method.SET);

            } else {
                logger.warn("eBUS command {} doesn't contain a known channel!", command.getId());

            }

            if (mainMethod != null) {

                if (mainMethod.getMasterTypes() != null && !mainMethod.getMasterTypes().isEmpty()) {
                    list.addAll(mainMethod.getMasterTypes());
                }

                if (mainMethod.getSlaveTypes() != null && !mainMethod.getSlaveTypes().isEmpty()) {
                    list.addAll(mainMethod.getSlaveTypes());
                }

                // now check for nested values
                List<IEBusValue> childList = new ArrayList<IEBusValue>();
                for (IEBusValue value : list) {
                    if (value instanceof IEBusNestedValue) {
                        childList.addAll(((IEBusNestedValue) value).getChildren());
                    }
                }
                list.addAll(childList);

                // *****************************************
                // generate a channel for each ebus value
                // *****************************************

                for (IEBusValue value : list) {
                    if (StringUtils.isNotEmpty(value.getName())) {

                        ChannelDefinition channelDefinition = createChannelDefinition(mainMethod, value);
                        if (channelDefinition != null) {
                            logger.trace("Add channel definition {}", value.getName());
                            channelDefinitions.add(channelDefinition);
                        }
                    }
                }
            }

            // *****************************************
            // create a channel group for each command
            // *****************************************

            if (StringUtils.isNotEmpty(command.getId())) {
                ChannelGroupDefinition channelGroupDefinition = createChannelGroupDefinition(command,
                        channelDefinitions);
                channelGroupDefinitions.add(channelGroupDefinition);
            }

        }

        // *****************************************
        // generate a thing for this collection
        // *****************************************
        ThingType thingType = createThingType(collection, null, channelGroupDefinitions);
        thingTypes.put(thingType.getUID(), thingType);
    }

    private EBusBindingConfiguration getConfiguration(Dictionary<String, ?> properties) {
        List<String> keys = Collections.list(properties.keys());
        Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), properties::get));

        org.eclipse.smarthome.config.core.Configuration c = new org.eclipse.smarthome.config.core.Configuration(
                dictCopy);

        return c.as(EBusBindingConfiguration.class);
    }

    @SuppressWarnings("null")
    private void updateConfiguration(@Nullable Dictionary<String, ?> properties) {

        logger.trace("Update eBUS configuration ...");

        EBusBindingConfiguration configuration = getConfiguration(properties);

        // Map
        if (commandRegistry == null) {
            return;
        }

        commandRegistry.clear();

        commandRegistry.loadBuildInCommandCollections();

        if (properties != null && !properties.isEmpty()) {

            if (configuration.configurationUrl != null) {
                logger.info("Load custom configuration file '{}' ...", configuration.configurationUrl);
                if (commandRegistry != null) {

                    loadConfigurationByUrl(commandRegistry, configuration.configurationUrl);
                }
            }

            if (configuration.configurationUrl1 != null) {
                logger.info("Load custom configuration file '{}' ...", configuration.configurationUrl1);
                if (commandRegistry != null) {
                    loadConfigurationByUrl(commandRegistry, configuration.configurationUrl1);
                }
            }

            if (configuration.configurationUrl2 != null) {
                logger.info("Load custom configuration file '{}' ...", configuration.configurationUrl2);
                if (commandRegistry != null) {
                    loadConfigurationByUrl(commandRegistry, configuration.configurationUrl2);
                }
            }

            if (configuration.configurationBundleUrl != null) {
                logger.info("Load custom configuration bundle '{}' ...", configuration.configurationBundleUrl);
                if (commandRegistry != null) {
                    loadConfigurationBundleByUrl(commandRegistry, configuration.configurationBundleUrl);
                }
            }
        }

        update(commandRegistry.getCommandCollections());
    }
}
