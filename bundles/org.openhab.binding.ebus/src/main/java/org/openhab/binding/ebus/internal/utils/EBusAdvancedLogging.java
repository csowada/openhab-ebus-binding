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
package org.openhab.binding.ebus.internal.utils;

import java.io.File;

import org.eclipse.jdt.annotation.NonNullByDefault;

import de.csdev.ebus.utils.EBusTelegramWriter;

/**
 * An openhab variant of the ebus core logger
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusAdvancedLogging extends EBusTelegramWriter {

    /**
     *
     */
    public EBusAdvancedLogging() {
        super(new File(System.getProperty("openhab.logdir")));
    }
}
