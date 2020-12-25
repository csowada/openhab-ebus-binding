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
import java.math.BigDecimal;
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
import org.eclipse.jdt.annotation.NonNullByDefault;
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
@NonNullByDefault
@Component(service = { IEBusTypeProvider.class, ThingTypeProvider.class, ChannelTypeProvider.class,
        ChannelGroupTypeProvider.class }, configurationPid = BINDING_PID, immediate = true)
public class EBusTypeProviderImpl extends EBusTypeProviderBase implements IEBusTypeProvider {

    private final Logger logger = LoggerFactory.getLogger(EBusTypeProviderImpl.class);

    private @Nullable EBusCommandRegistry commandRegistry;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    private @Nullable ConfigurationAdmin configurationAdmin;

    /**
     * Activating this component - called from DS.
     *
     * @param componentContext
     */
    @Activate
    public void activate(ComponentContext componentContext) {

        logger.trace("Loading eBUS Type Provider ...");

        commandRegistry = new EBusCommandRegistry(EBusConfigurationReaderExt.class, false);
        try {
            Dictionary<String, Object> properties = componentContext.getProperties();
            if (properties != null) {
                updateConfiguration(properties);
            }
        } catch (EBusTypeProviderException e) {
            logger.error("error!", e);
        }
    }

    /**
     * @param command
     * @param mainChannel
     * @param value
     * @return
     * @throws EBusTypeProviderException
     */
    @Nullable
    private ChannelDefinition createChannelDefinition(IEBusCommandMethod mainMethod, IEBusValue value)
            throws EBusTypeProviderException {

        ChannelType channelType = createChannelType(value, mainMethod);

        if (channelType != null) {

            logger.trace("Add channel {} for method {}", channelType.getUID(), mainMethod.getMethod());

            // add to global list
            ChannelTypeUID channelTypeUID = channelType.getUID();
            channelTypes.put(channelTypeUID, channelType);

            String name = value.getName();
            if (name == null) {
                throw new EBusTypeProviderException("Unable to get name from value!");
            }

            // store command id and value name
            Map<String, String> properties = new HashMap<>();
            properties.put(COMMAND, mainMethod.getParent().getId());
            properties.put(VALUE_NAME, name);

            String id = EBusBindingUtils.formatId(name);
            ChannelDefinitionBuilder builder = new ChannelDefinitionBuilder(id, channelTypeUID);

            return builder.withProperties(properties).withLabel(value.getLabel()).build();
        }

        return null;
    }

    private <T> T nullCheck(@Nullable T value) throws EBusTypeProviderException {

        if (value == null) {
            throw new EBusTypeProviderException("xxxxx");
        }

        return value;
    }

    /**
     * @param command
     * @param channelDefinitions
     * @return
     * @throws EBusTypeProviderException
     */
    private ChannelGroupDefinition createChannelGroupDefinition(IEBusCommand command,
            List<ChannelDefinition> channelDefinitions) throws EBusTypeProviderException {

        ChannelGroupTypeUID groupTypeUID = EBusBindingUtils.generateChannelGroupTypeUID(command);

        String label = StringUtils.defaultIfEmpty(command.getLabel(), "-undefined-");

        if (label == null) {
            throw new EBusTypeProviderException("Unable to generate label!");
        }

        ChannelGroupType cgt = ChannelGroupTypeBuilder.instance(groupTypeUID, label).isAdvanced(false)
                .withCategory(command.getId()).withChannelDefinitions(channelDefinitions).withDescription("HVAC")
                .build();

        // add to global list
        channelGroupTypes.put(nullCheck(cgt.getUID()), cgt);

        String cgdid = EBusBindingUtils.generateChannelGroupID(command);

        return new ChannelGroupDefinition(cgdid, groupTypeUID, command.getLabel(), command.getId());
    }

    /**
     * @param value
     * @param mainChannel
     * @return
     * @throws EBusTypeProviderException
     */
    @Nullable
    private ChannelType createChannelType(IEBusValue value, IEBusCommandMethod mainMethod)
            throws EBusTypeProviderException {

        // only process valid entries
        String name = value.getName();
        if (name != null && StringUtils.isNotEmpty(name) && StringUtils.isNotEmpty(mainMethod.getParent().getId())) {

            ChannelTypeUID uid = EBusBindingUtils.generateChannelTypeUID(value);

            IEBusCommandMethod commandSetter = mainMethod.getParent().getCommandMethod(IEBusCommandMethod.Method.SET);

            boolean readOnly = commandSetter == null;
            boolean polling = mainMethod.getType().equals(IEBusCommandMethod.Type.MASTER_SLAVE);

            // create a option list if mapping is used
            List<StateOption> options = null;
            Map<String, String> mappings = value.getMapping();

            if (mappings != null && !mappings.isEmpty()) {
                options = new ArrayList<>();
                for (Entry<String, String> entry : mappings.entrySet()) {
                    options.add(new StateOption(entry.getKey(), entry.getValue()));
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
                itemType = EBusBindingConstants.ITEM_TYPE_STRING;
            }

            boolean advanced = name.startsWith("_");
            String label = StringUtils.defaultIfEmpty(value.getLabel(), value.getName());
            String pattern = value.getFormat();

            StateDescriptionFragmentBuilder stateBuilder = StateDescriptionFragmentBuilder.create()
                    .withReadOnly(readOnly);

            BigDecimal val = value.getMin();
            if (val != null) {
                stateBuilder.withMinimum(val);
            }

            val = value.getMax();
            if (val != null) {
                stateBuilder.withMaximum(val);
            }

            val = value.getStep();
            if (val != null) {
                stateBuilder.withStep(val);
            }

            if (pattern != null) {
                stateBuilder.withPattern(pattern);
            }

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

            if (label == null) {
                throw new EBusTypeProviderException("No label available!");
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
     * @throws EBusTypeProviderException
     */
    private ThingType createThingType(IEBusCommandCollection collection,
            @Nullable ArrayList<ChannelDefinition> channelDefinitions,
            List<ChannelGroupDefinition> channelGroupDefinitions) throws EBusTypeProviderException {

        ThingTypeUID thingTypeUID = EBusBindingUtils.generateThingTypeUID(collection);

        String label = collection.getLabel();

        if (label == null) {
            throw new EBusTypeProviderException("No label for collection available!");
        }

        String description = collection.getDescription();

        Map<String, String> properties = new HashMap<>();

        String hash = String.valueOf(collection.hashCode());

        if (hash == null) {
            throw new EBusTypeProviderException("Unable to generate hash!");
        }

        properties.put("collectionHash", hash);

        ThingTypeBuilder builder = ThingTypeBuilder.instance(thingTypeUID, label)
                .withSupportedBridgeTypeUIDs(supportedBridgeTypeUIDs)
                .withChannelGroupDefinitions(channelGroupDefinitions)
                .withConfigDescriptionURI(CONFIG_DESCRIPTION_URI_NODE).withProperties(properties);

        if (description != null) {
            builder.withDescription(description);
        }

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

        if (this.commandRegistry != null) {
            this.commandRegistry.clear();
            this.commandRegistry = null;
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
    public boolean reload() throws EBusTypeProviderException {

        try {
            if (this.configurationAdmin != null) {
                Configuration configuration = this.configurationAdmin.getConfiguration(BINDING_PID, null);

                Dictionary<String, Object> properties = configuration.getProperties();
                if (properties != null && !properties.isEmpty()) {
                    updateConfiguration(properties);
                }
            }
            return true;

        } catch (IOException e) {
            logger.error("error!", e);
        }

        return false;
    }

    @Override
    public void update(List<IEBusCommandCollection> collections) throws EBusTypeProviderException {

        for (IEBusCommandCollection collection : collections) {
            if (StringUtils.isNotEmpty(collection.getId())) {
                updateCollection(collection);
            }
        }

        logger.debug("Generated all eBUS command collections ...");
    }

    /**
     * @param collection
     * @throws EBusTypeProviderException
     */
    private void updateCollection(IEBusCommandCollection collection) throws EBusTypeProviderException {

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

                List<IEBusValue> masterTypes = mainMethod.getMasterTypes();
                if (masterTypes != null && !masterTypes.isEmpty()) {
                    list.addAll(masterTypes);
                }

                List<IEBusValue> slaveTypes = mainMethod.getSlaveTypes();
                if (slaveTypes != null && !slaveTypes.isEmpty()) {
                    list.addAll(slaveTypes);
                }

                // now check for nested values
                List<IEBusValue> childList = new ArrayList<>();
                for (IEBusValue value : list) {
                    if (value instanceof IEBusNestedValue) {
                        IEBusNestedValue val = (IEBusNestedValue) value;
                        childList.addAll(val.getChildren());
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

    private @Nullable EBusBindingConfiguration getConfiguration(Dictionary<String, ?> properties) {
        List<String> keys = Collections.list(properties.keys());
        Map<String, Object> dictCopy = keys.stream().collect(Collectors.toMap(Function.identity(), properties::get));

        if (dictCopy != null) {
            org.eclipse.smarthome.config.core.Configuration c = new org.eclipse.smarthome.config.core.Configuration(
                    dictCopy);
            return c.as(EBusBindingConfiguration.class);
        }

        return null;
    }

    private void updateConfiguration(@Nullable Dictionary<String, ?> properties) throws EBusTypeProviderException {

        if (properties == null) {
            return;
        }

        logger.trace("Update eBUS configuration ...");

        EBusBindingConfiguration configuration = getConfiguration(properties);

        EBusCommandRegistry cmdRegistry = this.commandRegistry;

        // Map
        if (cmdRegistry == null || configuration == null) {
            return;
        }

        cmdRegistry.clear();

        cmdRegistry.loadBuildInCommandCollections();

        if (!properties.isEmpty()) {

            String configurationUrl = configuration.configurationUrl;
            if (configurationUrl != null) {
                logger.info("Load custom 'url' configuration file '{}' ...", configurationUrl);
                loadConfigurationByUrl(cmdRegistry, configurationUrl);
            }

            String configurationUrl1 = configuration.configurationUrl1;
            if (configurationUrl1 != null) {
                logger.info("Load custom 'url1' configuration file '{}' ...", configurationUrl1);
                loadConfigurationByUrl(cmdRegistry, configurationUrl1);
            }

            String configurationUrl2 = configuration.configurationUrl2;
            if (configurationUrl2 != null) {
                logger.info("Load custom 'url2' configuration file '{}' ...", configurationUrl2);
                loadConfigurationByUrl(cmdRegistry, configurationUrl2);
            }

            String configurationBundleUrl = configuration.configurationBundleUrl;
            if (configurationBundleUrl != null) {
                logger.info("Load custom 'bundleUrl' configuration bundle '{}' ...", configurationBundleUrl);
                loadConfigurationBundleByUrl(cmdRegistry, configurationBundleUrl);
            }
        }

        update(commandRegistry.getCommandCollections());
    }
}
