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
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link EBusBindingConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusBindingConfiguration {

    /**
     * Define a URL to load external configuration files.
     */

    public @Nullable String configurationUrl;

    /**
     * Define a URL to load external configuration files.
     */
    public @Nullable String configurationUrl1;

    /**
     * Define a URL to load external configuration files.
     */
    public @Nullable String configurationUrl2;

    /**
     * Define a URL to load external configuration bundles
     */
    public @Nullable String configurationBundleUrl;

    @Override
    public String toString() {
        return "EBusBindingConfiguration [configurationUrl=" + configurationUrl + ", configurationUrl1="
                + configurationUrl1 + ", configurationUrl2=" + configurationUrl2 + ", configurationBundleUrl="
                + configurationBundleUrl + "]";
    }
}
