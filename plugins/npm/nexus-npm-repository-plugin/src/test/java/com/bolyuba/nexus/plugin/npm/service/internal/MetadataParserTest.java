/*
 * Copyright (c) 2007-2014 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.bolyuba.nexus.plugin.npm.service.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.nexus.proxy.item.ContentLocator;
import org.sonatype.security.SecuritySystem;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.bolyuba.nexus.plugin.npm.NpmRepository.JSON_MIME_TYPE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MetadataParserTest
    extends TestSupport
{
  private File tmpDir;

  @Mock
  private ApplicationDirectories applicationDirectories;

  @Mock
  private SecuritySystem securitySystem;

  private MetadataParser metadataParser;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void setup() throws Exception {
    tmpDir = util.createTempDir();
    when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(tmpDir);
    when(applicationDirectories.getTemporaryDirectory()).thenReturn(tmpDir);
    metadataParser = new MetadataParser(applicationDirectories.getTemporaryDirectory(), securitySystem);
  }

  @Test
  public void attachmentParsing() throws Exception {
    final Map<String, NpmBlob> attachments = Maps.newHashMap();
    final JsonParser parser = objectMapper.getFactory()
        .createParser(
            "{\"first\":{\"stub\":true}, \"second\":{\"incomplete\":true}, \"third\":{\"content_type\":\"text/plain\",\"data\": \"VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=\"}}");
    parser.nextToken();
    metadataParser.parsePackageAttachments(parser, attachments);

    assertThat(attachments.entrySet(), hasSize(1));
    assertThat(attachments, hasKey("third"));
    assertThat(attachments.get("third").getMimeType(), equalTo("text/plain"));
    assertThat(attachments.get("third").getLength(), equalTo(29L)); // "This is a base64 encoded text"
  }

  @Test
  public void canFetchSimulatedAll() throws IOException {
    InputStream stream = getClass().getResourceAsStream("/all.json");
    ContentLocator content = mock(ContentLocator.class);

    when(content.getMimeType()).thenReturn(JSON_MIME_TYPE);
    when(content.getContent()).thenReturn(stream);
    metadataParser.parseRegistryRoot("repoId", content);
  }

  @Test //NEXUS-15720
  public void canParseWithMaintainerNotAnArray() throws Exception {
    InputStream stream = getClass().getResourceAsStream("/maintainer_not_array.json");
    ContentLocator content = mock(ContentLocator.class);

    when(content.getMimeType()).thenReturn(JSON_MIME_TYPE);
    when(content.getContent()).thenReturn(stream);
    metadataParser.parsePackageRoot("repoId", content);
  }

  @Test //NEXUS-15720
  public void canParseWithMaintainerAsShortenedArray() throws Exception {
    InputStream stream = getClass().getResourceAsStream("/maintainer_shortened_array.json");
    ContentLocator content = mock(ContentLocator.class);

    when(content.getMimeType()).thenReturn(JSON_MIME_TYPE);
    when(content.getContent()).thenReturn(stream);
    metadataParser.parsePackageRoot("repoId", content);
  }
  
  @Test //NEXUS-17202
  public void canParseMetadataWithLargeNumber() throws Exception {
    InputStream stream = getClass().getResourceAsStream("/metadata-with-large-number.json");
    ContentLocator content = mock(ContentLocator.class);

    when(content.getMimeType()).thenReturn(JSON_MIME_TYPE);
    when(content.getContent()).thenReturn(stream);
    metadataParser.parsePackageRoot("repoId", content);
  }
}
