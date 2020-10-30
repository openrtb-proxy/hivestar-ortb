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

import com.squareup.pollexor.Thumbor;
import com.squareup.pollexor.ThumborUrlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.squareup.pollexor.ThumborUrlBuilder.format;
import static com.squareup.pollexor.ThumborUrlBuilder.quality;

@Component
public class ThumborUtil {

    private Thumbor thumbor;

    @Autowired
    public ThumborUtil(@Value("${thumbor.server}") String thumborServer,
                             @Value("${thumbor.key}") String thumborKey) {
        thumbor = Thumbor.create(thumborServer, thumborKey);
    }

    public String getJpgUrl(String imageUrl) {
        String encodedUrl = URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
        return thumbor.buildImage(encodedUrl).filter(format(ThumborUrlBuilder.ImageFormat.JPEG), quality(95)).toUrl();
    }

}
