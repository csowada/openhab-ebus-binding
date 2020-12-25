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
