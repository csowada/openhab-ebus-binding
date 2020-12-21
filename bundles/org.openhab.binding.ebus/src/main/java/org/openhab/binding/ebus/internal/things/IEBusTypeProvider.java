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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.binding.ThingTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeUID;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;

import de.csdev.ebus.command.EBusCommandRegistry;
import de.csdev.ebus.command.IEBusCommandCollection;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public interface IEBusTypeProvider extends ThingTypeProvider, ChannelGroupTypeProvider, ChannelTypeProvider {

    /**
     * @param collections
     */
    public void update(List<IEBusCommandCollection> collections) throws EBusTypeProviderException;

    /**
     * @return
     */
    @Nullable
    public EBusCommandRegistry getCommandRegistry();

    /**
     * @return
     */
    public boolean reload() throws EBusTypeProviderException;

    /**
     * @see ChannelTypeRegistry#getChannelGroupType(ChannelGroupTypeUID, Locale)
     */
    @Override
    @Nullable
    ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID, @Nullable Locale locale);

    /**
     * @see ChannelTypeRegistry#getChannelGroupTypes(Locale)
     */
    @Override
    Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale);
}
