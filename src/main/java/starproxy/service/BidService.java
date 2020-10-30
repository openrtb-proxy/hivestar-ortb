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

package starproxy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import starproxy.enums.BroadsignPartner;
import starproxy.model.bidRequest.BidRequest;
import starproxy.model.bidResponse.BidResponse;
import starproxy.model.bidResponse.Seatbid;
import starproxy.model.vistar.VistarRequest;
import starproxy.repository.CreativesRepository;
import starproxy.repository.PlaylogsRepository;
import starproxy.service.BiddingPartners.HivestackService;
import starproxy.service.BiddingPartners.VistarService;
import starproxy.util.CacheData;
import starproxy.util.StarproxyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;


@Service
@Slf4j
public class BidService {

    @Autowired
    CacheData cacheData;

    @Autowired
    StarproxyUtils starproxyUtils;

    @Autowired
    PlaylogsRepository playlogsRepository;

    @Autowired
    CreativesRepository creativesRepository;

    @Autowired
    VistarService vistarService;

    @Autowired
    CreativeService creativeService;

    @Autowired
    HivestackService hivestackService;

    public Mono<String> biddingRequest(BidRequest bidRequest, BroadsignPartner partner) throws JsonProcessingException {
        log.debug("BidRequest for " + partner + ": " + starproxyUtils.mapToJson(bidRequest));
        CompletableFuture<String> bidResponseCompletableFuture = new CompletableFuture<>();
        Mono<String> bodyCompletedMono = Mono.fromFuture(bidResponseCompletableFuture);
        BidResponse bidResponse = new BidResponse();
        bidResponse.setId(bidRequest.getId());
        Seatbid seatbid = new Seatbid();
        bidResponse.setSeatbid(Collections.singletonList(seatbid));
        if (bidRequest.getImp() != null) {
            if (bidRequest.getImp().get(0).getPmp() != null) {
                if (bidRequest.getImp().get(0).getPmp().getDeals() != null) {
                    bidResponse.setCur(bidRequest.getImp().get(0).getPmp().getDeals().get(0).getBidFloorCur());
                    if (bidRequest.getImp().get(0).getPmp().getDeals().get(0).getWhitelistOfBidderSeats() != null) {
                        seatbid.setSeat(bidRequest.getImp().get(0).getPmp().getDeals().get(0).getWhitelistOfBidderSeats().get(0));
                    }
                }
            }

            hivestack:
            if (partner.equals(BroadsignPartner.BROADSIGN_HIVESTACK)) {
                if (!starproxyUtils.getReachHivestackMappingsMap().containsKey(bidRequest.getDevice().getUniqueScreenId())) {
                    log.error("No Hivestack mappings found for the Reach ifa: " + bidRequest.getDevice().getUniqueScreenId());
                    bidResponseCompletableFuture.complete("error");
                    break hivestack;
                }
                hivestackService.fetchHivestackAd(bidRequest.getImp().get(0), bidRequest.getId(), bidRequest.getDevice().getUniqueScreenId())
                        .doOnNext(bid -> {
                            if (bid.getId() == null) {
                                bidResponseCompletableFuture.complete("error");
                            } else {
                                seatbid.setBid(Collections.singletonList(bid));
                                try {
                                    bidResponseCompletableFuture.complete(starproxyUtils.mapToJson(bidResponse));
                                } catch (JsonProcessingException e) {
                                    log.error("While parsing bid response following error occured : " + e.getMessage());
                                    bidResponseCompletableFuture.complete("error");
                                }
                            }
                        }).subscribe();
            }

            vistar:
            if (partner.equals(BroadsignPartner.BROADSIGN_VISTAR)) {
                if (!starproxyUtils.getReachVistarMappingsMap().containsKey(bidRequest.getDevice().getUniqueScreenId())) {
                    log.error("No Vistar mappings found for the Reach ifa: " + bidRequest.getDevice().getUniqueScreenId());
                    bidResponseCompletableFuture.complete("error");
                    break vistar;
                }
                String ifa = starproxyUtils.getReachVistarMappingsMap().get(bidRequest.getDevice().getUniqueScreenId());
                // vistar request
                VistarRequest vistarRequest = vistarService.fetchVistarRequestObject(false);
                if (bidRequest.getImp().get(0).getBanner() != null) {
                    vistarRequest.getDisplayArea().get(0).setSupportedMedia(bidRequest.getImp().get(0).getBanner().getMimeTypes());
                }
                if (bidRequest.getImp().get(0).getVideo() != null) {
                    vistarRequest.getDisplayArea().get(0).setSupportedMedia(bidRequest.getImp().get(0).getVideo().getMimeTypes());
                }
                vistarRequest.setLatitude(bidRequest.getDevice().getGeo().getLat());
                vistarRequest.setLongitude(bidRequest.getDevice().getGeo().getLon());
                vistarRequest.setVenueId(ifa);

                vistarService.fetchVistarAd(bidRequest.getImp().get(0), bidRequest.getId(), vistarRequest, bidRequest.getDevice().getUniqueScreenId(), partner)
                        .doOnNext(bid -> {
                            if (bid.getId() == null) {
                                bidResponseCompletableFuture.complete("error");
                            } else {
                                seatbid.setBid(Collections.singletonList(bid));
                                try {
                                    bidResponseCompletableFuture.complete(starproxyUtils.mapToJson(bidResponse));
                                } catch (JsonProcessingException e) {
                                    log.error("While parsing bid response following error occured : " + e.getMessage());
                                    bidResponseCompletableFuture.complete("error");
                                }
                            }
                        }).subscribe();
            }
            vistar_french:
            if (partner.equals(BroadsignPartner.BROADSIGN_VISTAR_FR)) {
                if (!starproxyUtils.getReachVistarFrenchMappingsMap().containsKey(bidRequest.getDevice().getUniqueScreenId())) {
                    log.error("No Vistar French mappings found for the Reach ifa: " + bidRequest.getDevice().getUniqueScreenId());
                    bidResponseCompletableFuture.complete("error");
                    break vistar_french;
                }
                String ifa = starproxyUtils.getReachVistarFrenchMappingsMap().get(bidRequest.getDevice().getUniqueScreenId());
                // vistar request
                VistarRequest vistarRequest = vistarService.fetchVistarRequestObject(true);
                if (bidRequest.getImp().get(0).getBanner() != null) {
                    vistarRequest.getDisplayArea().get(0).setSupportedMedia(bidRequest.getImp().get(0).getBanner().getMimeTypes());
                }
                if (bidRequest.getImp().get(0).getVideo() != null) {
                    vistarRequest.getDisplayArea().get(0).setSupportedMedia(bidRequest.getImp().get(0).getVideo().getMimeTypes());
                }
                vistarRequest.setLatitude(bidRequest.getDevice().getGeo().getLat());
                vistarRequest.setLongitude(bidRequest.getDevice().getGeo().getLon());
                vistarRequest.setVenueId(ifa);

                vistarService.fetchVistarAd(bidRequest.getImp().get(0), bidRequest.getId(), vistarRequest, bidRequest.getDevice().getUniqueScreenId(), partner)
                        .doOnNext(bid -> {
                            if (bid.getId() == null) {
                                bidResponseCompletableFuture.complete("error");
                            } else {
                                seatbid.setBid(Collections.singletonList(bid));
                                try {
                                    bidResponseCompletableFuture.complete(starproxyUtils.mapToJson(bidResponse));
                                } catch (JsonProcessingException e) {
                                    log.error("While parsing bid response following error occured : " + e.getMessage());
                                    bidResponseCompletableFuture.complete("error");
                                }
                            }
                        }).subscribe();
            }
        }

        return bodyCompletedMono.doOnNext(s -> {
            if (s.equals("error")) {
                throw new ResponseStatusException(HttpStatus.NO_CONTENT);
            }
            log.debug("BidResponse for " + partner + ": " + s);
        });
    }

}