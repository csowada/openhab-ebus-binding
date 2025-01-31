/**
 * Copyright (c) 2017-2025 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ebus.internal.things;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 *
 * @author Christian Sowada - Initial contribution
 */
@NonNullByDefault
public class EBusTypeProviderException extends Exception {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    public EBusTypeProviderException() {
        super();
    }

    public EBusTypeProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public EBusTypeProviderException(String message) {
        super(message);
    }

    public EBusTypeProviderException(Throwable cause) {
        super(cause);
    }
}
