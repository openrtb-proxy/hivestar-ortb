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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import starproxy.model.starproxy.Playlogs;
import starproxy.repository.PlaylogsRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
@Slf4j
@Data
public class StarproxyUtils {

    @Autowired
    PlaylogsRepository playlogsRepository;

    @PostConstruct
    public void init() {
        fetchAllReachMappings();
    }

    private Map<String,String> reachHivestackMappingsMap = new HashMap<>();
    private Map<String,String> reachVistarMappingsMap = new HashMap<>();
    private Map<String,String> reachVistarFrenchMappingsMap = new HashMap<>();


    //convert object to json string
    public String mapToJson(Object object) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(object);
    }

    @Transactional
    public void fetchAllReachMappings() {
        log.debug("Entering fetchAllReachMappings()");

        // fetching all reach to hivestack mappings from database
        List<Playlogs> playlogsList = playlogsRepository.findAllByReachDeviceIfaNotNull();

        if (!playlogsList.equals(null)) {
            log.debug("Adding Reach Mappings to cache.");
            playlogsList.stream().forEach(playlog -> {
                if (playlog.getHivestackDisplayUuid() != null && playlog.getHivestackEnabled().equals("Y")) {
                    log.debug(playlog.getReachDeviceIfa() + "----Hivestack---->" + playlog.getHivestackDisplayUuid());
                    reachHivestackMappingsMap.put(playlog.getReachDeviceIfa(),playlog.getHivestackDisplayUuid());
                }
                if (playlog.getGeneratorId() != null && playlog.getVistarEnabled().equals("Y")) {
                    String panelId = playlog.getGeneratorId().substring(playlog.getGeneratorId().lastIndexOf(":") + 1).strip();
                    if (!panelId.isBlank() && panelId != null && playlog.getVistarLanguage().equals("EN")) {
                        log.debug(playlog.getReachDeviceIfa() + "----Vistar---->" + panelId);
                        reachVistarMappingsMap.put(playlog.getReachDeviceIfa(), panelId);
                    }
                    if (!panelId.isBlank() && panelId != null && playlog.getVistarLanguage().equals("FR")) {
                        log.debug(playlog.getReachDeviceIfa() + "----Vistar-French---->" + panelId);
                        reachVistarFrenchMappingsMap.put(playlog.getReachDeviceIfa(), panelId);
                    }
                }
            });
        }
        log.debug("Exiting fetchAllReachMappings().");
    }

}
