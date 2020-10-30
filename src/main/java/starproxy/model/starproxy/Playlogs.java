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

package starproxy.model.starproxy;

import lombok.Data;

import javax.persistence.*;


@Entity
@Table(name = "playlogs")
@Data
public class Playlogs {

    @Id
    @GeneratedValue
    private Integer id;

    @Column(name="reach_device_ifa")
    private String reachDeviceIfa;

    @Column(name="hivestack_display_uuid")
    private String hivestackDisplayUuid;

    @Column(name = "vistar_enabled")
    private String vistarEnabled;

    @Column(name = "vistar_language")
    private String vistarLanguage;

    @Column(name = "hivestack_enabled")
    private String hivestackEnabled;

    @Column(name = "generator_id")
    private String generatorId;

    @Column(name = "rott_ad_width")
    private Integer rottAdWidth;

    @Column(name = "rott_ad_height")
    private Integer rottAdHeight;

}
