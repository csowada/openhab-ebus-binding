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
package org.openhab.binding.ebus.internal;

import static org.openhab.binding.ebus.internal.EBusBindingConstants.*;

import java.math.BigDecimal;

/**
 * The {@link EBusBridgeHandlerConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusBridgeHandlerConfiguration {

    public String masterAddress;

    public String slaveAddress;

    public String serialPort;

    public String ipAddress;

    public BigDecimal port;

    public String raw;

    public String ebusd;

    public String networkDriver = DRIVER_RAW;

    public String serialPortDriver = DRIVER_NRJAVASERIAL;

    public Boolean advancedLogging;

    public String configurationUrl;

    public String configurationUrl1;

    public String configurationUrl2;

    public String configurationBundleUrl;

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
