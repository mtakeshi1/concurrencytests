package concurrencytest.util;

/*
 * Copyright (C) 2014-2020 Markus Junginger, greenrobot (http://greenrobot.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class PrimitiveArrayUtils {
    public static int getIntLE(byte[] bytes, int index) {
        return (bytes[index] & 0xff) | ((bytes[index + 1] & 0xff) << 8) |
                ((bytes[index + 2] & 0xff) << 16) | (bytes[index + 3] << 24);
    }
}
