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
package org.sonatype.nexus.repository.browse.internal.resources;

import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import static org.sonatype.nexus.repository.browse.internal.resources.AssetMapUtils.getValueFromAssetMap;
import static org.sonatype.nexus.repository.browse.internal.resources.ResourcesTestUtils.createAsset;

public class AssetMapUtilsTest
{
  private Map<String, Object> assetMap;

  @Before
  public void setup() {
    assetMap = createAsset("antlr.jar", "maven2", "first-sha1", of("extension", "jar"));
  }

  @Test
  public void testGetValueFromAssetMap_sha1() {
    runGetValueFromAssetMapTest(assetMap, "assets.attributes.checksum.sha1", "first-sha1");
  }

  @Test
  public void testGetValueFromAssetMap_MavenExtension() {
    runGetValueFromAssetMapTest(assetMap, "assets.attributes.maven2.extension", "jar");
  }

  @Test
  public void testGetValueFromAssetMap_BadQueryParam_ReturnsEmpty() {
    runGetValueFromAssetMapTest(assetMap, "junk", null);
  }

  @Test
  public void testGetValueFromAssetMap_BadQueryParam2_ReturnsEmpty() {
    runGetValueFromAssetMapTest(assetMap, "junk.junk", null);
  }

  @Test
  public void testGetValueFromAssetMap_MissingIdentifier_ReturnsEmpty() {
    runGetValueFromAssetMapTest(assetMap, null, null);
  }

  @Test
  public void testGetValueFromAssetMap_EmptyMap() {
    runGetValueFromAssetMapTest(emptyMap(), "assets.attributes.checksum.sha1", null);
  }

  private void runGetValueFromAssetMapTest(final Map<String, Object> assetMap, final String query, final String match) {
    Optional<Object> value = getValueFromAssetMap(assetMap, query);
    if (match == null) {
      assertThat(value, equalTo(empty()));
    }
    else {
      assertTrue(value.isPresent());
      assertThat(value.get(), equalTo(match));
    }
  }
}
