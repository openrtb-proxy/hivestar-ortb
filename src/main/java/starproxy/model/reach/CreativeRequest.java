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

package starproxy.model.reach;

import java.util.List;

import com.fasterxml.jackson.annotation.*;
import starproxy.enums.CreativeType;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "advertiser",
        "publishers",
        "original_url",
        "iab_categories",
        "type",
        "name",
        "external_id"
})
@Data
public class CreativeRequest {

    @JsonProperty("advertiser")
    private Advertiser advertiser;
    @JsonProperty("publishers")
    private List<Publisher> publishers = null;
    @JsonProperty("original_url")
    private String originalUrl;
    @JsonProperty("iab_categories")
    private List<IabCategory> iabCategories = null;
    @JsonProperty("type")
    private CreativeType type;
    @JsonProperty("name")
    private String name;
    @JsonProperty("external_id")
    private String externalId;
    @JsonIgnore
    private String thumborUrl;
}
