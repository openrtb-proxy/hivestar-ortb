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

package starproxy.service.BiddingPartners;

import starproxy.enums.BroadsignPartner;
import starproxy.model.bidRequest.Impression;
import starproxy.model.bidResponse.Bid;
import starproxy.model.bidResponse.Ext;
import starproxy.model.cache.VastDocument;
import starproxy.model.starproxy.Playlogs;
import starproxy.model.reach.CreativeRequest;
import starproxy.repository.CreativesRepository;
import starproxy.repository.PlaylogsRepository;
import starproxy.service.CreativeService;
import starproxy.service.OauthService;
import starproxy.util.CacheData;
import starproxy.util.StarproxyUtils;
import starproxy.util.UUIDType5;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import org.xml.sax.InputSource;
import reactor.core.publisher.Mono;

import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class HivestackService {

    @Value("${hivestack.upcoming-creative-url-list}")
    private String UPCOMING_CREATIVE_URL_LIST;

    @Value("${hivestack.baseurl}")
    private String HIVESTACK_BASE_URL;

    @Value("${hivestack.schedule-vast-path}")
    private String SCHEDULE_VAST_PATH;

    @Value("${vastserver.baseurl}")
    private String VASTSERVER_BASE_URL;

    @Value("${vastserver.cached-document-path}")
    private String CACHED_DOCUMENT_PATH;

    @Value("${seatbid.bid.price}")
    private Double BID_PRICE;

    @Value("${server.servlet.context-path}")
    private String CONTEXT_PATH;

    @Value("${hivestack.errors.nothing-scheduled}")
    private String NOTHING_SCHEDULED;

    @Value("${reach.nurl}")
    private String NURL;

    @Value("${reach.lurl}")
    private String LURL;

    @Autowired
    CacheData cacheData;

    @Autowired
    CreativeService creativeService;

    @Autowired
    PlaylogsRepository playlogsRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    CreativesRepository creativesRepository;

    @Autowired
    StarproxyUtils starproxyUtils;

    @Autowired
    OauthService oauthService;

    public void fetchUpcomingHivestackCreatives() {
        log.debug("Entering fetchUpcomingHivestackCreatives() - adding upcoming hivestack creatives to reach");
        oauthService.getBearerToken(BroadsignPartner.BROADSIGN_HIVESTACK);
        List<Playlogs> playlogsList = playlogsRepository.findAllByReachDeviceIfaNotNull();

        if (playlogsList == null) {
            log.error("playlogs list fetch returned null, aborting creatives fetch");
            return;
        }

        playlogsList.stream().forEach(playlogs -> {
            String hivestackDisplayUuid = playlogs.getHivestackDisplayUuid();
            if (hivestackDisplayUuid == null || hivestackDisplayUuid.isEmpty()) {
                log.error("hivestack uuid for Reach playlog " + playlogs.getReachDeviceIfa() + " was null, skipping");
                return;
            }

            webClientBuilder.build()
                    .get()
                    .uri(HIVESTACK_BASE_URL + UPCOMING_CREATIVE_URL_LIST, hivestackDisplayUuid)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(throwable -> {
                        log.error(" Error occured while fetching Upcoming Hivestack Creative. Error: " + throwable.getMessage());
                    })
                    .doOnSuccess(s -> {
                        JSONArray jsonArray = new JSONArray(s);
                                /*JSONArray jsonArray = new JSONArray("[\n" +
                                        "  {\n" +
                                        "    \"url\": \"https://s3.amazonaws.com/dev.assets.vistarmedia.com/creative/P7GgSiUhSdqO6z_PWOMElw/6c/01b/90d1a9c9-d500-4770-b1e8-b7d4ce4bffed.jpg\",\n" +
                                        "\t\"mime_type\": \"image/jpeg\"\n" +
                                        "  },\n" +
                                        "  {\n" +
                                        "    \"url\": \"https://s3.amazonaws.com/dev.assets.vistarmedia.com/creative/P7GgSiUhSdqO6z_PWOMElw/6c/01b/90d1a9c9-d500-4770-b1e8-b7d4ce4bffed.jpg\",\n" +
                                        "\t\"mime_type\": \"image/jpeg\"\n" +
                                        "  }\n" +
                                        "]");*/

                        if (jsonArray.isEmpty()) {
                            log.debug("[display: " + hivestackDisplayUuid + "] no creatives available");
                            return;
                        }

                        List<CreativeRequest> upcomingCreativesList = creativeService.jsonArrayToCreativeRequestObject(jsonArray, hivestackDisplayUuid, BroadsignPartner.BROADSIGN_HIVESTACK);
                        upcomingCreativesList.stream().forEach(upcomingCreative -> {
                            if (upcomingCreative.getOriginalUrl() != null || !upcomingCreative.getOriginalUrl().isEmpty())
                                if (!cacheData.creativeUrlExists(upcomingCreative.getOriginalUrl())) {
                                    creativeService.createReachCreative(upcomingCreative, BroadsignPartner.BROADSIGN_HIVESTACK, hivestackDisplayUuid);
                                } else {
                                    log.info("Creative already created and stored in database. Url: " + upcomingCreative.getOriginalUrl());
                                }
                        });
                    }).subscribe();
        });
        log.debug("Exiting fetchUpcomingHivestackCreatives().");
    }

    public Mono<Bid> fetchHivestackAd(Impression impression, String requestId, String reachId) {
        String ifa = starproxyUtils.getReachHivestackMappingsMap().get(reachId);
        CompletableFuture<Bid> bidResponseCompletableFuture = new CompletableFuture<>();
        Mono<Bid> bodyCompletedMono = Mono.fromFuture(bidResponseCompletableFuture);
        UriComponents uriComponents = UriComponentsBuilder
                .fromUriString(HIVESTACK_BASE_URL + SCHEDULE_VAST_PATH).build().expand(ifa);
        WebClient.builder().build()
                .get()
                .uri(HIVESTACK_BASE_URL + SCHEDULE_VAST_PATH, ifa)
                .accept(MediaType.APPLICATION_XML)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.is5xxServerError() || httpStatus.is4xxClientError(), clientResponse -> {
                    clientResponse.bodyToMono(String.class).subscribe(s -> {
                        log.error("Error occurred while fetching Hivestack Ad. Request ID: " + requestId
                                + " . Bid ID: " + impression.getId()
                                + " . Request URL: " + uriComponents.toString() + " . Error: " + s);
                        bidResponseCompletableFuture.complete(new Bid());
                    });
                    return Mono.empty();
                })
                .onStatus(HttpStatus::is2xxSuccessful, clientResponse -> {
                    clientResponse.bodyToMono(String.class).subscribe(s -> {
                        UUID uuid = UUID.randomUUID();

                        Bid bid = new Bid();
                        Ext ext = new Ext();
                        bid.setUuid(uuid.toString());
                        bid.setId(requestId);
                        bid.setPrice(BID_PRICE);
                        if (impression.getPmp() != null) {
                            if (impression.getPmp().getDeals() != null) {
                                bid.setDealid(impression.getPmp().getDeals().get(0).getId());
                            }
                        }

                        try {
                            bid.setNurl(VASTSERVER_BASE_URL + CONTEXT_PATH + URLDecoder.decode(NURL, StandardCharsets.UTF_8.toString()) + "&partner=Hivestack&device=" + reachId);
                            bid.setLurl(VASTSERVER_BASE_URL + CONTEXT_PATH + URLDecoder.decode(LURL, StandardCharsets.UTF_8.toString()) + "&partner=Hivestack&device=" + reachId);

                            String scheduledOrNot = XPathFactory.newInstance().newXPath().evaluate(
                                    "/VAST[@version=\"2.0\"]", new InputSource(new StringReader(s))).strip();
                            if (!scheduledOrNot.equals("") || !scheduledOrNot.isEmpty()) {

                                String impressionUrl = XPathFactory.newInstance().newXPath().evaluate(
                                        "/VAST[@version=\"2.0\"]/Ad[@id=\"1\"]/InLine/Impression", new InputSource(new StringReader(s))).strip();
                                String mediaType = XPathFactory.newInstance().newXPath().evaluate("/VAST[@version=\"2.0\"]/Ad[@id=\"1\"]/InLine/Creatives/Creative/Linear/MediaFiles/MediaFile/@type", new InputSource(new StringReader(s))).strip();
                                switch (mediaType) {
                                    case "image/jpeg":
                                    case "image/png":
                                        bid.setIurl(impressionUrl);
                                        bid.setImpid("1");
                                        log.debug("Hivestack Image Impression fetched: " + impressionUrl);
                                        break;
                                    case "video/mpeg":
                                    case "video/mp4":
                                        bid.setIurl(null); // should be null for video assets
                                        ext.setVastUrl(VASTSERVER_BASE_URL + CONTEXT_PATH + CACHED_DOCUMENT_PATH + ifa + "/" + impression.getId());
                                        bid.setImpid("2");
                                        bid.setExt(ext);
                                        break;
                                    default:
                                        log.error("unknown mime type in bid request (" + mediaType + "), returning 204");
                                        bidResponseCompletableFuture.complete(bid);
                                }

                                String mediaFileContent = XPathFactory.newInstance().newXPath().evaluate(
                                        "/VAST[@version=\"2.0\"]/Ad[@id=\"1\"]/InLine/Creatives/Creative/Linear/MediaFiles/MediaFile", new InputSource(new StringReader(s))).strip();
                                if (cacheData.creativeUrlExists(mediaFileContent)) {
                                    log.debug("Adding vast document to cache for ReachId: " + ifa + " and Impression Id: " + impression.getId());
                                    VastDocument vastDocument = new VastDocument();
                                    vastDocument.setId(ifa + impression.getId());
                                    vastDocument.setVastDocument(s);
                                    cacheData.updateVastDocument(vastDocument);
                                    log.debug("Starting encoding the URL with osn name space.");
                                    bid.setAdid(UUIDType5.fromUrlWithStarProxyNamespace(mediaFileContent));
                                    log.debug("Successfully finished encoding URL with osn name space.");
                                    bidResponseCompletableFuture.complete(bid);
                                } else {
                                    log.error("No creative url found in database. Request ID: " + requestId);
                                    JSONArray jsonArrayHivestack = new JSONArray();
                                    JSONObject jsonObjectHivestack = new JSONObject();
                                    jsonObjectHivestack.put("mime_type", mediaType);
                                    jsonObjectHivestack.put("url", mediaFileContent);
                                    jsonObjectHivestack.put("advertiser_name", mediaFileContent);
                                    jsonArrayHivestack.put(jsonObjectHivestack);
                                    List<CreativeRequest> upcomingCreativesList = creativeService.jsonArrayToCreativeRequestObject(jsonArrayHivestack, requestId, BroadsignPartner.BROADSIGN_HIVESTACK);

                                    upcomingCreativesList.stream().forEach(upcomingCreative -> {
                                        if (upcomingCreative.getOriginalUrl() != null) {
                                            creativeService.createReachCreative(upcomingCreative, BroadsignPartner.BROADSIGN_HIVESTACK, requestId)
                                                    .doOnNext(creativeCreated -> {
                                                        if (creativeCreated) {
                                                            bidResponseCompletableFuture.complete(bid);
                                                        } else {
                                                            bidResponseCompletableFuture.complete(new Bid());
                                                        }
                                                    })
                                                    .subscribe();
                                        } else {
                                            log.error("Creative originalUrl is null for hivestack. ReachID: " + reachId);
                                            bidResponseCompletableFuture.complete(new Bid());
                                        }
                                    });
                                }

                            } else {
                                log.info("BidResponse : " + NOTHING_SCHEDULED + " Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                        + " . Schedule Vast Request URL: " + uriComponents.toString() + " . Reach ID: " + reachId);
                                bidResponseCompletableFuture.complete(new Bid());
                            }
                        } catch (UnsupportedEncodingException e) {
                            log.error("Error occurred in Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                    + " . Schedule Vast Request URL: " + uriComponents.toString() + " . Error: " + e.getMessage());
                            bidResponseCompletableFuture.complete(new Bid());
                        } catch (XPathExpressionException | URISyntaxException e) {
                            log.error("Error occurred in Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                    + " . Schedule Vast Request URL: " + uriComponents.toString() + " . Error: " + e.getMessage());
                            bidResponseCompletableFuture.complete(new Bid());
                        } catch (Exception e) {
                            log.error("Error occurred in Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                    + " . Schedule Vast Request URL: " + uriComponents.toString() + " . Exception type: " + e.getClass().getName() + " . Error: " + e.getMessage());
                            bidResponseCompletableFuture.complete(new Bid());
                        }
                    });
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .subscribe();

        return bodyCompletedMono;
    }

}
