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

package starproxy.model.bidRequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "ifa",
        "ua",
        "h",
        "ip",
        "w",
        "geo"
})
@Data
public class Device {

    @JsonProperty("ifa")
    private String uniqueScreenId;
    @JsonProperty("ua")
    private String userAgent;
    @JsonProperty("h")
    private Integer height;
    @JsonProperty("ip")
    private String ipAddress;
    @JsonProperty("w")
    private Integer width;
    @JsonProperty("geo")
    private Geo geo;

}
