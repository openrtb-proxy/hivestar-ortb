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
import starproxy.model.cache.OAuthToken;
import starproxy.util.CacheData;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@Slf4j
public class OauthService {

    private WebClient webClient;

    OauthService(WebClient webClient) {
        this.webClient = webClient;
    }

    @Value("${reach.baseurl}")
    private String REACH_BASE_URL;

    @Value("${reach.oauth2-token-path}")
    private String OAUTH2_TOKEN_PATH;

    @Value("${reach.vistar.username}")
    private String VISTAR_USERNAME;

    @Value("${reach.vistar.password}")
    private String VISTAR_PASSWORD;

    @Value("${reach.vistar_french.username}")
    private String VISTAR_FRENCH_USERNAME;

    @Value("${reach.vistar_french.password}")
    private String VISTAR_FRENCH_PASSWORD;

    @Value("${reach.hivestack.username}")
    private String HIVESTACK_USERNAME;

    @Value("${reach.hivestack.password}")
    private String HIVESTACK_PASSWORD;

    @Autowired
    CacheData cacheData;

    public String getBearerToken(BroadsignPartner broadsignPartner) {
        OAuthToken oAuthToken = cacheData.getOauthToken(broadsignPartner.name());
        if (oAuthToken.getToken() != null) {
            Instant validUntill = oAuthToken.getExpiry();
            if (validUntill.isAfter(Instant.now())) {
                return oAuthToken.getToken();
            } else {
                bearerToken(broadsignPartner.name()).block();
                return getBearerToken(broadsignPartner);
            }
        } else {
            bearerToken(broadsignPartner.name()).block();
            return getBearerToken(broadsignPartner);
        }
    }

    public Mono<String> bearerToken(String partner) {
        log.debug("Entering bearerToken() with partner as " + partner);
        String username = "";
        String password = "";

        if (partner.equals(BroadsignPartner.BROADSIGN_HIVESTACK.name())) {
            username = HIVESTACK_USERNAME;
            password = HIVESTACK_PASSWORD;
        }
        if (partner.equals(BroadsignPartner.BROADSIGN_VISTAR.name())) {
            username = VISTAR_USERNAME;
            password = VISTAR_PASSWORD;
        }
        if (partner.equals(BroadsignPartner.BROADSIGN_VISTAR_FR.name())) {
            username = VISTAR_FRENCH_USERNAME;
            password = VISTAR_FRENCH_PASSWORD;
        }

        return webClient
                .post()
                .uri(REACH_BASE_URL + OAUTH2_TOKEN_PATH)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData("username", username)
                        .with("password", password))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(clientResponse -> {
                    JSONObject jsonObject = new JSONObject(clientResponse);
                    String accessToken = jsonObject.getString("access_token");
                    OAuthToken oAuthToken = new OAuthToken();
                    oAuthToken.setBroadsignPartner(partner);
                    Instant instant = Instant.now();
                    Instant validUntill = instant.plusSeconds(jsonObject.getLong("expires_in"));
                    oAuthToken.setExpiry(validUntill);
                    oAuthToken.setToken("Bearer " + accessToken);
                    cacheData.updateOauthToken(oAuthToken);
                })
                .doOnError(throwable -> {
                    log.error("Error fetching the bearer token for Reach operations." + throwable.getMessage());
                });
    }
}
