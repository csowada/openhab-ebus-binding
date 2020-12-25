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

import java.math.BigDecimal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link EBusHandlerConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusHandlerConfiguration {

    /**
     * Slave address of this node as HEX value
     */
    public @Nullable String slaveAddress;

    /**
     * Master address of this node as HEX value. Usually does not have to be set. Calculated on the basis of the slave
     * address.
     */
    public @Nullable String masterAddress;

    /**
     * Accept telegrams for master address
     */
    public @Nullable Boolean filterAcceptMaster = false;

    /**
     * Accept telegrams for slave address<
     */
    public @Nullable Boolean filterAcceptSlave = true;

    /**
     * Accept broadcasts telegrams from master address
     */
    public @Nullable Boolean filterAcceptBroadcasts = true;

    /**
     * Set to poll all getter channels every n seconds.
     */
    public @Nullable BigDecimal polling;

    @Override
    public String toString() {
        return "EBusHandlerConfiguration [slaveAddress=" + slaveAddress + ", masterAddress=" + masterAddress
                + ", filterAcceptMaster=" + filterAcceptMaster + ", filterAcceptSlave=" + filterAcceptSlave
                + ", filterAcceptBroadcasts=" + filterAcceptBroadcasts + ", polling=" + polling + "]";
    }
}
