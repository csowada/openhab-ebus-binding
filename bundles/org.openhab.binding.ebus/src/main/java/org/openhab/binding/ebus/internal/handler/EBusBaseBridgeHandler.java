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
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.State;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public abstract class EBusBaseBridgeHandler extends BaseBridgeHandler implements IEBusBridgeHandler {

    protected EBusBaseBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public ScheduledExecutorService getBindingScheduler() {
        return scheduler;
    }

    @Override
    public void updateState(ChannelUID channelUID, State state) {
        super.updateState(channelUID, state);
    }

    @Override
    public void updateState(String channelID, State state) {
        super.updateState(channelID, state);
    }
}
