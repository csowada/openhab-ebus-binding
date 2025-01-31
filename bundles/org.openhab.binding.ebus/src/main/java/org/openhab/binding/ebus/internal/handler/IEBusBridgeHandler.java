/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal.handler;

import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.ebus.internal.utils.EBusClientBridge;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.State;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public interface IEBusBridgeHandler extends ThingHandler {

    /**
     * Returns the eBUS core lib client
     *
     * @return
     */
    public EBusClientBridge getLibClient();

    @Override
    public Bridge getThing();

    public ScheduledExecutorService getBindingScheduler();

    public void updateState(ChannelUID channelUID, State state);

    public void updateState(String channelID, State state);
}
