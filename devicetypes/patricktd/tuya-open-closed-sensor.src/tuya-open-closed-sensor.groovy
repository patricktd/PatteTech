/**
 *  Tuya Open/Closed Sensor
 *
 *  Copyright 2020 SmartThings - patte.com.br
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import physicalgraph.zigbee.clusters.iaszone.ZoneStatus
import physicalgraph.zigbee.zcl.DataType

metadata {
	definition(name: "Tuya Open/Closed Sensor", namespace: "patricktd", author: "Patrick Teixeira (Patte Tech)", runLocally: true, minHubCoreVersion: '000.017.0012', executeCommandsLocally: false, genericHandler: "Zigbee", ocfDeviceType: "x.com.st.d.sensor.contact") {
		capability "Battery"
		capability "Configuration"
		capability "Contact Sensor"
		capability "Refresh"
		capability "Health Check"
		capability "Sensor"

 		fingerprint endpointId: "01", profileId: "0104", deviceId: "0402", inClusters: "0000,0001,0003,0500", outClusters: "0003", manufacturer: "TUYATEC-n4qd0btb", model: "RH3001"
		fingerprint endpointId: "01", profileId: "0104", deviceId: "0402", inClusters: "0000,000A,0001,0500", outClusters: "0019", manufacturer: "TUYATEC-trhrga6p", model: "RH3001"
        fingerprint endpointId: "01", profileId: "0104", deviceId: "0402", inClusters: "0000,000A,0001,0500", outClusters: "0019", manufacturer: "TUYATEC-xnoof3ts", model: "RH3001"
	}


}

private getIAS_ZONE_TYPE_ATTRIBUTE() { 0x0001 }
private getIAS_ZONE_TYPE_CONTACT_SWITCH_ATTRIBUTE_VALUE() { 0x0001 }
private getBATTERY_VOLTAGE_VALUE_ATTRIBUTE() { 0x0020 }
private getPOLL_CONTROL_CLUSTER() { 0x0020 }
private getCHECK_IN_INTERVAL_ATTRIBUTE() { 0x0000 }
private getFAST_POLL_TIMEOUT_ATTRIBUTE() { 0x0003 }
private getSET_LONG_POLL_INTERVAL_CMD() { 0x02 }
private getSET_SHORT_POLL_INTERVAL_CMD() { 0x03 }

def parse(String description) {
	log.debug "description: $description"

	Map map = zigbee.getEvent(description)
	if (!map) {
		if (description?.startsWith('zone status') || description?.startsWith('zone report')) {
			map = parseIasMessage(description)
		} else {
			Map descMap = zigbee.parseDescriptionAsMap(description)
            log.debug zigbee.parseDescriptionAsMap(description)
			if (descMap?.clusterInt == zigbee.POWER_CONFIGURATION_CLUSTER && descMap.commandInt != 0x07 && descMap?.value) {
				map = getBatteryResult(Integer.parseInt(descMap.value, 16))
			} else if (descMap?.clusterInt == zigbee.IAS_ZONE_CLUSTER && descMap.attrInt == zigbee.ATTRIBUTE_IAS_ZONE_STATUS) {
				def zs = new ZoneStatus(zigbee.convertToInt(descMap.value, 16))
				map = getContactResult(zs.isAlarm1Set() ? "open" : "closed")
			} else if (descMap?.clusterInt == zigbee.IAS_ZONE_CLUSTER && descMap.commandInt == 0x07) {
				if (descMap.data[0] == "00") {
					log.debug "IAS ZONE REPORTING CONFIG RESPONSE: $descMap"
					sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
				} else {
					log.warn "IAS ZONE REPORTING CONFIG FAILED- error code: ${descMap.data[0]}"
				}
			}
		}
	}

	log.debug "Parse returned $map"
	def result = map ? createEvent(map) : [:]

	if (description?.startsWith('enroll request')) {
		List cmds = zigbee.enrollResponse()
		log.debug "enroll response: ${cmds}"
		result = cmds?.collect { new physicalgraph.device.HubAction(it) }
	}
	return result
}


private Map parseIasMessage(String description) {
	ZoneStatus zs = zigbee.parseZoneStatus(description)
	return zs.isAlarm1Set() ? getContactResult('open') : getContactResult('closed')
}

private Map getBatteryResult(rawValue) {
	log.debug 'Battery'
	def linkText = getLinkText(device)

	def result = [:]

	def volts = rawValue / 10
	if (!(rawValue == 0 || rawValue == 255)) {
		def minVolts = 2.1
		def maxVolts = 3.0
		def pct = (volts - minVolts) / (maxVolts - minVolts)
		def roundedPct = Math.round(pct * 100)
		if (roundedPct <= 0)
			roundedPct = 1
		result.value = Math.min(100, roundedPct)
		result.descriptionText = "${linkText} battery was ${result.value}%"
		result.name = 'battery'
	}

	return result
}

private Map getContactResult(value) {
	log.debug 'Contact Status'
	def linkText = getLinkText(device)
	def descriptionText = "${linkText} was ${value == 'open' ? 'opened' : 'closed'}"
	return [
		name           : 'contact',
		value          : value,
		descriptionText: descriptionText
	]
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS)
}

def refresh() {
	log.debug "Refreshing Battery"
	def refreshCmds = zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_VOLTAGE_VALUE_ATTRIBUTE) + zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS) + zigbee.enrollResponse()

	return refreshCmds
}