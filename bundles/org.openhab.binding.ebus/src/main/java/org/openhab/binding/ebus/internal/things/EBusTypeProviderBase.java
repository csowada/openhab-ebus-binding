/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal.things;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.THING_TYPE_EBUS_BRIDGE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.ThingType;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public abstract class EBusTypeProviderBase
        implements IEBusTypeProvider {

    protected final List<String> supportedBridgeTypeUIDs = Arrays.asList(THING_TYPE_EBUS_BRIDGE.getAsString());

    protected Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypes = new HashMap<>();

    protected Map<ChannelTypeUID, ChannelType> channelTypes = new HashMap<>();

    protected Map<ThingTypeUID, ThingType> thingTypes = new HashMap<>();

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        return channelGroupTypes.get(channelGroupTypeUID);
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return channelGroupTypes.values();
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return channelTypes.get(channelTypeUID);
    }

    @Override
    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return channelTypes.values();
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        return thingTypes.get(thingTypeUID);
    }

    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return thingTypes.values();
    }
}
