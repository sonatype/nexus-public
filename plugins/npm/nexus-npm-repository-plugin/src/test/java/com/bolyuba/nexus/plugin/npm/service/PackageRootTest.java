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
package com.bolyuba.nexus.plugin.npm.service;

import java.io.File;
import java.util.Map;

import org.sonatype.nexus.proxy.item.FileContentLocator;
import org.sonatype.sisu.litmus.testsupport.TestSupport;

import com.bolyuba.nexus.plugin.npm.NpmRepository;
import com.bolyuba.nexus.plugin.npm.service.internal.MetadataParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UT for {@link PackageRoot}
 */
public class PackageRootTest
    extends TestSupport
{
  private final ObjectMapper objectMapper;

  public PackageRootTest() {
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  @Test
  public void overlay() throws Exception {
    final Map<String, Object> commonjs1Map = objectMapper
        .readValue(util.resolveFile("src/test/npm/ROOT_commonjs_v1.json"),
            new TypeReference<Map<String, Object>>() { });
    final PackageRoot commonjs1 = new PackageRoot("repo", commonjs1Map);

    final Map<String, Object> commonjs2Map = objectMapper
        .readValue(util.resolveFile("src/test/npm/ROOT_commonjs_v2.json"),
            new TypeReference<Map<String, Object>>() { });
    final PackageRoot commonjs2 = new PackageRoot("repo", commonjs2Map);
    commonjs2.getProperties().put("flag", "2");

    final Map<String, Object> commonjs3Map = objectMapper
        .readValue(util.resolveFile("src/test/npm/ROOT_commonjs_vIncomplete.json"),
            new TypeReference<Map<String, Object>>() { });
    final PackageRoot commonjs3 = new PackageRoot("repo", commonjs3Map);
    commonjs3.getProperties().put("flag", "3");

    assertThat(commonjs1.getComponentId(), equalTo(commonjs2.getComponentId()));
    assertThat(commonjs1.getVersions().keySet(), hasItem("0.0.1"));
    assertThat(commonjs1.isIncomplete(), is(false));
    assertThat(commonjs1.getProperties().entrySet(), empty());

    commonjs1.overlay(commonjs2);

    assertThat(commonjs1.getComponentId(), equalTo(commonjs2.getComponentId()));
    assertThat(commonjs1.getVersions().keySet(), hasItems("0.0.1", "0.0.2"));
    assertThat(commonjs1.isIncomplete(), is(false));
    assertThat(commonjs1.getProperties().entrySet(), hasSize(1));
    assertThat(commonjs1.getProperties(), hasEntry("flag", "2"));

    commonjs1.overlay(commonjs3);

    assertThat(commonjs1.getVersions().keySet(), hasItems("0.0.1", "0.0.2", "0.0.3"));
    assertThat(commonjs1.isIncomplete(), is(true));
    assertThat(commonjs1.getProperties(), hasEntry("flag", "3"));

    // objectMapper.writeValue(System.out, commonjs1.getRaw());
  }

  @Test
  public void attachmentExtraction() throws Exception {
    final NpmRepository npmRepository = mock(NpmRepository.class);
    when(npmRepository.getId()).thenReturn("repo");
    final File uploadRequest = util.resolveFile("src/test/npm/ROOT_testproject.json");

    final File tmpDir = util.createTempDir();

    final MetadataParser parser = new MetadataParser(tmpDir);
    final PackageRoot root = parser
        .parsePackageRoot(npmRepository.getId(), new FileContentLocator(uploadRequest, NpmRepository.JSON_MIME_TYPE));

    assertThat(root.getAttachments().size(), is(1));
    assertThat(root.getAttachments(), hasKey("testproject-0.0.0.tgz"));
    final NpmBlob attachment = root.getAttachments().get("testproject-0.0.0.tgz");
    assertThat(attachment.getName(), is("testproject-0.0.0.tgz"));
    assertThat(attachment.getMimeType(), is("application/octet-stream"));
    assertThat(attachment.getLength(), is(276L));
    assertThat(attachment.getFile().isFile(), is(true));
    assertThat(attachment.getFile().length(), is(276L));

    JSONObject onDisk = new JSONObject(Files.toString(uploadRequest, Charsets.UTF_8));
    onDisk.remove("_attachments"); // omit "attachments" as they are processed separately
    JSONObject onStore = new JSONObject(objectMapper.writeValueAsString(root.getRaw()));
    JSONAssert.assertEquals(onDisk, onStore, false);
  }

  @Test
  public void shrinking() throws Exception {
    final Map<String, Object> commonjsRaw = objectMapper
        .readValue(util.resolveFile("src/test/npm/ROOT_commonjs_multiversion.json"),
            new TypeReference<Map<String, Object>>() { });
    final PackageRoot commonjs = new PackageRoot("repo", commonjsRaw);

    assertThat(commonjs.getVersions().entrySet(), hasSize(3));

    commonjs.shrinkPackageVersions();

    assertThat(commonjs.getVersions().entrySet(), hasSize(0));

    // now test the shrinked map: it's a string-string map with version-tag mapping (where tag avail)
    final Map<String, String> versions = (Map<String, String>) commonjs.getRaw().get("versions");
    assertThat(versions.entrySet(), hasSize(3));
    assertThat(versions, hasEntry("0.0.1", "0.0.1"));
    assertThat(versions, hasEntry("0.0.2", "stable"));
    assertThat(versions, hasEntry("0.0.3", "latest"));
  }

  @Test
  public void maintainTime() throws Exception {
    final Map<String, Object> commonjsRaw = objectMapper
        .readValue(util.resolveFile("src/test/npm/ROOT_commonjs_multiversion.json"),
            new TypeReference<Map<String, Object>>() { });
    final PackageRoot commonjs = new PackageRoot("repo", commonjsRaw);

    assertThat(commonjs.getRaw().get("time"), nullValue());

    commonjs.maintainTime();

    assertThat(commonjs.getRaw().get("time"), notNullValue());

    final Map<String, String> time = (Map<String, String>) commonjs.getRaw().get("time");
    assertThat(time, hasKey("created"));
    assertThat(time, hasKey("modified"));
    assertThat(time, hasKey("0.0.1"));
    assertThat(time, hasKey("0.0.2"));
    assertThat(time, hasKey("0.0.3"));
  }
}
