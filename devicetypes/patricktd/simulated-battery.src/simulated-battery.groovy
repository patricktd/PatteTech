/**
 *  Battery
 *
 *  Copyright 2020 Patrick Teixeira
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
metadata {
 definition (name: "Simulated Battery", namespace: "patricktd", author: "Patrick Teixeira (Patte Tech)", ocfDeviceType: "x.com.st.d.remotecontroller") {
  capability "Battery"
  capability "Relative Humidity Measurement"
  capability "Temperature Measurement"

  command "setBatteryLevel", ["number"]
 }

}


def setBatteryLevel(newLevel) {
 log.debug "Executing 'setBatteryLevel'"
 sendEvent(name: "battery", value: newLevel, isStateChange: true)
 sendEvent(name: "humidity", value: newLevel, isStateChange: true)
}