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
import java.util.Map;

import org.sonatype.nexus.configuration.application.ApplicationDirectories;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.service.NpmBlob;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class MetadataParserTest
    extends TestSupport
{
  private File tmpDir;

  @Mock
  private ApplicationDirectories applicationDirectories;

  private MetadataParser metadataParser;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Before
  public void setup() throws Exception {
    tmpDir = util.createTempDir();
    when(applicationDirectories.getWorkDirectory(anyString())).thenReturn(tmpDir);
    when(applicationDirectories.getTemporaryDirectory()).thenReturn(tmpDir);
    metadataParser = new MetadataParser(applicationDirectories.getTemporaryDirectory());
  }

  @Test
  public void attachmentParsing() throws Exception {
    final Map<String, NpmBlob> attachments = Maps.newHashMap();
    final JsonParser parser = objectMapper.getFactory()
        .createParser(
            "{\"first\":{\"stub\":true}, \"second\":{\"incomplete\":true}, \"third\":{\"content_type\":\"text/plain\",\"data\": \"VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=\"}}");
    metadataParser.parsePackageAttachments(parser, attachments);

    assertThat(attachments.entrySet(), hasSize(1));
    assertThat(attachments, hasKey("third"));
    assertThat(attachments.get("third").getMimeType(), equalTo("text/plain"));
    assertThat(attachments.get("third").getLength(), equalTo(29L)); // "This is a base64 encoded text"
  }
}
