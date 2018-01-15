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
package org.sonatype.nexus.repository.search;

import java.io.IOException;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.storage.Asset;
import org.sonatype.nexus.repository.storage.Bucket;
import org.sonatype.nexus.repository.storage.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createBucket;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createDetachedAsset;
import static org.sonatype.nexus.repository.storage.StorageTestUtil.createDetachedComponent;

public class DefaultComponentMetadataProducerTest
    extends TestSupport
{
  private static final String REPO_NAME = "repo";

  private static final String GROUP = "group";

  private static final String NAME = "name";

  private static final String VERSION = "1.2.3";

  private final ObjectMapper mapper = new ObjectMapper();

  @Mock
  private ComponentMetadataProducerExtension componentMetadataProducerExtension;

  private DefaultComponentMetadataProducer underTest;

  @Before
  public void setup() {
    underTest = new DefaultComponentMetadataProducer(ImmutableSet.of(componentMetadataProducerExtension));
  }

  @Test
  public void testGetMetadata() throws IOException {
    Bucket bucket = createBucket(REPO_NAME);
    Component component = createDetachedComponent(bucket, GROUP, NAME, VERSION);
    Iterable<Asset> assets = newArrayList(createDetachedAsset(bucket, NAME, component));
    Map<String, Object> additional = ImmutableMap.of("foo", "bar");

    String result = underTest.getMetadata(component, assets, additional);

    JsonNode json = mapper.readTree(result);

    assertValue(json, DefaultComponentMetadataProducer.FORMAT, "format-id");
    assertValue(json, DefaultComponentMetadataProducer.NAME, NAME);
    assertValue(json, DefaultComponentMetadataProducer.GROUP, GROUP);
    assertValue(json, DefaultComponentMetadataProducer.VERSION, VERSION);
    assertValue(json, "foo", "bar");

    JsonNode jsonAssets = json.get(DefaultComponentMetadataProducer.ASSETS);
    assertTrue(jsonAssets.isArray());
    assertThat(jsonAssets.size(), equalTo(1));
    JsonNode jsonAsset = jsonAssets.get(0);
    assertValue(jsonAsset, DefaultComponentMetadataProducer.NAME, NAME);

    verify(componentMetadataProducerExtension).getComponentMetadata(any(Component.class));
  }

  private void assertValue(final JsonNode json, final String jsonAttribute, final String value) {
    assertThat(json.get(jsonAttribute).asText(), equalTo(value));
  }
}
