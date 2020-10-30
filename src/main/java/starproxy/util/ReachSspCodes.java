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

package starproxy.util;

import lombok.Value;

import java.util.HashMap;
import java.util.Map;

public class ReachSspCodes {
    private static Map<String, Code> lossCodes = new HashMap<>();

    static {
        addCode(new Code("3", "Invalid Bid Response",  "Bid object does not contain required field imp.id."));
        addCode(new Code("4", "Invalid Deal ID","Deal ID was not found in Reach SSP."));
        addCode(new Code("5", "Invalid Auction ID",""));
        addCode(new Code("7", "Missing Markup",""));
        addCode(new Code("8", "Missing Creative ID","Bid_response does not contain required ‘adid’ field."));
        addCode(new Code("9", "Missing Bid Price","Bid_response does not contain required ‘price’ field."));
        addCode(new Code("10", "Missing Minimum Creative Approval Data","The creative has not been approved by the publisher."));
        addCode(new Code("101", "Bid was Below Deal Floor.","Deal ID was not found in Reach SSP."));
        addCode(new Code("102", "Lost to Higher Bid",""));
        addCode(new Code("104", "Buyer Seat Blocked","Seat was not found in Reach SSP."));
        addCode(new Code("200", "Creative Filtered - General; reason unknown",""));
        addCode(new Code("203", "Creative Filtered - Size Not Allowed","The creative resolution does not match with the resolution of the screen in the bid request"));
        addCode(new Code("204", "Creative Filtered - Incorrect Creative Format","The creative submitted in the bid response is image, but the POP endpoint ext.vast_url is provided, or, vice-versa; mismatch between creative submitted and bid_response format."));
        addCode(new Code("212", "Creative Filtered - Animation Too Long","The creative submitted is longer than the permissible ad slot duration as defined in the bid request."));
        addCode(new Code("1009", "Absent Response ID","Bid_response does not contain required field ‘id’."));
        addCode(new Code("1012", "Bad ADID Type","Field bid.adid has incorrect type (string expected)."));
        addCode(new Code("1014", "Bad Bid EXT Type","Field bid.ext has incorrect type (object expected)."));
        addCode(new Code("1015", "Bad Bid ID Type","Field bid.id has incorrect type (string expected)."));
        addCode(new Code("1017", "Bad Bid Price Type","Field bid.price has incorrect type (float expected)."));
        addCode(new Code("1018", "Bad BURL Type","Field bid.burl has incorrect format (string expected)."));
        addCode(new Code("1022", "Bad Deal ID Type","Field bid.deal.id has incorrect type (string expected)."));
        addCode(new Code("1023", "Bad imp ID Type","Field bid.imp.id has incorrect type (string expected)."));
        addCode(new Code("1026", "Bad iurl Format","Field bid.iurl has incorrect format (URL expected)."));
        addCode(new Code("1027", "Bad iurl Type","Field bid.iurl has incorrect type (string expected)."));
        addCode(new Code("1030", "Bad nurl Format","Field bid.nurl has incorrect format (URL expected)."));
        addCode(new Code("1031", "Bad nurl Type","Field bid.nurl has incorrect type (string expected)."));
        addCode(new Code("1037", "Bad VAST_URL Format","Field bid.ext.vast_url has incorrect format (URL expected)."));
        addCode(new Code("1038", "Bad VAST_URL Type","Field bid.ext.vast_url has incorrect type (string expected)."));
        addCode(new Code("1039", "Empty ADID","ADID field in bid_response is not populated with any value."));
        addCode(new Code("1055", "No Creative Found","Generic creative failure."));
        addCode(new Code("1056", "No Deal Received","Bid_response does not contain any deal field in case of open or non-private auction."));
        addCode(new Code("1061", "Private Deal Expected","Bid_response does not contain deal field in case of private auction."));
        addCode(new Code("1062", "Unexpected ADID","Unknown Creative ID in the bid_response."));
        addCode(new Code("1066", "Unexpected imp ID","The bid.imp.id does not match with the imp object of the related bid_request."));
        addCode(new Code("1068", "Unexpected Response ID","The ID of the bid_response does not match the ID of the related bid_request"));
        addCode(new Code("1070", "Not Accepted Currency","DSP does not participate in auction because of invalid currency (cur in bid response does not correspond to cur in bid request array or with imp.id)"));
        addCode(new Code("1090", "Fixed Adspottype: Bid Response Mismatch","If adspottype is Fixed, but crduration_ms / adjimpressions do not match full adslot duration and full audience."));
        addCode(new Code("1091", "Variable Adspottype: Under Minimum Duration","If adspottype is Variable, but crduration_ms is below the minimum duration defined in the bid request."));
        addCode(new Code("1092", "Adspottype: Missing crduration_ms","If adspottype is Variable, adjimpressions field is present but crduration_ms is missing in bid response."));
        addCode(new Code("1093", "Adspottype: Missing adjimpressions","If adspottype is Variable, crduration_ms field is present but adjimpressions is missing in bid response."));
        addCode(new Code("1094", "Adspottype: Creative Duration Failure","If the crduration_ms does not match with the actual creative duration (declared in adid) within the accepted tolerance limit (50 ms)."));
        addCode(new Code("1095", "Adspottype: Incorrectly Declared Impression Purchase","If the adjimpressions value does not match the proportion of the crduration_ms to the maxduration of the ad spot."));
        addCode(new Code("1096", "Bad crduation_ms Format and Type","If crduration_ms is not an integer."));
        addCode(new Code("1097", "Bad adjimpressions Format and Type","If adjimpressions is not a float."));
        addCode(new Code("1098", "Adjimpressions Protocol Failure","Adjimpressions field is not allowed by provider protocol. This error primarily fires if event multiplication for impressions is enabled on the DSP’s technical provider."));
        addCode(new Code("1201", "Frequency Capped","This code is triggered when a frequency cap set by the publisher is triggered."));
        addCode(new Code("1202", "Tagged Excluded on Screen","Publisher restricted the creative related to the adid in the bid_response from winning."));
    }

    private static void addCode(Code code) {
        lossCodes.put(code.getCode(), code);
    }
    
    public static Code getError(String code) {
        if (lossCodes.containsKey(code)) {
            return lossCodes.get(code);
        } else {
            return new Code(code, "", "");
        }
    }

    @Value
    public static class Code {
        String code;
        String description;
        String explanation;
    }
}


