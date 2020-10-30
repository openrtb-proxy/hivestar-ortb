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
        "id",
        "proof_of_play_url",
        "expiration_url",
        "order_id",
        "display_time",
        "lease_expiry",
        "display_area_id",
        "creative_id",
        "asset_id",
        "asset_url",
        "width",
        "height",
        "mime_type",
        "length_in_seconds",
        "length_in_milliseconds",
        "campaign_id",
        "creative_category",
        "advertiser",
        "deal_id"
})
@Data
public class Advertisement {

    @JsonProperty("id")
    private String id;
    @JsonProperty("proof_of_play_url")
    private String proofOfPlayUrl;
    @JsonProperty("expiration_url")
    private String expirationUrl;
    @JsonProperty("order_id")
    private String orderId;
    @JsonProperty("display_time")
    private Integer displayTime;
    @JsonProperty("lease_expiry")
    private Integer leaseExpiry;
    @JsonProperty("display_area_id")
    private String displayAreaId;
    @JsonProperty("creative_id")
    private String creativeId;
    @JsonProperty("asset_id")
    private String assetId;
    @JsonProperty("asset_url")
    private String assetUrl;
    @JsonProperty("width")
    private Integer width;
    @JsonProperty("height")
    private Integer height;
    @JsonProperty("mime_type")
    private String mimeType;
    @JsonProperty("length_in_seconds")
    private Integer lengthInSeconds;
    @JsonProperty("length_in_milliseconds")
    private Integer lengthInMilliseconds;
    @JsonProperty("campaign_id")
    private Integer campaignId;
    @JsonProperty("creative_category")
    private String creativeCategory;
    @JsonProperty("advertiser")
    private String advertiser;
    @JsonProperty("deal_id")
    private String dealId;

}
