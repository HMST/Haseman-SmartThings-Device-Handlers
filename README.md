# Haseman-Smarthings-Device-Handlers

## Overview

&nbsp; &nbsp;This repository contains [Haseman](http://www.haseman-electric.com) Z-Wave DIN modules device handlers. They are compatible with Classic App of Samsung's [SmartThings](http://www.smartthings.com) home automation platform.

#### Step One: Install the code using the SmartThings IDE

1. Within the SmartThings IDE, click '*My SmartApps*', then '*+ New SmartApp*'. 
2. Select the '*From Code*' tab and paste in the contents of the relevant groovy file.
3. Click '*Create*', and then '*Publish*' *(For Me)*.

**Note:** Some SmartApps may support multiple instances, whereas others may only allow one instance.

## Device Handlers

#### [Haseman RS-10PM2 Z-Wave, DIN Rail, 10-Channel Relay Module with Power Meter](http://www.haseman-electric.com/downloads/Haseman_RS-10PM2_Manual.pdf):
 - Support control of 10x Relay Switches
 - Support live reporting of Energy[KW/H], Power[W], Current[A], Voltage[V], Frequency[HZ] and PF[cosf]
 - Support all availbale device parameters synchronisation
 
 <img src="http://www.haseman-electric.com/downloads/RS-10PM2.png" width="200"> <img src="https://raw.githubusercontent.com/codersaur/SmartThings/master/devices/fibaro-dimmer-2/screenshots/fd2-ss-tiles-on.png" width="200">

#### [Evohome Heating Zone - BETA](https://github.com/codersaur/SmartThings/tree/master/devices/evohome):
 - This device handler is required for the Evohome (Connect) SmartApp.

#### [Fibaro Dimmer 2 (FGD-212)](https://github.com/codersaur/SmartThings/tree/master/devices/fibaro-dimmer-2):
 - An advanced device handler for the Fibaro Dimmer 2 (FGD-212) Z-Wave Dimmer, with support for full parameter synchronisation, multi-channel device associations, protection modes, fault reporting, and advanced logging options.
 - The _Nightmode_ function forces the dimmer to switch on at a specific level (e.g. low-level during the night). It can be enabled/disabled manually using the _Nightmode_ tile, or scheduled from the device's settings.  
   <img src="https://raw.githubusercontent.com/codersaur/SmartThings/master/devices/fibaro-dimmer-2/screenshots/fd2-ss-tiles-on.png" width="200">

#### [Fibaro Flood Sensor (FGFS-101)](https://github.com/codersaur/SmartThings/tree/master/devices/fibaro-flood-sensor):
 - An advanced SmartThings device handler for the Fibaro Flood Sensor (FGFS-101) (EU), with support for full parameter synchronisation, multi-channel device associations, and advanced logging options.  
   <img src="https://raw.githubusercontent.com/codersaur/SmartThings/master/devices/fibaro-flood-sensor/screenshots/ffs-ss-tiles-wet.png" width="200">

#### [Fibaro RGBW Controller (FGRGBWM-441)](https://github.com/codersaur/SmartThings/tree/master/devices/fibaro-rgbw-controller):
 - This device handler is written specifically for the Fibaro RGBW Controller (FGRGBWM-441).
 - It extends the native SmartThings device handler to support editing the device's parameters from the SmartThings GUI, and to support the use of one or more of the controller's channels in IN/OUT mode (i.e. analog sensor inputs).  
   <img src="https://raw.githubusercontent.com/codersaur/SmartThings/master/devices/fibaro-rgbw-controller/screenshots/screenshot_rgbw.png" width="200">
