# eBUS Binding

The eBUS binding allows you to control your heating system. The eBUS protocol is
used by heating system vendors like Wolf, Vaillant, Kromschröder etc. You can
read temperatures, pump performance, gas consumption etc.

     ┌────────┐                       serial/usb (rs232) ┌──────┐
     │        │  serial (eBUS)  ┌───┐ or ethernet        │ ──── │
     │        │<--------------->│ X │<------------------>│ :    │
     │  ◊◊◊◊  │                 └───┘                    └──────┘
     └────────┘
    Heating Unit             eBUS Adapter              openHAB Server

## Installation

To install this binding copy the `org.openhab.binding.ebus-3.x.x.kar` file into
the  `openhab\addon` directory. The kar file is an archive that contains all
required dependencies. Restart your openHAB server afterwards.

## Supported Bridges

For this binding you need a heating system that uses the eBUS protocol and an
eBUS interface to access the bus.

### Heating system vendors

Your heating system must support the eBUS protocol. Connect your eBUS interface 
to your heating system. A heating system normally consists of several components, 
like a burner, mixers and solar modules. Please check your manual.

- Vaillant
- Wolf

### eBUS Interfaces

The binding connects openHAB to an eBUS interface via serial interface,
Ethernet or a running `ebusd` daemon. You can buy a ready interface or solder your own circuit
(examples: [eBUS Wiki *german*](http://ebus-wiki.org/doku.php/ebus/ebuskonverter)).

You should check for compatible devices on the [ebusd wiki](https://github.com/john30/ebusd/wiki/6.-Hardware) page.

**In general, all LAN and Wifi devices add additional latency. In most cases this causes problems when writing to the bus.**

#### List of working devices

- eBUS Adapters (1.6, 2.0, 2.1, 2.2) from FHEM forum
- eBUS Adapter from Benedikt P. from https://www.mikrocontroller.net/
- ESERA-Automation / E-Service GmbH: eBus Koppler USB (commercial)

Keep in mind, that most interfaces need to be adjusted before first use.

#### List of NOT or PARTIAL working devices

- ESERA-Automation / E-Service GmbH: eBUS Koppler Ethernet (commercial)
- Serial to Ethernet bridges and software like `ser2net` or `socat`


## Discovery

This binding is able to resolve many devices automatically. It's listening for 
new devices on the bus and try to identify the device. If the device ID is 
known, a corresponding thing is added to the inbox. In any case, an `eBUS 
Standard` thing is added for each detected device.


## Binding Configuration

You can add up to three custom configuration files or one bundle file. This 
allows you to add new eBUS devices without updating this binding. The built-in 
configurations are always loaded first, the own files are loaded afterwards. 
These can also overwrite already loaded commands. You must use the URL syntax. 
For more information see [URL](https://en.wikipedia.org/wiki/URL) on wikipedia.

There are several settings for a binding:

- **Configuration URL** _(configurationUrl)_  
Define a URL to load external configuration files

- **Configuration URL** _(configurationUrl2)_  
Define a second URL to load external configuration files

- **Configuration URL** _(configurationUrl3)_  
Define a third URL to load external configuration files

- **Configuration Bundle URL** _(configurationBundleUrl)_  
Define a bundle URL to load a set of configurations at once.


### Example URLs

    http://www.mydomain.com/files/custom-configuration.json
    file:///etc/openhab/custom-configuration.json


## Bridge Configuration

The bridge is the central communication gateway to your heating system. It is 
mandatory to set the network or serial port parameters.

There are several settings for a bridge:

- **Network Address** _(ipAddress)_  
Network address or hostname of the eBUS interface

- **Network Port** _(port)_  
Port of the eBUS interface

- **Network Driver** _(networkDriver)_  
You can use `raw` (default) for a TCP connection or `ebusd` to use the daemon for low-level access. See `Use ebusd` section.

- **Serial Port** _(serialPort)_  
Serial port

- **Serial Port Driver** _(serialPortDriver)_  
Serial port `nrjavaserial` (default) for the openhab build-in nrjavaserial (RXTX) driver or `jserialcomm` for the jSerialComm driver. Some users have reported a lower latency for jSerialComm.

- **Master Address** _(masterAddress)_  
Master address of this bridge as HEX, default is `FF`

- **Advanced Logging** _(advancedLogging)_  
Enable more logging for this bridge, default is `false`

### Use `ebusd`

You can use the `ebusd` daemon to handle the eBUS low-level layers. These are handling collisions, resend telegrams etc. In that case the eBUS binding only work on the higher levels. You can benefit from the rock solid and fast `ebusd` daemon that is written in C++ for Linux.

*The ebusd driver is new and can cause some issues*

*Warning: Currently there is an issue to send a Master-Master telegram with ebusd 3.4 and eBUS binding 3.0.0 with my setup (Wolf system). Same work withs a `raw` `socat` connection.*

#### Requirements

You need the `ebusd` version **3.4** or newer to connect via TCP to the daemon.

#### Run `ebusd`

    ./ebusd -d /dev/ttyUSB1 --enablehex --scanconfig= -f -p 8888

*We need no configuration files from ebusd, we still use the eBUS binding configurations!
This is just an example, feel free to modify the parameters and run it as service etc.*

## Thing Configuration

There are several settings for a thing:

- **Bridge** _(bridge)_ (required)  
Select an eBUS bridge

- **Slave Address** _(slaveAddress)_ (required)  
Slave address of this node as HEX value like `FF` or `00`


**Advanced settings:**

- **eBUS Master Address** _(masterAddress)_  
Master address of this node as HEX value like `FF`. In general, this value 
must not be set, since this value is calculated on the basis of the slave address.

- **Accept for master address** _(filterAcceptMaster)_  
Accept telegrams for master address, default is `false`

- **Accept for slave address** _(filterAcceptSlave)_  
Accept telegrams for slave address, default is `true`

- **Accept broadcasts** _(filterAcceptBroadcasts)_  
Accept broadcasts telegrams from master address, default is `true`

- **Polling all channels** _(polling)_  
Poll all getter channels every n seconds from an eBUS slave. The binding starts
every eBUS command with a random delay to scatter the bus access.


## Channel Configuration

Polling can be set for all getter channels. The polling applies to all channels in 
a group. Thus, the value must only be set for one channel.

There are only one settings for a channel:

- **Polling** _(polling)_ 
Poll a getter channel every n seconds from a eBUS slave. All channels of a 
channel group will be refreshed by one polling. Polling is not available on 
broadcast and Master-Master commands.


## Channels

Due to the long and dynamic channels, there is no list here.


## Full Example

It is also possible to set up the configuration by text instead of PaperUI. Due to 
the dynamic channels the configuration is not as comfortable as with PaperUI. The 
problem is finding the right IDs.
You should first setup the configuration via PaperUI. From there you can copy 
the information for things and channels.

You can get the channel type by copying the channel id from PaperUI and replace the hash ``#`` character with and underscore ``_`` character.

**.thing file**

```java
Bridge ebus:bridge:home1 "eBUS Bridge (serial)" @ "Home" [ serialPort="/dev/ttyUSB1", masterAddress="FF", advancedLogging=true ] {
  Thing std 08 "My eBUS Standard at address 08" [ slaveAddress="08" ]
  Thing vrc430 15 "My VRC430 at address 15" [ slaveAddress="15" ] {
    Channels:
      Type vrc430_heating_program-heating-circuit_program : vrc430_heating_program-heating-circuit#program [ polling = 60 ]
  }
}
```

```java
Bridge ebus:bridge:home2 "eBUS Bridge2" [ ipAddress="10.0.0.2", port=80 ] {
    ...
}
```

**.items file**

```java
Number Heating_HC_Program "Heating Program [%s]" (Heating) { channel="ebus:vrc430:home1:15:vrc430_heating_program-heating-circuit#program" }
```

## Actions

This binding includes two rule actions, which allows to send eBUS telegrams from within rules.

**sendCommand**

Send a command with given values to a destination address. See configuration JSON files for the correct keys for the map.

``sendCommand("<collectionId>", "<commandId>", "<Dst Address>", MapObject)``

**sendRawTelegram**

Sends a raw hex string telegram over the eBUS binding. The bridge source address is not relevant.

``sendRawTelegram("<Telegram as HEX String")``

**Example**

```
   val ebusAction = getActions("ebus","ebus:bridge:<uid>")
   val values = newLinkedHashMap(
       'temp_d_lead_water' -> 45.5,
       'temp_d_srv_water' -> 40.0,
       'turnoff_water_heating' -> true,
       'turnoffservice_water_heating' -> true
   ) 

   ebusAction.sendCommand("bai", "boiler.control.setopdata", "00", values)
```

## Console Commands

This binding also brings some useful console commands to get more information from
the configuration.

    smarthome:ebus list                                    lists all eBUS devices
    smarthome:ebus send "<ebus telegram>" [<bridgeUID>]    sends a raw hex telegram to an eBUS bridge or if not set to first bridge
    smarthome:ebus devices [<bridgeUID>]                   lists all devices connect to an eBUS bridge or list only a specific bridge
    smarthome:ebus resolve "<ebus telegram>"               resolves and analyze a telegram
    smarthome:ebus reload                                  reload all defined json configuration files
    smarthome:ebus update                                  update all things to newest json configuration files


## Issues

* If receive an error like
  `java.lang.NoClassDefFoundError: gnu/io/SerialPortEventListener`. You can call
  the command below to install the missing serial library.

```
feature:install openhab-transport-serial
```

* Binding stops working, console shows
  `Send queue is full! The eBUS service will reset the queue to ensure proper operation.`

This is maybe a hardware fault by your USB/Serial Converter. Specially cheap
adapters with FTDI chips are fakes. You can try to reduce the USB speed on 
Linux, see [here](https://github.com/raspberrypi/linux/issues/1187).


## Logging

If you want to see what's going on in the binding, switch the loglevel to DEBUG
in the Karaf console

```
log:set DEBUG org.openhab.binding.ebus
```

If you want to see even more, switch to TRACE to also see the gateway
request/response data

```
log:set TRACE org.openhab.binding.ebus
```

Set the logging back to normal

```
log:set INFO org.openhab.binding.ebus
```

You can also set the logging for the core library. In that case use
``de.csdev.ebus``


## `socat` Example

This example is not recommended for productive use. The additional network latency can cause issues!

    socat /dev/ttyUSBn,raw,b2400,echo=0 tcp-listen:8000,fork


## Create your own eBUS configuration files

You can create or customize your own configuration files. The configuration files are json text files. You find the documentation in the link list. You should inspect the included configuration files before.

* [eBUS Configuration Format](https://github.com/csowada/ebus/blob/master/doc/configuration.md)
* [eBUS Configuration ebusd Mapping](https://github.com/csowada/ebus/blob/master/doc/ebusd-mapping.md)
* [eBUS Core Lib included configurations](https://github.com/csowada/ebus-configuration/tree/master/src/main/resources)