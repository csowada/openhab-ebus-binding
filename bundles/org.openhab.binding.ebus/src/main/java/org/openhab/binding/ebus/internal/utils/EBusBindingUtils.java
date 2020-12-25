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

import static org.openhab.binding.ebus.internal.EBusBindingConstants.BINDING_ID;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

import de.csdev.ebus.command.IEBusCommand;
import de.csdev.ebus.command.IEBusCommandCollection;
import de.csdev.ebus.command.IEBusCommandMethod;
import de.csdev.ebus.command.IEBusValue;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusBindingUtils {

    private EBusBindingUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Generates a channel type uid
     *
     * @param value
     * @return
     */
    public static ChannelTypeUID generateChannelTypeUID(IEBusValue value) {
        String id = generateValueId(value);
        Objects.requireNonNull(id);
        return new ChannelTypeUID(BINDING_ID, id);
    }

    /**
     * Generates a channel uid
     *
     * @param value
     * @param thingUID
     * @return
     */
    public static @Nullable ChannelUID generateChannelUID(IEBusValue value, ThingUID thingUID) {
        IEBusCommandMethod method = value.getParent();

        if (method == null) {
            return null;
        }

        IEBusCommand command = method.getParent();

        String name = value.getName();
        if (name == null) {
            return null;
        }

        return new ChannelUID(thingUID, generateChannelGroupID(command), formatId(name));
    }

    /**
     * Generates a channel uid
     *
     * @param command
     * @param valueName
     * @param thingUID
     * @return
     */
    public static ChannelUID generateChannelUID(IEBusCommand command, String valueName, ThingUID thingUID) {
        return new ChannelUID(thingUID, generateChannelGroupID(command), formatId(valueName));
    }

    /**
     * Generates a channel group uid
     *
     * @param command
     * @return
     */
    public static ChannelGroupTypeUID generateChannelGroupTypeUID(IEBusCommand command) {
        return new ChannelGroupTypeUID(BINDING_ID, generateChannelGroupID(command));
    }

    /**
     * Generates a thing type uid
     *
     * @param collection
     * @return
     */
    public static ThingTypeUID generateThingTypeUID(IEBusCommandCollection collection) {
        return new ThingTypeUID(BINDING_ID, formatCollectionId(collection));
    }

    /**
     * Generates a channel group id
     *
     * @param command
     * @return
     */
    public static String generateChannelGroupID(IEBusCommand command) {
        IEBusCommandCollection parentCollection = command.getParentCollection();
        return String.format("%s_%s", formatCollectionId(parentCollection), formatId(command.getId()));
    }

    /**
     * Generates a value id
     *
     * @param value
     * @return
     */
    public static @Nullable String generateValueId(IEBusValue value) {
        IEBusCommandMethod method = value.getParent();

        if (method == null) {
            return null;
        }

        IEBusCommand command = method.getParent();

        String name = value.getName();
        if (name == null) {
            return null;
        }

        return String.format("%s_%s", generateChannelGroupID(command), formatId(name));
    }

    /**
     * Format id string and replace all underscore and dot characters
     *
     * @param id
     * @return
     */
    public static String formatId(String id) {
        return id.replace('_', '-').replace('.', '_');
    }

    /**
     *
     * @param collection
     * @return
     */
    public static String formatCollectionId(IEBusCommandCollection collection) {
        return collection.getId().replace(' ', 'o');
    }
}
