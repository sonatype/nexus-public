/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.search.index;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class ElasticSearchIndexServiceImplJsonTest {

  @Test
  public void testRemoveAttributes() throws JsonProcessingException {
    String json = "{\n"
        + "  \"assets\": [\n"
        + "    {\n"
        + "      \"content_type\": \"text/plain\",\n"
        + "      \"name\": \"/info.txt\",\n"
        + "      \"attributes\": {\n"
        + "        \"cona.n\": {\n"
        + "          \"channel\": \"_\",\n"
        + "          \"baseVersion\": \"1.5.2\"\n"
        + "        },\n"
        + "        \"conan\": {\n"
        + "          \"channel\": \"_\",\n"
        + "          \"revision\": \"74cd46b9525f6d1af311660ef17ca48c\",\n"
        + "          \"packageId\": \"45708ed6e5806ef31e05ce6fc0318b81ec6e0f2c\",\n"
        + "          \"baseVersion\": \"1.5.2\",\n"
        + "          \"packageRevision\": \"27b6bae1eeb5886a6aa47e3deeae1789\"\n"
        + "        },\n"
        + "        \"checksum\": {\n"
        + "          \"md5\": \"68e017afcbbbc5f5fcbaa524ef445aab\"\n"
        + "        },\n"
        + "        \"content\": {\n"
        + "          \"last_modified\": 1681918942046\n"
        + "        }\n"
        + "      },\n"
        + "      \"id\": \"daa66d2e\"\n"
        + "    }\n"
        + "  ],\n"
        + "  \"format\": \"conan\",\n"
        + "  \"attributes\": {\n"
        + "    \"conan\": {\n"
        + "      \"channel\": \"_\",\n"
        + "      \"baseVersion\": \"1.5.2\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"tags\": []\n"
        + "}";

    String newJson = ElasticSearchIndexServiceImpl.filterConanAssetAttributes(json);
    String exspected = "{\n"
        + "  \"assets\": [\n"
        + "    {\n"
        + "      \"content_type\": \"text/plain\",\n"
        + "      \"name\": \"/info.txt\",\n"
        + "      \"attributes\": {\n"
        + "        \"conan\": {\n"
        + "          \"packageId\": \"45708ed6e5806ef31e05ce6fc0318b81ec6e0f2c\",\n"
        + "          \"packageRevision\": \"27b6bae1eeb5886a6aa47e3deeae1789\"\n"
        + "        },\n"
        + "        \"checksum\": {\n"
        + "          \"md5\": \"68e017afcbbbc5f5fcbaa524ef445aab\"\n"
        + "        }\n"
        + "      },\n"
        + "      \"id\": \"daa66d2e\"\n"
        + "    }\n"
        + "  ],\n"
        + "  \"format\": \"conan\",\n"
        + "  \"attributes\": {\n"
        + "    \"conan\": {\n"
        + "      \"channel\": \"_\",\n"
        + "      \"baseVersion\": \"1.5.2\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"tags\": []\n"
        + "}";
    ObjectMapper mapper = new ObjectMapper();
    assertEquals(mapper.readTree(exspected), mapper.readTree(newJson));
  }


  @Test
  public void testRemoveAttributesNoAssetAttributes() throws JsonProcessingException {
    String json = "{\n"
        + "  \"format\": \"conan\",\n"
        + "  \"attributes\": {\n"
        + "    \"conan\": {\n"
        + "      \"channel\": \"_\",\n"
        + "      \"baseVersion\": \"1.5.2\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"tags\": []\n"
        + "}";

    String newJson = ElasticSearchIndexServiceImpl.filterConanAssetAttributes(json);
    ObjectMapper mapper = new ObjectMapper();
    assertEquals(mapper.readTree(json), mapper.readTree(newJson));
  }

  @Test
  public void testRemoveAttributesNotConen() throws JsonProcessingException {
    String json = "{\n"
        + "  \"assets\": [\n"
        + "    {\n"
        + "      \"content_type\": \"text/plain\",\n"
        + "      \"name\": \"/info.txt\",\n"
        + "      \"attributes\": {\n"
        + "        \"cona.n\": {\n"
        + "          \"channel\": \"_\",\n"
        + "          \"baseVersion\": \"1.5.2\"\n"
        + "        },\n"
        + "        \"conan\": {\n"
        + "          \"channel\": \"_\",\n"
        + "          \"revision\": \"74cd46b9525f6d1af311660ef17ca48c\",\n"
        + "          \"packageId\": \"45708ed6e5806ef31e05ce6fc0318b81ec6e0f2c\",\n"
        + "          \"baseVersion\": \"1.5.2\",\n"
        + "          \"packageRevision\": \"27b6bae1eeb5886a6aa47e3deeae1789\"\n"
        + "        },\n"
        + "        \"checksum\": {\n"
        + "          \"md5\": \"68e017afcbbbc5f5fcbaa524ef445aab\"\n"
        + "        },\n"
        + "        \"content\": {\n"
        + "          \"last_modified\": 1681918942046\n"
        + "        }\n"
        + "      },\n"
        + "      \"id\": \"daa66d2e\"\n"
        + "    }\n"
        + "  ],\n"
        + "  \"format\": \"maven\",\n"
        + "  \"attributes\": {\n"
        + "    \"conan\": {\n"
        + "      \"channel\": \"_\",\n"
        + "      \"baseVersion\": \"1.5.2\"\n"
        + "    }\n"
        + "  },\n"
        + "  \"tags\": []\n"
        + "}";
    System.out.println(json);

    String newJson = ElasticSearchIndexServiceImpl.filterConanAssetAttributes(json);
    ObjectMapper mapper = new ObjectMapper();
    assertEquals(mapper.readTree(json), mapper.readTree(newJson));
  }
}
