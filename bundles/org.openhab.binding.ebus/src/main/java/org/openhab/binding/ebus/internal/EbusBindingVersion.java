/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

import de.csdev.ebus.core.EBusVersion;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EbusBindingVersion extends EBusVersion {
    public static String getVersion() {
        return getVersion(EbusBindingVersion.class);
    }
}
