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
