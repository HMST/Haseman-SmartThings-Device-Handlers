# Haseman-Smarthings-Device-Handlers

## Overview

&nbsp; &nbsp;This repository contains [Haseman](http://www.haseman-electric.com) Z-Wave DIN modules device handlers. They are compatible with Classic App of Samsung's [SmartThings](http://www.smartthings.com) home automation platform.

#### Install the code using the SmartThings IDE

1. Log in to your Smarthings account
2. Go to "My device handler" section.
3. Tab on "New device handler"
4. Select "From Code" tab and paste in the contents of the relevant groovies files. 
5. Click "Create".
6. After import both groovy files you should proceed with iclusion(Add a thing) to Z-wave network (if already done it, just exclude and re-inculde)
7. Wait for configuration of device and should be ready use it.

## Device Handler

#### [Haseman RS-10PM2 Z-Wave, DIN Rail, 10-Channel Relay Module with Power Meter](http://www.haseman-electric.com/downloads/Haseman_RS-10PM2_Manual.pdf):
 - Support control of 10x Relay Switches
 - Support live reporting of Energy[KW/H], Power[W], Current[A], Voltage[V], Frequency[HZ] and PF[cosf]
 - Support all availbale device parameters synchronisation
 
 <img src="http://www.haseman-electric.com/downloads/RS-10PM2.png" width="200"> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
 <img src="https://raw.githubusercontent.com/codersaur/SmartThings/master/devices/fibaro-dimmer-2/screenshots/fd2-ss-tiles-on.png" width="200"> &nbsp; &nbsp; &nbsp; &nbsp; &nbsp;
 <img src="https://raw.githubusercontent.com/codersaur/SmartThings/master/devices/fibaro-dimmer-2/screenshots/fd2-ss-tiles-on.png" width="200">
