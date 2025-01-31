/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
