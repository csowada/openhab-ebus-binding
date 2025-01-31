/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal.things;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelTypeProvider;

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
