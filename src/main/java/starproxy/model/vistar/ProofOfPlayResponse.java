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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "impressions",
        "media_cost",
        "spots",
        "expires",
        "errors"
})
@Data
public class ProofOfPlayResponse {

    @JsonProperty("impressions")
    public Double impressions;
    @JsonProperty("media_cost")
    public Integer mediaCost;
    @JsonProperty("spots")
    public Integer spots;
    @JsonProperty("expires")
    public Integer expires;
    @JsonProperty("errors")
    public Integer errors;

}
