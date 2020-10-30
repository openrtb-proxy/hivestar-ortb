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

package starproxy.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import starproxy.enums.BroadsignPartner;
import starproxy.model.bidRequest.BidRequest;
import starproxy.service.BidService;
import starproxy.service.BiddingPartners.HivestackService;
import starproxy.service.BiddingPartners.VistarService;
import starproxy.service.CreativeService;
import starproxy.util.CacheData;
import starproxy.util.ReachSspCodes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;

@RestController
@CrossOrigin
@Slf4j
public class BidController {

    @Autowired
    BidService bidService;

    @Autowired
    HivestackService hivestackService;

    @Autowired
    VistarService vistarService;

    @Autowired
    CacheData cacheData;

    @Autowired
    CreativeService creativeService;

    @Value("${vistar.enabled}")
    boolean vistarEnabled;

    @Value("${hivestack.enabled}")
    boolean hivestackEnabled;

    @Value("${vistar.french.enabled}")
    boolean vistarFrenchEnabled;

    @GetMapping(value = "/cachedDocuments/{uuid}/{impressionId}")
    public ResponseEntity<String> vastCache(@PathVariable("uuid") String uuid, @PathVariable("impressionId") String impressionId) {
        return new ResponseEntity<>(cacheData.getVastDocument(uuid + impressionId).getVastDocument(), HttpStatus.OK);
    }

    @Scheduled(fixedRate = 3600 * 1000, initialDelay = 10 * 1000)
    public void loadUpcomingHivestackCreatives() {
        if (hivestackEnabled) {
            try {
                hivestackService.fetchUpcomingHivestackCreatives();
            } catch (Exception e) {
                log.error("unexpected exception fetching hivestack creatives: " + e.getMessage(), e);
            }
        }
    }

    @Scheduled(fixedRate = 3600 * 1000, initialDelay = 10 * 1000)
    public void loadUpcomingVistarCreatives() {
        if (vistarEnabled) {
            vistarService.fetchUpcomingVistarCreative();
        }
    }

    @GetMapping(value = "/win")
    public void auctionWin(@RequestParam(required = true) String auction,
                           @RequestParam(required = true) String bid,
                           @RequestParam(required = true) String adid,
                           @RequestParam(required = true) String partner,
                           @RequestParam(required = true) String device
    ) {
        log.info(partner + " auction win event - auction: " + auction + " bid: " + bid + " adid: " + adid + " device: " + device);
    }

    @GetMapping(value = "/loss")
    public void auctionLoss(@RequestParam(required = true) String auction,
                            @RequestParam(required = true) String bid,
                            @RequestParam(required = true) String loss,
                            @RequestParam(required = true) String adid,
                            @RequestParam(required = true) String partner,
                            @RequestParam(required = false) String lossurl,
                            @RequestParam(required = true) String device
    ) {
        if (lossurl != null) {
            vistarService.callNotifyUrl(lossurl);
        }
        log.info(partner + " auction loss event - auction: " + auction + " bid: " + bid + " adid: " + adid + " device: " + device + " loss: " + ReachSspCodes.getError(loss));
    }

    @PostMapping(value = "/bids/{partner}", consumes = {"application/json"}, produces = {"application/json"})
    @ResponseBody
    public Mono<String> bidsForPartners(@RequestBody BidRequest bidRequest, @PathVariable("partner") String partner) throws JsonProcessingException, UnsupportedEncodingException {
        switch (partner) {
            case "vistar":
                if (vistarEnabled) {
                    return bidService.biddingRequest(bidRequest, BroadsignPartner.BROADSIGN_VISTAR);
                } else {
                    log.error("received bidrequest for vistar but vistar is disabled");
                    return Mono.empty();
                }
            case "vistar_french":
                if (vistarFrenchEnabled) {
                    return bidService.biddingRequest(bidRequest, BroadsignPartner.BROADSIGN_VISTAR_FR);
                } else {
                    log.error("received bidrequest for vistar_french but vistar_french is disabled");
                    return Mono.empty();
                }
            case "hivestack":
                if (hivestackEnabled) {
                    return bidService.biddingRequest(bidRequest, BroadsignPartner.BROADSIGN_HIVESTACK);
                } else {
                    log.error("received bidrequest for hivestack but hivestack is disabled");
                    return Mono.empty();
                }
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
