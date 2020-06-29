/*
 *  Note: This handler requires the "Haseman RS-10PM2 Switch Child Device" to be installed.
 *
 *  Copyright 2019 Andy Poe
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Haseman RS-10PM2
 *
 *  Author: Andy Poe
 *
 */

metadata {
    definition (name: "Haseman RS-10PM2", namespace: "Z-Wave", author: "Andy Poe", vid:"generic-switch-power-energy") {
        capability "Sensor"
        capability "Actuator"
        capability "Switch"
        capability "Polling"
        capability "Configuration"
        capability "Refresh"
        capability "Zw Multichannel"
        capability "Energy Meter"
        capability "Power Meter"
        capability "Health Check"

        command "reset"

        fingerprint mfr: "0115", prod: "F111", model: "1111", deviceJoinName: "Haseman RS-10PM2"

        fingerprint deviceId: "0x1001", inClusters:"0x5E,0x86,0x72,0x59,0x73,0x22,0x56,0x32,0x71,0x98,0x7A,0x25,0x5A,0x85,0x70,0x8E,0x60,0x75,0x5B"
    }

    simulator {
    }

    tiles(scale: 2){

        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 1, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#00a0dc", nextState:"turningOff"
            }
        }

        valueTile("voltage", "device.voltage", inactiveLabel: false, width: 2, height: 1) {
            state("voltage", label:'Voltage\n ${currentValue} ${unit}', unit:'V')
        }
        valueTile("currentA", "device.currentA", inactiveLabel: false, width: 2, height: 1) {
            state("currentA", label:'Current\n ${currentValue} ${unit}', unit:'A')
        }
        valueTile("frequency", "device.frequency", inactiveLabel: false, width: 2, height: 1) {
            state("frequency", label:'Frequency\n ${currentValue} ${unit}', unit:'Hz')
        }
        valueTile("power", "device.power", inactiveLabel: false, width: 2, height: 1) {
            state "power", label:'Power\n ${currentValue} W'
        }
        valueTile("energy", "device.energy", inactiveLabel: false, width: 2, height: 1) {
            state "energy", label:'Energy\n ${currentValue} kWh'
        }
        valueTile("powerfactor", "device.powerfactor", inactiveLabel: false, width: 2, height: 1) {
            state "powerfactor", label:'Power Factor\n ${currentValue} Cos'
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"REFRESH", action:"refresh.refresh"
        }
        standardTile("configure", "device.needUpdate", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "NO" , label:'', action:"configuration.configure", icon:"st.secondary.configure"
            state "YES", label:'', action:"configuration.configure", icon:"https://github.com/erocm123/SmartThingsPublic/raw/master/devicetypes/erocm123/qubino-flush-1d-relay.src/configure@2x.png"
        }
        standardTile("reset", "device.energy", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:'RESET KWH', action:"reset"
        }

        main(["switch", "voltage", "frequency", "currentA", "power", "powerfactor", "energy"])
        details(["switch", "voltage", "frequency", "currentA",  "power", "powerfactor", "energy", childDeviceTiles("all"),
                 "refresh","reset","configure"])

    }
    preferences {
        input description: "Once you change values on this page, the corner of the \"configuration\" icon will change orange until all configuration parameters are updated.", title: "Settings", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        generate_preferences(configuration_model())
    }
}

def parse(String description) {
    def result = []
    if (description.startsWith("Err 106")) {
        state.sec = 0
        result = createEvent(descriptionText: description, isStateChange: true)
    } else {
        def cmd = zwave.parse(description)
        if (cmd) {
            result += zwaveEvent(cmd)
        } else {
            log.debug "Non-parsed event: ${description}"
        }
    }

    def statusTextmsg = ""

    result.each {
        if ((it instanceof Map) == true && it.find{ it.key == "name" }?.value == "power") {
            statusTextmsg = "${it.value} W ${device.currentValue('energy')? device.currentValue('energy') : "0"} kWh"
        }
        if ((it instanceof Map) == true && it.find{ it.key == "name" }?.value == "energy") {
            statusTextmsg = "${device.currentValue('power')? device.currentValue('power') : "0"} W ${it.value} kWh"
        }
    }
    if (statusTextmsg != "") sendEvent(name:"statusText", value:statusTextmsg, displayed:false)

    return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd, ep=null) {
    logging("BasicSet: $cmd : Endpoint: $ep")
    if (ep) {
        def event
        childDevices.each { childDevice ->
            if (childDevice.deviceNetworkId == "$device.deviceNetworkId-ep$ep") {
                childDevice.sendEvent(name: "switch", value: cmd.value ? "on" : "off")
            }
        }
        if (cmd.value) {
            event = [createEvent([name: "switch", value: "on"])]
        } else {
            def allOff = true
            childDevices.each { n ->
                if (n.currentState("switch").value != "off") allOff = false
            }
            if (allOff) {
                event = [createEvent([name: "switch", value: "off"])]
            } else {
                event = [createEvent([name: "switch", value: "on"])]
            }
        }
        return event
    }
}

def zwaveEvent(physicalgraph.zwave.commands.switchbinaryv1.SwitchBinaryReport cmd, ep=null)
{
    logging("SwitchBinaryReport: $cmd : Endpoint: $ep")
    if (ep) {
        def event
        childDevices.each { childDevice ->
            if (childDevice.deviceNetworkId == "$device.deviceNetworkId-ep$ep") {
                childDevice.sendEvent(name: "switch", value: cmd.value ? "on" : "off")
            }
        }
        if (cmd.value) {
            event = [createEvent([name: "switch", value: "on"])]
        } else {
            def allOff = true
            childDevices.each { n ->
                if (n.currentState("switch").value != "off") allOff = false
            }
            if (allOff) {
                event = [createEvent([name: "switch", value: "off"])]
            } else {
                event = [createEvent([name: "switch", value: "on"])]
            }
        }
        return event
    } 
}

def zwaveEvent(physicalgraph.zwave.commands.meterv3.MeterReport cmd, ep=null) {
    logging("MeterReport: $cmd : Endpoint: $ep")
    def map = null
    if (cmd.meterType == 1) {
        if (cmd.scale == 0) {
            map = [name: "energy", value: cmd.scaledMeterValue,
                   unit: "kWh"]
        } else if (cmd.scale == 6) {
            map = [name: "powerfactor", value: cmd.scaledMeterValue,
                   unit: "Cos"]
        } else if (cmd.scale == 2) {
            map = [name: "power", value: cmd.scaledMeterValue, 
            	   unit: "W"]
    }
  }

    if (map) {
        if (cmd.previousMeterValue && cmd.previousMeterValue != cmd.meterValue) {
            map.descriptionText = "${device.displayName} ${map.name} is ${map.value} ${map.unit}, previous: ${cmd.scaledPreviousMeterValue}"
        }
        createEvent(map)
    } else {
        null
    }
}

def zwaveEvent(physicalgraph.zwave.commands.sensormultilevelv5.SensorMultilevelReport cmd, ep=null) {
    logging("Report: $cmd : Endpoint: $ep")
    def map = [ displayed: true, value: cmd.scaledSensorValue.toString() ]
    switch (cmd.sensorType) {
        case 4:
            map.name = "power"
            map.unit = cmd.scale == 1 ? "Btu/h" : "W"
            break;
        case 0xF:
            map.name = "voltage"
            map.unit = cmd.scale == 1 ? "mV" : "V"
            break;
        case 0x10:
            map.name = "currentA"
            map.unit = cmd.scale == 1 ? "mA" : "A"
            break;
        case 0x20:
            map.name = "frequency"
            map.unit = "Hz"
            break;
    }
    createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCapabilityReport cmd)
{
    //log.debug "multichannelv3.MultiChannelCapabilityReport $cmd"

    if (cmd.endPoint == 10 ) {
        def currstate = device.currentState("switch10").getValue()
        if (currstate == "on")
            sendEvent(name: "switch10", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch10", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 9 ) {
        def currstate = device.currentState("switch9").getValue()
        if (currstate == "on")
            sendEvent(name: "switch9", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch9", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 8 ) {
        def currstate = device.currentState("switch8").getValue()
        if (currstate == "on")
            sendEvent(name: "switch8", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch8", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 7 ) {
        def currstate = device.currentState("switch7").getValue()
        if (currstate == "on")
            sendEvent(name: "switch7", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch7", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 6 ) {
        def currstate = device.currentState("switch6").getValue()
        if (currstate == "on")
            sendEvent(name: "switch6", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch6", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 5 ) {
        def currstate = device.currentState("switch5").getValue()
        if (currstate == "on")
            sendEvent(name: "switch5", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch5", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 4 ) {
        def currstate = device.currentState("switch4").getValue()
        if (currstate == "on")
            sendEvent(name: "switch4", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch4", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 3 ) {
        def currstate = device.currentState("switch3").getValue()
        if (currstate == "on")
            sendEvent(name: "switch3", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch3", value: "on", isStateChange: true, display: false)
    }
    if (cmd.endPoint == 2 ) {
        def currstate = device.currentState("switch2").getValue()
        if (currstate == "on")
            sendEvent(name: "switch2", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch2", value: "on", isStateChange: true, display: false)
    }
    else if (cmd.endPoint == 1 ) {
        def currstate = device.currentState("switch1").getValue()
        if (currstate == "on")
            sendEvent(name: "switch1", value: "off", isStateChange: true, display: false)
        else if (currstate == "off")
            sendEvent(name: "switch1", value: "on", isStateChange: true, display: false)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {

// logging("MultiChannelCmdEncap ${cmd}")
    def encapsulatedCommand = cmd.encapsulatedCommand([0x32: 3, 0x25: 1, 0x20: 1])
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand, cmd.sourceEndPoint as Integer)
    }
}

// check by log
def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    log.debug "AssociationReport $cmd"
    if (zwaveHubNodeId in cmd.nodeId) state."association${cmd.groupingIdentifier}" = true
    else state."association${cmd.groupingIdentifier}" = false
}

// check by log
def zwaveEvent(physicalgraph.zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd) {
    log.debug "MultiChannelAssociationReport $cmd"
    if (cmd.groupingIdentifier == 1) {
        if ([0,zwaveHubNodeId,1] == cmd.nodeId) state."associationMC${cmd.groupingIdentifier}" = true
        else state."associationMC${cmd.groupingIdentifier}" = false
    }
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "Unhandled event $cmd"
// This will capture any commands not handled by other instances of zwaveEvent
// and is recommended for development so you can see every command the device sends
    return createEvent(descriptionText: "${device.displayName}: ${cmd}")
}

def zwaveEvent(physicalgraph.zwave.commands.switchallv1.SwitchAllReport cmd) {
    log.debug "SwitchAllReport $cmd"
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
    update_current_properties(cmd)
    logging("${device.displayName} parameter '${cmd.parameterNumber}' with a byte size of '${cmd.size}' is set to '${cmd2Integer(cmd.configurationValue)}'")
}

def handler() {
    log.debug "handlerMethod called at"
}

def refresh() {
    def cmds = []
    (1..16).each { endpoint ->
        cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), endpoint)
        cmds << encap(zwave.meterV3.meterGet(scale: 0), endpoint)
        cmds << encap(zwave.meterV3.meterGet(scale: 2), endpoint)
        cmds << encap(zwave.meterV3.meterGet(scale: 6), endpoint)
        cmds << encap(zwave.sensorMultilevelV5.sensorMultilevelGet(), endpoint)
    }
    commands(cmds, 100)
}

def reset() {
    logging("reset()")
    def cmds = []
    (14..16).each { endpoint ->
        cmds << encap(zwave.meterV3.meterReset(), endpoint)
        cmds << encap(zwave.meterV3.meterGet(scale: 0), endpoint)
        cmds << encap(zwave.meterV3.meterGet(scale: 2), endpoint)
        cmds << encap(zwave.meterV3.meterGet(scale: 6), endpoint)
    }
    commands(cmds, 500)
}

def ping() {
    def cmds = []
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 1)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 2)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 3)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 4)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 5)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 6)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 7)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 8)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 9)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 10)
    commands(cmds, 1000)
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
    def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
    log.debug "msr: $msr"
    updateDataValue("MSR", msr)
}

def poll() {
    def cmds = []
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 1)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 2)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 3)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 4)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 5)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 6)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 7)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 8)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 9)
    cmds << encap(zwave.switchBinaryV1.switchBinaryGet(), 10)
    commands(cmds, 1000)
}

def configure() {
    state.enableDebugging = settings.enableDebugging
    logging("Configuring Device For SmartThings Use")
    def cmds = []

    cmds = update_needed_settings()

    if (cmds != []) commands(cmds)
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd) {
    logging("CentralSceneNotification: $cmd")
    logging("sceneNumber: $cmd.sceneNumber")
    logging("sequenceNumber: $cmd.sequenceNumber")
    logging("keyAttributes: $cmd.keyAttributes")

    buttonEvent(cmd.keyAttributes + 1, (cmd.sceneNumber == 1? "pushed" : "held"))

}

// check by log
def buttonEvent(button, value) {
    logging("buttonEvent() Button:$button, Value:$value")
    createEvent(name: "button", value: value, data: [buttonNumber: button], descriptionText: "$device.displayName button $button was $value", isStateChange: true)
}

/**
 * Triggered when Done button is pushed on Preference Pane
 */
def updated()
{
    state.enableDebugging = settings.enableDebugging
    logging("updated() is being called")
    if (!childDevices) {
        createChildDevices()
    }
    else if (device.label != state.oldLabel) {
        childDevices.each {
            if (it.label == "${state.oldLabel} (S${channelNumber(it.deviceNetworkId)})") {
                def newLabel = "${device.displayName} (S${channelNumber(it.deviceNetworkId)})"
                it.setLabel(newLabel)
            }
        }
        state.oldLabel = device.label
    }
    sendEvent(name: "checkInterval", value: 2 * 30 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
    def cmds = update_needed_settings()

    sendEvent(name:"needUpdate", value: device.currentValue("needUpdate"), displayed:false, isStateChange: true)

    if (cmds != []) response(commands(cmds))
}

def on() {
    commands([
            encap(zwave.basicV1.basicSet(value: 0xFF), 1),
            encap(zwave.basicV1.basicSet(value: 0xFF), 2),
            encap(zwave.basicV1.basicSet(value: 0xFF), 3),
            encap(zwave.basicV1.basicSet(value: 0xFF), 4),
            encap(zwave.basicV1.basicSet(value: 0xFF), 5),
            encap(zwave.basicV1.basicSet(value: 0xFF), 6),
            encap(zwave.basicV1.basicSet(value: 0xFF), 7),
            encap(zwave.basicV1.basicSet(value: 0xFF), 8),
            encap(zwave.basicV1.basicSet(value: 0xFF), 9),
            encap(zwave.basicV1.basicSet(value: 0xFF), 10)

    ])
}
def off() {
    commands([
            encap(zwave.basicV1.basicSet(value: 0x00), 1),
            encap(zwave.basicV1.basicSet(value: 0x00), 2),
            encap(zwave.basicV1.basicSet(value: 0x00), 3),
            encap(zwave.basicV1.basicSet(value: 0x00), 4),
            encap(zwave.basicV1.basicSet(value: 0x00), 5),
            encap(zwave.basicV1.basicSet(value: 0x00), 6),
            encap(zwave.basicV1.basicSet(value: 0x00), 7),
            encap(zwave.basicV1.basicSet(value: 0x00), 8),
            encap(zwave.basicV1.basicSet(value: 0x00), 9),
            encap(zwave.basicV1.basicSet(value: 0x00), 10)

    ])
}

void childOn(String dni) {
    logging("childOn($dni)")
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.basicV1.basicSet(value: 0xFF), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchBinaryV1.switchBinaryGet(), channelNumber(dni))))
    sendHubCommand(cmds, 3000)
}

void childOff(String dni) {
    logging("childOff($dni)")
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.basicV1.basicSet(value: 0x00), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchBinaryV1.switchBinaryGet(), channelNumber(dni))))
    sendHubCommand(cmds, 3000)
}

void childRefresh(String dni) {
    logging("childRefresh($dni)")
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.switchBinaryV1.switchBinaryGet(), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.meterV2.meterGet(scale: 0), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.meterV2.meterGet(scale: 2), channelNumber(dni))))
    sendHubCommand(cmds, 1000)
}

void childReset(String dni) {
    logging("childReset($dni)")
    def cmds = []
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.meterV2.meterReset(), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.meterV2.meterGet(scale: 0), channelNumber(dni))))
    cmds << new physicalgraph.device.HubAction(command(encap(zwave.meterV2.meterGet(scale: 2), channelNumber(dni))))
    sendHubCommand(cmds, 1000)
}

private encap(cmd, endpoint) {
    if (endpoint) {
        zwave.multiChannelV3.multiChannelCmdEncap(destinationEndPoint:endpoint).encapsulate(cmd)
    } else {
        cmd
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    state.sec = 1
    def encapsulatedCommand = cmd.encapsulatedCommand([0x20: 1, 0x32: 3, 0x25: 1, 0x98: 1, 0x70: 2, 0x85: 2, 0x9B: 1, 0x90: 1, 0x73: 1, 0x30: 1, 0x28: 1, 0x2B: 1]) // can specify command class versions here like in zwave.parse
    if (encapsulatedCommand) {
        return zwaveEvent(encapsulatedCommand)
    } else {
        log.warn "Unable to extract encapsulated cmd from $cmd"
        createEvent(descriptionText: cmd.toString())
    }
}

def generate_preferences(configuration_model)
{
    def configuration = parseXml(configuration_model)

    configuration.Value.each
            {
                switch(it.@type)
                {
                    case ["byte","short","four"]:
                        input "${it.@index}", "number",
                                title:"${it.@label}\n" + "${it.Help}",
                                range: "${it.@min}..${it.@max}",
                                defaultValue: "${it.@value}",
                                displayDuringSetup: "${it.@displayDuringSetup}"
                        break
                    case "list":
                        def items = []
                        it.Item.each { items << ["${it.@value}":"${it.@label}"] }
                        input "${it.@index}", "enum",
                                title:"${it.@label}\n" + "${it.Help}",
                                defaultValue: "${it.@value}",
                                displayDuringSetup: "${it.@displayDuringSetup}",
                                options: items
                        break
                    case "decimal":
                        input "${it.@index}", "decimal",
                                title:"${it.@label}\n" + "${it.Help}",
                                range: "${it.@min}..${it.@max}",
                                defaultValue: "${it.@value}",
                                displayDuringSetup: "${it.@displayDuringSetup}"
                        break
                    case "boolean":
                        input "${it.@index}", "boolean",
                                title: it.@label != "" ? "${it.@label}\n" + "${it.Help}" : "" + "${it.Help}",
                                defaultValue: "${it.@value}",
                                displayDuringSetup: "${it.@displayDuringSetup}"
                        break
                    case "paragraph":
                        input title: "${it.@label}",
                                description: "${it.Help}",
                                type: "paragraph",
                                element: "paragraph"
                        break
                }
            }
}

def update_current_properties(cmd)
{
    def currentProperties = state.currentProperties ?: [:]

    currentProperties."${cmd.parameterNumber}" = cmd.configurationValue

    if (settings."${cmd.parameterNumber}" != null)
    {
        if (convertParam(cmd.parameterNumber, settings."${cmd.parameterNumber}") == cmd2Integer(cmd.configurationValue))
        {
            sendEvent(name:"needUpdate", value:"NO", displayed:false, isStateChange: true)
        }
        else
        {
            sendEvent(name:"needUpdate", value:"YES", displayed:false, isStateChange: true)
        }
    }

    state.currentProperties = currentProperties
}

def update_needed_settings()
{
    def cmds = []
    def currentProperties = state.currentProperties ?: [:]

    def configuration = parseXml(configuration_model())
    def isUpdateNeeded = "NO"

    sendEvent(name:"numberOfButtons", value:"5")

    if(!state.associationMC1) {
        logging("Adding MultiChannel association group 1")
        cmds << zwave.associationV2.associationRemove(groupingIdentifier: 1, nodeId: [])
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 1, nodeId: [0,zwaveHubNodeId,1])
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: 1)
    }
    if(state.association2){
        logging("Removing association group 2")
        cmds << zwave.associationV2.associationRemove(groupingIdentifier:2, nodeId:zwaveHubNodeId)
        cmds << zwave.associationV2.associationGet(groupingIdentifier:2)
    }
    if(state.association4){
        logging("Removing association group 4")
        cmds << zwave.associationV2.associationRemove(groupingIdentifier:4, nodeId:zwaveHubNodeId)
        cmds << zwave.associationV2.associationGet(groupingIdentifier:4)
    }

    configuration.Value.each
            {
                if ("${it.@setting_type}" == "zwave"){
                    if (currentProperties."${it.@index}" == null)
                    {
                        isUpdateNeeded = "YES"
                        logging("Current value of parameter ${it.@index} is unknown")
                        cmds << zwave.configurationV1.configurationGet(parameterNumber: it.@index.toInteger())

                    }
                    if (settings."${it.@index}" != null && cmd2Integer(currentProperties."${it.@index}") != convertParam(it.@index.toInteger(), settings."${it.@index}"))
                    {
                        isUpdateNeeded = "YES"

                        logging("Parameter ${it.@index} will be updated to " + convertParam(it.@index.toInteger(), settings."${it.@index}"))
                        def convertedConfigurationValue = convertParam(it.@index.toInteger(), settings."${it.@index}")
                        cmds << zwave.configurationV1.configurationSet(configurationValue: integer2Cmd(convertedConfigurationValue, it.@byteSize.toInteger()), parameterNumber: it.@index.toInteger(), size: it.@byteSize.toInteger())
                        cmds << zwave.configurationV1.configurationGet(parameterNumber: it.@index.toInteger())
                    }
                }
            }

    sendEvent(name:"needUpdate", value: isUpdateNeeded, displayed:false, isStateChange: true)
    return cmds
}

def convertParam(number, value) {
    def parValue
    switch (number){
        case 28:
            parValue = (value == "true" ? 1 : 0)
            parValue += (settings."fc_2" == "true" ? 2 : 0)
            parValue += (settings."fc_3" == "true" ? 4 : 0)
            parValue += (settings."fc_4" == "true" ? 8 : 0)
            break
        case 29:
            parValue = (value == "true" ? 1 : 0)
            parValue += (settings."sc_2" == "true" ? 2 : 0)
            parValue += (settings."sc_3" == "true" ? 4 : 0)
            parValue += (settings."sc_4" == "true" ? 8 : 0)
            break
        default:
            parValue = value
            break
    }

    return parValue.toInteger()
}

private def logging(message) {
    if (state.enableDebugging == null || state.enableDebugging == "true") log.debug "$message"
}

/**
 * Convert 1 and 2 bytes values to integer
 */
def cmd2Integer(array) {

    switch(array.size()) {
        case 1:
            array[0]
            break
        case 2:
            ((array[0] & 0xFF) << 8) | (array[1] & 0xFF)
            break
        case 3:
            ((array[0] & 0xFF) << 16) | ((array[1] & 0xFF) << 8) | (array[2] & 0xFF)
            break
        case 4:
            ((array[0] & 0xFF) << 24) | ((array[1] & 0xFF) << 16) | ((array[2] & 0xFF) << 8) | (array[3] & 0xFF)
            break
    }
}

def integer2Cmd(value, size) {
    switch(size) {
        case 1:
            [value]
            break
        case 2:
            def short value1   = value & 0xFF
            def short value2 = (value >> 8) & 0xFF
            [value2, value1]
            break
        case 3:
            def short value1   = value & 0xFF
            def short value2 = (value >> 8) & 0xFF
            def short value3 = (value >> 16) & 0xFF
            [value3, value2, value1]
            break
        case 4:
            def short value1 = value & 0xFF
            def short value2 = (value >> 8) & 0xFF
            def short value3 = (value >> 16) & 0xFF
            def short value4 = (value >> 24) & 0xFF
            [value4, value3, value2, value1]
            break
    }
}

private command(physicalgraph.zwave.Command cmd) {
    if (state.sec) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private commands(commands, delay=1500) {
    delayBetween(commands.collect{ command(it) }, delay)
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    def versions = [0x31: 5, 0x30: 1, 0x9C: 1, 0x70: 2, 0x85: 2]
    def version = versions[cmd.commandClass as Integer]
    def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
    def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
    if (encapsulatedCommand) {
        zwaveEvent(encapsulatedCommand)
    }
}

private channelNumber(String dni) {
    dni.split("-ep")[-1] as Integer
}

private void createChildDevices() {
    state.oldLabel = device.label
    try {
        for (i in 1..10) {
            addChildDevice("Haseman RS-10PM2 Child Device", "${device.deviceNetworkId}-ep${i}", null,
                    [completedSetup: true, label: "${device.displayName} (S${i})",
                     isComponent: false, componentName: "ep$i", componentLabel: "Switch $i"])
        }
    } catch (e) {
        runIn(2, "sendAlert")
    }
}

private sendAlert() {
    sendEvent(
            descriptionText: "Child device creation failed. Please make sure that the \"Haseman RS-10PM2 Child Device\" is installed and published.",
            eventType: "ALERT",
            name: "childDeviceCreation",
            value: "failed",
            displayed: true,
    )
}

def configuration_model()
{
    '''
<configuration>
    <Value type="list" byteSize="1" index="11" label="Parameter 11 - Polling time" min="0" max="255" value="30" setting_type="zwave" fw="">
    <Help>
Range: 0~255s
Default: 30
    </Help>
        <Item label="10s" value="10" />
        <Item label="20s" value="20" />
        <Item label="30s" value="30" />
        <Item label="60s" value="60" />
        <Item label="255s" value="255" />
  </Value>
  <Value type="list" byteSize="2" index="64" label="Parameter 64 - Power Up Memory" min="0" max="1" value="0" setting_type="zwave" fw="">
    <Help>
Range: 0-1
Default: 0
    </Help>
        <Item label="INACTIVE" value="0" />
        <Item label="ACTIVE" value="1" />
  </Value>
  <Value type="list" byteSize="2" index="65" label="Parameter 65 - Button Type CH1" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="2" index="66" label="Parameter 66 - Button Type CH2" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="2" index="67" label="Parameter 67 - Button Type CH3" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="2" index="68" label="Parameter 68 - Button Type CH4" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="1" index="69" label="Parameter 69 - Button Type CH5" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="2" index="70" label="Parameter 70 - Button Type CH6" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="2" index="71" label="Parameter 71 - Button Type CH7" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
 <Value type="list" byteSize="2" index="72" label="Parameter 72 - Button Type CH8" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="2" index="73" label="Parameter 73 - Button Type CH9" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  <Value type="list" byteSize="2" index="74" label="Parameter 74 - Button Type CH10" min="1" max="3" value="1" setting_type="zwave" fw="">
    <Help>
Range: 1-3
Default: 1
    </Help>
        <Item label="PUSH BUTTON" value="1" />
        <Item label="TOGGLE SWITCH" value="2" />
        <Item label="FOLLOWER SWITCH" value="3" />
  </Value>
  
</configuration>
'''
}