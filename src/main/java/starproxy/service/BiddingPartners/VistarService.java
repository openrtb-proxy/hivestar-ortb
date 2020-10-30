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

import com.fasterxml.jackson.core.JsonProcessingException;
import starproxy.enums.BroadsignPartner;
import starproxy.model.bidRequest.Impression;
import starproxy.model.bidResponse.Bid;
import starproxy.model.bidResponse.Ext;
import starproxy.model.cache.VastDocument;
import starproxy.model.starproxy.Playlogs;
import starproxy.model.reach.CreativeRequest;
import starproxy.model.vistar.DisplayArea;
import starproxy.model.vistar.VistarRequest;
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
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class VistarService {

    @Autowired
    PlaylogsRepository playlogsRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    CacheData cacheData;

    @Value("${vistar.staging.baseurl}")
    private String VISTAR_STAGING_BASE_URL;

    @Value("${vistar.production.baseurl}")
    private String VISTAR_PRODUCTION_BASE_URL;

    @Value("${vistar.staging.ad-serving-path}")
    private String VISTAR_AD_SERVING_PATH;

    @Value("${vistar.staging.creative-caching}")
    private String VISTAR_CREATIVE_CACHING_PATH;

    @Value("${vistar.networkid}")
    private String VISTAR_NETWORK_ID;

    @Value("${vistar.apikey}")
    private String VISTAR_APIKEY;

    @Value("${vistar.french.networkid}")
    private String VISTAR_FRENCH_NETWORK_ID;

    @Value("${vistar.french.apikey}")
    private String VISTAR_FRENCH_APIKEY;

    @Value("${vistar.staging.baseurl}")
    private String VISTAR_BASE_URL;

    @Value("${seatbid.bid.price}")
    private Double BID_PRICE;

    @Value("${server.servlet.context-path}")
    private String CONTEXT_PATH;

    @Value("${vastserver.baseurl}")
    private String VASTSERVER_BASE_URL;

    @Value("${vastserver.cached-document-path}")
    private String CACHED_DOCUMENT_PATH;

    @Value("${reach.nurl}")
    private String NURL;

    @Value("${reach.lurl}")
    private String LURL;

    @Autowired
    StarproxyUtils starproxyUtils;

    @Autowired
    CreativeService creativeService;

    @Autowired
    OauthService oauthService;

    @Autowired
    CreativesRepository creativesRepository;

    public VistarRequest fetchVistarRequestObject(Boolean frenchEnabled) {

        VistarRequest vistarRequest = new VistarRequest();
        vistarRequest.setNetworkId(VISTAR_NETWORK_ID);
        vistarRequest.setApiKey(VISTAR_APIKEY);
        if (frenchEnabled) {
            vistarRequest.setNetworkId(VISTAR_FRENCH_NETWORK_ID);
            vistarRequest.setApiKey(VISTAR_FRENCH_APIKEY);
        }
        vistarRequest.setDirectConnection(false);
        vistarRequest.setDeviceId("");
        vistarRequest.setDisplayTime((int) (System.currentTimeMillis() / 1000));
        DisplayArea displayArea = new DisplayArea();
        vistarRequest.setDisplayArea(Collections.singletonList(displayArea));
        displayArea.setId("1");
        displayArea.setAllowAudio(false);
        displayArea.setSupportedMedia(Stream.of("image/jpeg", "video/mp4", "image/png", "video/mpeg")
                .collect(Collectors.toList()));
        return vistarRequest;
    }

    public void vistarCreative(Playlogs playlog) {
        VistarRequest vistarRequest = new VistarRequest();
        BroadsignPartner partner = BroadsignPartner.BROADSIGN_VISTAR;
        vistarRequest = fetchVistarRequestObject(false);
        String creativeCachingendpoint = VISTAR_STAGING_BASE_URL + VISTAR_CREATIVE_CACHING_PATH;
        if (playlog.getVistarLanguage().equals("FR")) {
            partner = BroadsignPartner.BROADSIGN_VISTAR_FR;
            vistarRequest = fetchVistarRequestObject(true);
            creativeCachingendpoint = VISTAR_PRODUCTION_BASE_URL + VISTAR_CREATIVE_CACHING_PATH;
        }

        String panelId = "";
        if (playlog.getGeneratorId() != null && playlog.getGeneratorId().contains(":")) {
            panelId = playlog.getGeneratorId().substring(playlog.getGeneratorId().lastIndexOf(":") + 1).strip();
        }
        if (panelId.isEmpty() || panelId == null) {
            log.debug("PanelId is empty or null for playlog" + playlog.getId());
            return;
        }
        vistarRequest.setVenueId(panelId);
        vistarRequest.getDisplayArea().get(0).setHeight(playlog.getRottAdHeight());
        vistarRequest.getDisplayArea().get(0).setWidth(playlog.getRottAdWidth());
        String finalPanelId = panelId;
        BroadsignPartner finalPartner = partner;
        webClientBuilder.build()
                .post()
                .uri(creativeCachingendpoint)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(BodyInserters.fromValue(vistarRequest))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(), clientResponse -> {
                    clientResponse.bodyToMono(String.class).subscribe(s -> {
                        log.error(" Error occured while requesting Vistar " + playlog.getVistarLanguage() + " Creative Caching endpoint. Venue ID: " + finalPanelId + " . Error: " + s);
                    });
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .doOnSuccess(s -> {
                    JSONObject jsonObject = new JSONObject(s);
                    if (!jsonObject.isEmpty()) {
                        JSONArray assetArray = (JSONArray) jsonObject.get("asset");
                        List<CreativeRequest> upcomingCreativesList = creativeService.jsonArrayToCreativeRequestObject(assetArray, finalPanelId, finalPartner);
                        upcomingCreativesList.stream().forEach(creative -> {
                            try {
                                if (creative.getOriginalUrl() != null || !creative.getOriginalUrl().isEmpty()) {
                                    if (!cacheData.creativeUrlExists(creative.getOriginalUrl())) {
                                        log.debug("[display: " + finalPanelId + "] adding VISTAR " + playlog.getVistarLanguage() + " creative to reach: " + starproxyUtils.mapToJson(creative));
                                        creativeService.createReachCreative(creative, finalPartner, finalPanelId);
                                    } else {
                                        log.info("Vistar " + playlog.getVistarLanguage() + " Creative already created and stored in database. Url: " + creative.getOriginalUrl());
                                    }
                                }
                            } catch (JsonProcessingException e) {
                                log.error("While parsing Vistar " + playlog.getVistarLanguage() + " creative following error occured : " + e.getMessage());
                            }
                        });
                    } else {
                        log.info("No assets available for vistar " + playlog.getVistarLanguage() + " with PanelId: " + finalPanelId);
                    }
                }).subscribe();
    }


    public void fetchUpcomingVistarCreative() {
        log.debug("Entered fetchUpcomingVistarCreative().");

        // adding token to cache
        oauthService.getBearerToken(BroadsignPartner.BROADSIGN_VISTAR);
        oauthService.getBearerToken(BroadsignPartner.BROADSIGN_VISTAR_FR);
        List<Playlogs> playlogsList = playlogsRepository.findAllByReachDeviceIfaNotNull();
        List<Playlogs> filteredPlaylogs = new ArrayList<>();
        if (!playlogsList.equals(null)) {
            playlogsList.stream().forEach(playlog -> {
                if (playlog.getGeneratorId() != null && playlog.getVistarEnabled().equals("Y")) {
                    String panelId = playlog.getGeneratorId().substring(playlog.getGeneratorId().lastIndexOf(":") + 1).strip();
                    if (!panelId.isBlank()) {
                        filteredPlaylogs.add(playlog);
                    }
                }
                //filteredPlaylogs.add(playlog);
            });
        }
        filteredPlaylogs.forEach(this::vistarCreative);
        log.debug("Exiting fetchUpcomingVistarCreative().");
    }

    public void callNotifyUrl(String url) {
        log.info("Notiyfing vistar for bid loss. Expiration url: " + url);
        webClientBuilder.build()
                .get()
                .uri(url)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.is5xxServerError() || httpStatus.is4xxClientError(), clientResponse -> {
                    clientResponse.bodyToMono(String.class).subscribe(s -> {
                        log.error("Error occured while notifying vistar. Error: " + s);
                    });
                    return Mono.empty();
                })
                .onStatus(HttpStatus::is2xxSuccessful, clientResponse -> {
                    log.info("Successfully notified vistar for bid loss.");
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .subscribe();
    }

    public Mono<Bid> fetchVistarAd(Impression impression, String requestId, VistarRequest vistarRequest, String reachId, BroadsignPartner partner) throws JsonProcessingException {
        String vistarLanguage = "EN";
        if (partner.equals(BroadsignPartner.BROADSIGN_VISTAR_FR)) {
            vistarLanguage = "FR";
        }
        log.debug("Fetching Vistar " + vistarLanguage + " Ad for impression: " + impression.getId());
        CompletableFuture<Bid> bidResponseCompletableFuture = new CompletableFuture<>();
        Mono<Bid> bodyCompletedMono = Mono.fromFuture(bidResponseCompletableFuture);

        vistarRequest.setDisplayTime(impression.getExt().getDisplaytime());
        if (impression.getId().equals("2") && impression.getVideo() != null) {
            vistarRequest.setMinDuration(impression.getVideo().getMinDuration());
            vistarRequest.setMaxDuration(impression.getVideo().getMaxDuration());
            vistarRequest.getDisplayArea().get(0).setWidth(impression.getVideo().getWidth());
            vistarRequest.getDisplayArea().get(0).setHeight(impression.getVideo().getHeight());
        }

        if (impression.getId().equals("1") && impression.getBanner() != null) {
            vistarRequest.getDisplayArea().get(0).setWidth(impression.getBanner().getWidth());
            vistarRequest.getDisplayArea().get(0).setHeight(impression.getBanner().getHeight());
        }

        String finalVistarLanguage = vistarLanguage;
        WebClient.builder().build()
                .post()
                .uri(VISTAR_BASE_URL + VISTAR_AD_SERVING_PATH)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(vistarRequest), VistarRequest.class)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.is5xxServerError() || httpStatus.is4xxClientError(), clientResponse -> {
                    clientResponse.bodyToMono(String.class).subscribe(s -> {
                        log.error("Error occurred while fetching Vistar " + finalVistarLanguage + " Ad. Request ID: " + requestId
                                + " . Bid ID: " + impression.getId()
                                + " . Venue ID: " + vistarRequest.getVenueId() + " . Error: " + s);
                        bidResponseCompletableFuture.complete(new Bid());
                    });
                    return Mono.empty();
                })
                .onStatus(HttpStatus::is2xxSuccessful, clientResponse -> {
                    clientResponse.bodyToMono(String.class).subscribe(s -> {
                        UriComponents uriComponents = UriComponentsBuilder
                                .fromUriString(VISTAR_BASE_URL + VISTAR_AD_SERVING_PATH).build();
                        UUID uuid = UUID.randomUUID();
                        Bid bid = new Bid();
                        Ext ext = new Ext();
                        bid.setUuid(uuid.toString());
                        bid.setPrice(BID_PRICE);
                        if (impression.getPmp() != null) {
                            if (impression.getPmp().getDeals() != null) {
                                bid.setDealid(impression.getPmp().getDeals().get(0).getId());
                            }
                        }

                        JSONObject jsonObject = new JSONObject(s);
                        if (!jsonObject.isEmpty()) {
                            try {
                                JSONArray advertisementArray = (JSONArray) jsonObject.get("advertisement");
                                String popUrl = advertisementArray.getJSONObject(0).getString("proof_of_play_url");
                                String assetUrl = advertisementArray.getJSONObject(0).getString("asset_url");
                                String mediaType = advertisementArray.getJSONObject(0).getString("mime_type");
                                String advertisementId = advertisementArray.getJSONObject(0).getString("id");
                                String advertiser = advertisementArray.getJSONObject(0).getString("advertiser");
                                String expirationUrl = advertisementArray.getJSONObject(0).getString("expiration_url");
                                Integer width = advertisementArray.getJSONObject(0).getInt("width");
                                Integer height = advertisementArray.getJSONObject(0).getInt("height");
                                Long lengthInMilliseconds = advertisementArray.getJSONObject(0).getLong("length_in_milliseconds");
                                Duration timeLeft = Duration.ofMillis(lengthInMilliseconds);
                                String hhmmss = String.format("%02d:%02d:%02d",
                                        timeLeft.toHours(), timeLeft.toMinutesPart(), timeLeft.toSecondsPart());
                                String vastTemp = String.format(VAST_DOCUMENT_TEMPLATE, popUrl, hhmmss, width, height, mediaType, assetUrl);
                                bid.setId(advertisementId);
                                bid.setAdid(UUIDType5.fromUrlWithStarProxyNamespace(assetUrl));
                                bid.setNurl(VASTSERVER_BASE_URL + CONTEXT_PATH + URLDecoder.decode(NURL, StandardCharsets.UTF_8.toString()) + "&partner=Vistar_" + finalVistarLanguage + "&device=" + reachId);
                                bid.setLurl(VASTSERVER_BASE_URL + CONTEXT_PATH + URLDecoder.decode(LURL, StandardCharsets.UTF_8.toString())
                                        + "&lossurl=" + URLEncoder.encode(expirationUrl, StandardCharsets.UTF_8) + "&partner=Vistar_" + finalVistarLanguage + "&device=" + reachId);
                                switch (mediaType) {
                                    case "image/jpeg":
                                    case "image/png":
                                        bid.setIurl(popUrl);
                                        bid.setImpid("1");
                                        log.debug("Vistar " + finalVistarLanguage + " Image Impression fetched: " + popUrl);
                                        break;
                                    case "video/mpeg":
                                    case "video/mp4":
                                        bid.setIurl(null); // should be null for video assets
                                        ext.setVastUrl(VASTSERVER_BASE_URL + CONTEXT_PATH + CACHED_DOCUMENT_PATH + vistarRequest.getVenueId() + "/" + impression.getId());
                                        bid.setImpid("2");
                                        bid.setExt(ext);
                                        log.debug("Vistar " + finalVistarLanguage + " Video Impression fetched: " + popUrl);
                                        break;
                                    default:
                                        log.error("Vistar " + finalVistarLanguage + " unknown mime type in bid request (" + mediaType + "), returning 204");
                                        bidResponseCompletableFuture.complete(new Bid());
                                }

                                if (cacheData.creativeUrlExists(assetUrl)) {
                                    log.debug("Adding Vast " + finalVistarLanguage + " document to cache for ReachId: " + vistarRequest.getVenueId() + " and Impression Id: " + impression.getId());
                                    VastDocument vastDocument = new VastDocument();
                                    vastDocument.setId(vistarRequest.getVenueId() + impression.getId());
                                    vastDocument.setVastDocument(vastTemp);
                                    cacheData.updateVastDocument(vastDocument);
                                    bidResponseCompletableFuture.complete(bid);
                                } else {
                                    log.error("No creative url found in database for Vistar " + finalVistarLanguage + ". Request ID: " + requestId);
                                    JSONArray jsonArrayVistar = new JSONArray();
                                    JSONObject jsonObjectVistar = new JSONObject();
                                    jsonObjectVistar.put("mime_type", mediaType);
                                    jsonObjectVistar.put("asset_url", assetUrl);
                                    jsonObjectVistar.put("creative_name", advertiser);
                                    jsonArrayVistar.put(jsonObjectVistar);
                                    List<CreativeRequest> vistarAssetList = creativeService.jsonArrayToCreativeRequestObject(jsonArrayVistar, requestId, BroadsignPartner.BROADSIGN_VISTAR);

                                    vistarAssetList.stream().forEach(vistarAsset -> {
                                        if (vistarAsset.getOriginalUrl() != null) {
                                            creativeService.createReachCreative(vistarAsset, partner, requestId)
                                                    .doOnNext(creativeCreated -> {
                                                        if (creativeCreated) {
                                                            bidResponseCompletableFuture.complete(bid);
                                                        } else {
                                                            bidResponseCompletableFuture.complete(new Bid());
                                                        }
                                                    })
                                                    .subscribe();
                                        } else {
                                            log.error("Creative originalUrl is null for vistar " + finalVistarLanguage + ". ReachID: " + reachId);
                                            bidResponseCompletableFuture.complete(new Bid());
                                        }
                                    });
                                }

                            } catch (UnsupportedEncodingException e) {
                                log.error("Error occurred in Vistar " + finalVistarLanguage + " Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                        + " . Venue ID: " + vistarRequest.getVenueId() + " . Error: " + e.getMessage());
                                bidResponseCompletableFuture.complete(new Bid());
                            } catch (URISyntaxException e) {
                                //log.error("unable to calculate adid guid from asset url [" + assetUrl + "]: " + e.getMessage(), e);
                                log.error("unable to calculate adid guid from asset url for Vistar " + finalVistarLanguage + " Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                        + " . Venue ID: " + vistarRequest.getVenueId() + " . Error: " + e.getMessage());
                                bidResponseCompletableFuture.complete(new Bid());
                            } catch (NullPointerException e) {
                                log.error("Error occurred in Vistar " + finalVistarLanguage + " Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                        + " . Venue ID: " + vistarRequest.getVenueId() + " . Error: " + e.getMessage());
                                bidResponseCompletableFuture.complete(new Bid());
                            }
                        } else {
                            log.info("BidResponse : No Vistar " + finalVistarLanguage + " Ad to be served. Request ID: " + requestId + " . Bid ID: " + impression.getId()
                                    + " . Venue ID: " + vistarRequest.getVenueId());
                            bidResponseCompletableFuture.complete(new Bid());
                        }
                    });
                    return Mono.empty();
                })
                .bodyToMono(String.class)
                .subscribe();

        return bodyCompletedMono;
    }

    public String VAST_DOCUMENT_TEMPLATE = "<?xml version=\"1.0\"?>\n" +
            "<VAST version=\"2.0\">\n" +
            "    <Ad id=\"1\" sequence=\"1\">\n" +
            "        <InLine>\n" +
            "            <Impression>\n" +
            "                <![CDATA[\n" +
            "                    %s\n" +
            "                ]]>\n" +
            "            </Impression>\n" +
            "            <Creatives>\n" +
            "                <Creative>\n" +
            "                    <Linear>\n" +
            "                        <Duration>%s</Duration>\n" +
            "                        <TrackingEvents>\n" +
            "\n" +
            "                        </TrackingEvents>\n" +
            "                        <MediaFiles>\n" +
            "                            <MediaFile width=\"%d\" height=\"%d\" type=\"%s\" delivery=\"progressive\">\n" +
            "                                <![CDATA[\n" +
            "                                    %s\n" +
            "                                ]]>\n" +
            "                            </MediaFile>\n" +
            "\n" +
            "                        </MediaFiles>\n" +
            "                    </Linear>\n" +
            "                </Creative>\n" +
            "            </Creatives>\n" +
            "        </InLine>\n" +
            "    </Ad>\n" +
            "\n" +
            "</VAST>";


}