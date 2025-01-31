/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.*;

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link EBusBridgeHandlerConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusBridgeHandlerConfiguration {

    public @Nullable String masterAddress;

    public @Nullable String slaveAddress;

    public @Nullable String serialPort;

    public @Nullable String ipAddress;

    public @Nullable BigDecimal port;

    public @Nullable String raw;

    public @Nullable String ebusd;

    public @Nullable String networkDriver = DRIVER_RAW;

    public @Nullable String serialPortDriver = DRIVER_BUILDIN;

    public @Nullable Boolean advancedLogging;

    public @Nullable String configurationUrl;

    public @Nullable String configurationUrl1;

    public @Nullable String configurationUrl2;

    public @Nullable String configurationBundleUrl;

    @Override
    public String toString() {
        return "EBusBridgeHandlerConfiguration [masterAddress=" + masterAddress + ", slaveAddress=" + slaveAddress
                + ", serialPort=" + serialPort + ", ipAddress=" + ipAddress + ", port=" + port + ", raw=" + raw
                + ", ebusd=" + ebusd + ", networkDriver=" + networkDriver + ", serialPortDriver=" + serialPortDriver
                + ", advancedLogging=" + advancedLogging + ", configurationUrl=" + configurationUrl
                + ", configurationUrl1=" + configurationUrl1 + ", configurationUrl2=" + configurationUrl2
                + ", configurationBundleUrl=" + configurationBundleUrl + "]";
    }
}
