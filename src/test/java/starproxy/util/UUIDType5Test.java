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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UUIDType5Test {

    @Test
    void fromUrl() throws Exception {
        assertEquals("f15e417d-16c0-51ae-adb6-6fbbd50c5eb8", UUIDType5.fromUrlWithStarProxyNamespace("http://cow.org"));
        assertEquals("f15e417d-16c0-51ae-adb6-6fbbd50c5eb8", UUIDType5.fromUrlWithStarProxyNamespace("http://cow.org?moo&moo&moo"));
        assertEquals("5637087f-e266-50e0-88a5-e3f800a65b72", UUIDType5.fromUrlWithStarProxyNamespace("http://google.com"));
        assertEquals("5637087f-e266-50e0-88a5-e3f800a65b72", UUIDType5.fromUrlWithStarProxyNamespace("http://google.com?somequerystring"));
    }
}