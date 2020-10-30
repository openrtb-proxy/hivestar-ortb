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

import starproxy.model.cache.VastDocument;
import starproxy.model.starproxy.Creatives;
import starproxy.model.cache.OAuthToken;
import starproxy.repository.CreativesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class CacheData {

    @Autowired
    CreativesRepository creativesRepository;

    @CachePut(value = "vastDocumentCache", key = "#vastDocument.id")
    public VastDocument updateVastDocument(VastDocument vastDocument) {
        log.info("Executing updateVastDocument method...");
        return vastDocument;
    }

    @Cacheable(value = "vastDocumentCache", key = "#id")
    public VastDocument getVastDocument(String id) {
        log.debug("Fetching vast document for uuid: " + id);
        return new VastDocument();
    }

    @CachePut(value = "oAuthTokenCache", key = "#oAuthToken.broadsignPartner")
    public OAuthToken updateOauthToken(OAuthToken oAuthToken) {
        log.info("Executing updateOauthToken method...");
        return oAuthToken;
    }

    @Cacheable(value = "oAuthTokenCache", key = "#broadsignPartner")
    public OAuthToken getOauthToken(String broadsignPartner) {
        log.debug("Fetching oauth token for partner: " + broadsignPartner);
        return new OAuthToken();
    }

    @CachePut(value = "creativeUrlsCache", key = "#creative.hivestackUrl")
    public boolean saveCreativeUrl(Creatives creative) {
        try {
            creativesRepository.save(creative);
            return true;
        } catch (Exception e) {
            log.error("Following error occured while saving creative in database." + e.getMessage());
            return false;
        }
    }

    @Cacheable(value = "creativeUrlsCache", key = "#url")
    public boolean creativeUrlExists(String url) {
        return creativesRepository.existsCreativeByHivestackUrl(url);
    }

}
