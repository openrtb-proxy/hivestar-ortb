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


import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "bidfloor",
        "id",
        "bidfloorcur",
        "wseat",
        "at"
})
@Data
public class Deal {

    @JsonProperty("bidfloor")
    private float bidFloor;
    @JsonProperty("id")
    private String id;
    @JsonProperty("bidfloorcur")
    private String bidFloorCur;
    @JsonProperty("wseat")
    private List<String> whitelistOfBidderSeats = null;
    @JsonProperty("at")
    private Integer auctionType;

}