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
package org.openhab.binding.ebus.internal.serial;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.transport.serial.PortInUseException;
import org.openhab.core.io.transport.serial.SerialPort;
import org.openhab.core.io.transport.serial.SerialPortEvent;
import org.openhab.core.io.transport.serial.SerialPortEventListener;
import org.openhab.core.io.transport.serial.SerialPortIdentifier;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.io.transport.serial.UnsupportedCommOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.csdev.ebus.core.connection.AbstractEBusConnection;
import de.csdev.ebus.utils.CommonsUtils;

/**
 * @author Christian Sowada - Initial contribution
 *
 */
@NonNullByDefault
public class EBusSerialBuildInSerialConnection extends AbstractEBusConnection {

    private final Logger logger = LoggerFactory.getLogger(EBusSerialBuildInSerialConnection.class);

    /** The serial object */
    @Nullable
    private SerialPort serialPort;

    /** The serial port name */
    private String port;

    private SerialPortManager serialPortManager;

    public EBusSerialBuildInSerialConnection(final SerialPortManager serialPortManager, final String port) {
        this.port = port;
        this.serialPortManager = serialPortManager;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.connection.IEBusConnection#open()
     */
    @Override
    public boolean open() throws IOException {
        try {

            final SerialPortIdentifier portIdentifier = serialPortManager.getIdentifier(port);

            if (portIdentifier != null) {

                logger.info(
                        "Use openhab build-in serial driver .................................................................");

                SerialPort serialPort = portIdentifier.open(this.getClass().getName(), 3000);
                serialPort.setSerialPortParams(2400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

                serialPort.enableReceiveThreshold(1);
                serialPort.disableReceiveTimeout();

                outputStream = serialPort.getOutputStream();
                inputStream = serialPort.getInputStream();

                outputStream.flush();
                if (inputStream.markSupported()) {
                    inputStream.reset();
                }

                // use event to let readByte wait until data is available, optimize cpu usage
                serialPort.addEventListener(new SerialPortEventListener() {
                    @Override
                    public void serialEvent(SerialPortEvent event) {
                        if (event.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
                            synchronized (inputStream) {
                                inputStream.notifyAll();
                            }
                        }
                    }
                });

                serialPort.notifyOnDataAvailable(true);

                this.serialPort = serialPort;
                return true;
            }

        } catch (PortInUseException e) {
            logger.error("Serial port {} is already in use", port);

        } catch (UnsupportedCommOperationException e) {
            logger.error("Unsupported Comm Operation", e);

        } catch (TooManyListenersException e) {
            logger.error("Too many listeners error!", e);
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.connection.AbstractEBusConnection#close()
     */
    @Override
    public boolean close() throws IOException {
        if (serialPort == null) {
            return true;
        }

        // run the serial.close in a new not-interrupted thread to
        // prevent an IllegalMonitorStateException error
        Thread shutdownThread = new Thread(new Runnable() {

            @Override
            public void run() {

                CommonsUtils.closeQuietly(inputStream);

                if (outputStream != null) {
                    try {
                        outputStream.flush();
                    } catch (IOException e) {
                    }
                    CommonsUtils.closeQuietly(outputStream);
                }

                SerialPort serialPort = EBusSerialBuildInSerialConnection.this.serialPort;

                if (serialPort != null) {

                    serialPort.notifyOnDataAvailable(false);
                    serialPort.removeEventListener();

                    serialPort.close();
                    EBusSerialBuildInSerialConnection.this.serialPort = null;
                }

                inputStream = null;
                outputStream = null;
            }
        }, "eBUS serial shutdown thread");

        shutdownThread.start();

        try {
            // wait for shutdown
            shutdownThread.join(2000);
        } catch (InterruptedException e) {
        }

        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.csdev.ebus.core.connection.AbstractEBusConnection#readByte(boolean)
     */
    @Override
    public int readByte(boolean lowLatency) throws IOException {
        if (lowLatency) {
            return inputStream.read();
        } else {
            if (inputStream.available() > 0) {
                return inputStream.read();

            } else {
                synchronized (inputStream) {
                    try {
                        inputStream.wait(3000);
                        return inputStream.read();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return -1;
                }
            }
        }
    }
}
