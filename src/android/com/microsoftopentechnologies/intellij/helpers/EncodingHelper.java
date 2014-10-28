/**
 * Copyright 2014 Microsoft Open Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.microsoftopentechnologies.intellij.helpers;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;

public class EncodingHelper {
    public static Map<String, String> parseKeyValueList(String input, char delimiter, boolean urlDecode) {
        if(StringHelper.isNullOrWhiteSpace(input)) {
            return null;
        }

        Map<String, String> response = Splitter.on(delimiter).
                trimResults().
                omitEmptyStrings().
                withKeyValueSeparator('=').
                split(input);

        if(urlDecode) {
            return Maps.transformValues(response, new Function<String, String>() {
                @Override
                public String apply(String s) {
                    try {
                        return URLDecoder.decode(s, Charsets.UTF_8.name());
                    } catch (UnsupportedEncodingException e) {
                        return s;
                    }
                }
            });
        }
        else {
            return response;
        }
    }

    public static String toQueryString(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuffer buffer = new StringBuffer();
        String charset = Charsets.UTF_8.name();
        boolean first = true;

        for(Map.Entry<String, String> entry : params.entrySet()) {
            buffer.append(String.format("%s%s=%s",
                    first ? "" : "&",
                    URLEncoder.encode(entry.getKey(), charset),
                    URLEncoder.encode(entry.getValue(), charset)));
            if(first)
                first = false;
        }

        return buffer.toString();
    }
}
