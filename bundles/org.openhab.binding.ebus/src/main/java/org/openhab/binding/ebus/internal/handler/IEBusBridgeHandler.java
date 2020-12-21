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
