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

import starproxy.enums.BroadsignPartner;
import starproxy.enums.CreativeType;
import starproxy.model.starproxy.Creatives;
import starproxy.model.reach.Advertiser;
import starproxy.model.reach.CreativeRequest;
import starproxy.model.reach.IabCategory;
import starproxy.model.reach.Publisher;
import starproxy.repository.CreativesRepository;
import starproxy.util.CacheData;
import starproxy.util.StarproxyUtils;
import starproxy.util.ThumborUtil;
import starproxy.util.UUIDType5;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class CreativeService {

    @Value("${reach.create-creative-path}")
    private String CREATE_CREATIVE_PATH;

    @Value("${creative.hivestack.name}")
    private String HIVESTACK_CREATIVE_NAME;

    @Value("${creative.hivestack.advertiser.id}")
    private Integer HIVESTACK_ADVERTISER_ID;

    @Value("${creative.vistar.advertiser.id}")
    private Integer VISTAR_ADVERTISER_ID;

    @Value("${creative.vistar_french.advertiser.id}")
    private Integer VISTAR_FRENCH_ADVERTISER_ID;

    @Value("${creative.publisher.id}")
    private Integer PUBLISHER_ID;

    @Value("${creative.publisher.name}")
    private String PUBLISHER_NAME;

    @Value("${creative.iab_category.id}")
    private Integer IAB_CATEGORY_ID;

    @Value("${reach.baseurl}")
    private String REACH_BASE_URL;

    @Value("${thumbor.enabled}")
    private boolean THUMBOR_ENABLED;

    @Autowired
    CacheData cacheData;

    @Autowired
    OauthService oauthService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private ThumborUtil thumborUtil;

    @Autowired
    CreativesRepository creativesRepository;

    @Autowired
    StarproxyUtils starproxyUtils;

    private WebClient webClient;

    CreativeService(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<CreativeRequest> jsonArrayToCreativeRequestObject(JSONArray jsonArray, String id, BroadsignPartner partner) {
        return IntStream.range(0, jsonArray.length())
                .mapToObj(index -> {
                    CreativeRequest creativeRequest = new CreativeRequest();
                    JSONObject jsonObject = (JSONObject) jsonArray.get(index);
                    String mimeType = jsonObject.getString("mime_type");
                    String url = "";
                    if (partner.equals(BroadsignPartner.BROADSIGN_HIVESTACK)) {
                        url = jsonObject.getString("url");
                        try {
                            if (!jsonObject.optString("advertiser_name").isEmpty()) {
                                creativeRequest.setName(jsonObject.getString("advertiser_name") + " - " + id);
                            }
                        } catch (Exception e) {
                            log.warn("No advertiser_name found for [display: " + id + "]");
                        }
                    }
                    if (partner.equals(BroadsignPartner.BROADSIGN_VISTAR) || partner.equals(BroadsignPartner.BROADSIGN_VISTAR_FR)) {
                        url = jsonObject.getString("asset_url");
                        creativeRequest.setName(jsonObject.getString("creative_name"));
                    }

                    creativeRequest.setOriginalUrl(url);
                    switch (mimeType) {
                        case "image/jpeg":
                            creativeRequest.setType(CreativeType.ImageUrlCreative);
                            break;
                        case "image/png":
                            if (THUMBOR_ENABLED) {
                                String thumborUrl = thumborUtil.getJpgUrl(url);
                                log.info("[" + partner + " display: " + id + "] converting original png url " + url + " to new jpg url: " + thumborUrl);
                                creativeRequest.setType(CreativeType.ImageUrlCreative);
                                creativeRequest.setThumborUrl(thumborUrl);
                            } else {
                                creativeRequest.setType(CreativeType.ImageUrlCreative);
                            }
                            break;
                        case "video/mp4":
                        case "video/mpeg":
                            creativeRequest.setType(CreativeType.VideoCreative);
                            break;
                        default:
                            log.error("[" + partner + " display: " + id + "] unknown mimetype (" + mimeType + ") for creative, skipping: " + url);
                            break;
                    }
                    return creativeRequest;
                })
                .collect(Collectors.toList());
    }

    public CreativeRequest createCreativeObject(String partner, CreativeRequest creativeRequest) {

        Publisher publisher = new Publisher();
        publisher.setId(PUBLISHER_ID);
        publisher.setName(PUBLISHER_NAME);

        Advertiser advertiser = new Advertiser();
        if (partner.equals("vistar")) {
            advertiser.setId(VISTAR_ADVERTISER_ID);
            //creativeRequest.setName(VISTAR_CREATIVE_NAME);
        }
        if (partner.equals("vistar_french")) {
            advertiser.setId(VISTAR_FRENCH_ADVERTISER_ID);
            //creativeRequest.setName(VISTAR_FRENCH_CREATIVE_NAME);
        }
        if (partner.equals("hivestack")) {
            advertiser.setId(HIVESTACK_ADVERTISER_ID);
            creativeRequest.setName(HIVESTACK_CREATIVE_NAME);
        }
        creativeRequest.setAdvertiser(advertiser);
        creativeRequest.setPublishers(Collections.singletonList(publisher));

        // IAB24 - Uncategorized
        IabCategory iabCategory = new IabCategory();
        iabCategory.setId(IAB_CATEGORY_ID);
        creativeRequest.setIabCategories(Collections.singletonList(iabCategory));
        String externalId = "";
        try {
            externalId = UUIDType5.fromUrlWithStarProxyNamespace(creativeRequest.getOriginalUrl());
        } catch (URISyntaxException e) {
            log.error("Error encoding the original_url to uuid: " + e.getMessage());
        }
        creativeRequest.setExternalId(externalId);
        //creativeRequest.setExternalId(UUID.randomUUID().toString());
        creativeRequest.setOriginalUrl(creativeRequest.getOriginalUrl());

        return creativeRequest;
    }

    public Mono<ClientResponse> createCreativeWebClient(String hivestackDisplayUuid, CreativeRequest creativeRequest, BroadsignPartner partner) {
        final String creativePostUrl = REACH_BASE_URL + CREATE_CREATIVE_PATH;
        CreativeRequest tempCreativeRequest = new CreativeRequest();
        tempCreativeRequest.setAdvertiser(creativeRequest.getAdvertiser());
        tempCreativeRequest.setPublishers(creativeRequest.getPublishers());
        tempCreativeRequest.setExternalId(creativeRequest.getExternalId());
        tempCreativeRequest.setType(creativeRequest.getType());
        tempCreativeRequest.setName(creativeRequest.getName());
        tempCreativeRequest.setIabCategories(creativeRequest.getIabCategories());
        tempCreativeRequest.setOriginalUrl(creativeRequest.getOriginalUrl());
        if (creativeRequest.getThumborUrl() != null) {
            tempCreativeRequest.setOriginalUrl(creativeRequest.getThumborUrl());
        }
        log.debug("[display: " + hivestackDisplayUuid + "] sending creative to [" + creativePostUrl + "]: " + creativeRequest);
        return webClient
                .post()
                .uri(creativePostUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, oauthService.getBearerToken(partner))
                .body(BodyInserters.fromValue(tempCreativeRequest))
                .accept(MediaType.APPLICATION_JSON)
                .exchange();
    }

    public Mono<Boolean> createReachCreative(CreativeRequest creativeRequest, BroadsignPartner partner, String requestID) {
        log.debug("Entered createReachCreative() with partner: " + partner + " requestId: " + requestID);
        CompletableFuture<Boolean> reachCreativeCompletableFuture = new CompletableFuture<>();
        Mono<Boolean> bodyCompletedMono = Mono.fromFuture(reachCreativeCompletableFuture);
        creativeRequest = createCreativeObject(partner.name(), creativeRequest);
        if (creativeRequest.getExternalId().isEmpty()) {
            log.debug("ExternalId is empty [display: " + requestID + "] [partner: " + partner + "]");
            reachCreativeCompletableFuture.complete(false);
            return bodyCompletedMono;
        }
        if (cacheData.creativeUrlExists(creativeRequest.getOriginalUrl())) {
            log.trace("[display: " + requestID + "] creative has already been added this session, skipping: " + creativeRequest.getExternalId() + " - " + creativeRequest.getOriginalUrl());
            reachCreativeCompletableFuture.complete(false);
            return bodyCompletedMono;
        }
        log.debug("[display: " + requestID + "] [partner: " + partner + "] adding creative: " + creativeRequest.getExternalId() + " - " + creativeRequest.getOriginalUrl());
        CreativeRequest finalCreativeRequest = creativeRequest;
        createCreativeWebClient(requestID, creativeRequest, partner)
                .subscribe(clientResponse -> {
                    Mono<String> stringMono = clientResponse.bodyToMono(String.class);
                    if (clientResponse.statusCode().is2xxSuccessful()) {
                        stringMono.subscribe(s -> {
                            JSONObject jsonObject = new JSONObject(s);
                            Integer id = jsonObject.getInt("id");
                            if (finalCreativeRequest.getOriginalUrl() != null) {
                                if (!cacheData.creativeUrlExists(finalCreativeRequest.getOriginalUrl())) {
                                    saveCreative(id.toString(), finalCreativeRequest.getOriginalUrl(), requestID, partner.name());
                                    reachCreativeCompletableFuture.complete(true);
                                } else {
                                    reachCreativeCompletableFuture.complete(false);
                                }

                            }
                        });
                    }
                    if (clientResponse.statusCode().is4xxClientError() || clientResponse.statusCode().is5xxServerError()) {
                        stringMono.subscribe(s -> {
                            if (s.contains("Must be unique inside")) {
                                log.debug("[display: " + requestID + "] creative was already in reach: " + finalCreativeRequest.getExternalId() + " - " + finalCreativeRequest.getOriginalUrl());
                                if (!cacheData.creativeUrlExists(finalCreativeRequest.getOriginalUrl())) {
                                    log.info("Saving previously created creative to database with url: " + finalCreativeRequest.getOriginalUrl());
                                    // this isn't really an error, so we're safe to add this to the cache.
                                    saveCreative(null, finalCreativeRequest.getOriginalUrl(), requestID, partner.name());
                                    reachCreativeCompletableFuture.complete(true);
                                }
                            } else {
                                log.error("[display: " + requestID + "] got error when POSTing " + partner + " creative " + finalCreativeRequest.getExternalId() + " - " + finalCreativeRequest.getOriginalUrl() + ": " + s);
                                reachCreativeCompletableFuture.complete(false);
                            }
                        });
                    }
                });
        log.debug("Exiting createReachCreative().");
        return bodyCompletedMono;
    }


    public void saveCreative(String id, String url, String hivestackDisplayUuid, String partner) {
        Creatives creatives = new Creatives();
        creatives.setReachId(id);
        creatives.setHivestackUrl(url);
        cacheData.saveCreativeUrl(creatives);
        log.info("Saved to creatives. [display: " + hivestackDisplayUuid + "] [partner: " + partner + "] [reachId: " + creatives.getReachId() + "] [url: " + creatives.getHivestackUrl() + "]");
    }

}
