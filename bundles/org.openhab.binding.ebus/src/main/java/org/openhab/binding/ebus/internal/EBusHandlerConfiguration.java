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

/**
 * The {@link EBusHandlerConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christian Sowada - Initial contribution
 */
public class EBusHandlerConfiguration {

    /**
     * Slave address of this node as HEX value
     */
    public String slaveAddress;

    /**
     * Master address of this node as HEX value. Usually does not have to be set. Calculated on the basis of the slave
     * address.
     */
    public String masterAddress;

    /**
     * Accept telegrams for master address
     */
    public Boolean filterAcceptMaster = false;

    /**
     * Accept telegrams for slave address<
     */
    public Boolean filterAcceptSlave = true;

    /**
     * Accept broadcasts telegrams from master address
     */
    public Boolean filterAcceptBroadcasts = true;

    /**
     * Set to poll all getter channels every n seconds.
     */
    public BigDecimal polling;

    @Override
    public String toString() {
        return "EBusHandlerConfiguration [slaveAddress=" + slaveAddress + ", masterAddress=" + masterAddress
                + ", filterAcceptMaster=" + filterAcceptMaster + ", filterAcceptSlave=" + filterAcceptSlave
                + ", filterAcceptBroadcasts=" + filterAcceptBroadcasts + ", polling=" + polling + "]";
    }
}
