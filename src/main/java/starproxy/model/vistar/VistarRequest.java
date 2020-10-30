/*
 * Copyright 2020 Pattison Outdoor Advertising LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package starproxy.model.vistar;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "device_id",
        "direct_connection",
        "device_attribute",
        "display_area",
        "display_time",
        "venue_id",
        "network_id",
        "longitude",
        "latitude",
        "api_key",
        "min_duration",
        "max_duration"
})
@Data
public class VistarRequest {

    @JsonProperty("device_id")
    private String deviceId;
    @JsonProperty("direct_connection")
    private Boolean directConnection;
    @JsonProperty("device_attribute")
    private List<Object> deviceAttribute = null;
    @JsonProperty("display_area")
    private List<DisplayArea> displayArea = null;
    @JsonProperty("display_time")
    private Integer displayTime;
    @JsonProperty("venue_id")
    private String venueId;
    @JsonProperty("network_id")
    private String networkId;
    @JsonProperty("longitude")
    private Double longitude;
    @JsonProperty("latitude")
    private Double latitude;
    @JsonProperty("api_key")
    private String apiKey;
    @JsonProperty("min_duration")
    private Integer minDuration;
    @JsonProperty("max_duration")
    private Integer maxDuration;


}
