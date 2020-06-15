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
package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.search.query.SearchUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.rest.internal.resources.AssetMapUtils.getValueFromAssetMap;
import static org.sonatype.nexus.repository.rest.internal.resources.ResourcesTestUtils.createAsset;

public class AssetMapUtilsTest
    extends TestSupport
{
  private static final String CLASSIFIER_ATTRIBUTE_NAME = "assets.attributes.maven2.classifier";

  private static final String EXTENSION_ATTRIBUTE_NAME = "assets.attributes.maven2.extension";

  private static final String ARTIFACT_ID_ATTRIBUTE_NAME = "assets.attributes.maven2.artifactId";

  private Map<String, Object> assetMap;

  private Map<String, Object> assetMapWithClassifier;

  private AssetMapUtils underTest;

  @Mock
  private SearchUtils searchUtils;

  @Before
  public void setup() {
    assetMap = createAsset("antlr.jar", "maven2", "first-sha1", of("extension", "jar"));
    assetMapWithClassifier = createAsset("antlr.jar", "maven2", "first-sha1", of("extension", "jar", "classifier", "sources"));
    underTest = new AssetMapUtils(searchUtils);
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

  @Test
  public void testGetValueFromAssetMap_IncludeClassifierFlag_ReturnsOne() {
    Optional<Object> value = getValueFromAssetMap(assetMapWithClassifier, CLASSIFIER_ATTRIBUTE_NAME);
    assertTrue(value.isPresent());
    assertThat(value.get(), equalTo("sources"));
  }

  @Test
  public void testGetValueFromAssetMap_IncludeClassifierFlag() {
    Optional<Object> value = getValueFromAssetMap(assetMap, EXTENSION_ATTRIBUTE_NAME);
    assertTrue(value.isPresent());
    assertThat(value.get(), equalTo("jar"));
  }

  @Test
  public void testFilterAsset_AssetMapClassifier_AssetParamClassifier() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(CLASSIFIER_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(CLASSIFIER_ATTRIBUTE_NAME, "sources");

    assertTrue(underTest.filterAsset(assetMapWithClassifier, assetParams));
  }

  @Test
  public void testFilterAsset_AssetMapNoClassifier_AssetParamNoClassifier() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(EXTENSION_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(EXTENSION_ATTRIBUTE_NAME, "jar");

    assertTrue(underTest.filterAsset(assetMap, assetParams));
  }

  @Test
  public void testFilterAsset_AssetMapNoClassifier_AssetParamClassifier() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(CLASSIFIER_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(CLASSIFIER_ATTRIBUTE_NAME, "sources");

    assertFalse(underTest.filterAsset(assetMap, assetParams));
  }

  @Test
  public void testFilterAsset_AssetMapClassifier_AssetParamNoClassifier() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(EXTENSION_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(EXTENSION_ATTRIBUTE_NAME, "sources");

    assertFalse(underTest.filterAsset(assetMapWithClassifier, assetParams));
  }

  @Test
  public void testFilterAsset_AssetMapNoClassifier_AssetParamEmptyClassifier() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(CLASSIFIER_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(CLASSIFIER_ATTRIBUTE_NAME, "");

    assertTrue(underTest.filterAsset(assetMap, assetParams));
  }

  @Test
  public void testFilterAsset_AssetMapClassifier_AssetParamEmptyClassifier() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(CLASSIFIER_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(CLASSIFIER_ATTRIBUTE_NAME, "");

    assertFalse(underTest.filterAsset(assetMapWithClassifier, assetParams));
  }

  @Test
  public void testFilterAsset_AssetMapClassifier_EmptyAssetParam() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(CLASSIFIER_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();

    assertTrue(underTest.filterAsset(assetMapWithClassifier, assetParams));
  }

  @Test
  public void testFilterAsset_MultipleAssetParams() {
    when(searchUtils.getFullAssetAttributeName(CLASSIFIER_ATTRIBUTE_NAME)).thenReturn(CLASSIFIER_ATTRIBUTE_NAME);
    when(searchUtils.getFullAssetAttributeName(EXTENSION_ATTRIBUTE_NAME)).thenReturn(EXTENSION_ATTRIBUTE_NAME);
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(CLASSIFIER_ATTRIBUTE_NAME, "sources");
    assetParams.add(EXTENSION_ATTRIBUTE_NAME, "jar");

    assertTrue(underTest.filterAsset(assetMapWithClassifier, assetParams));
    assertFalse(underTest.filterAsset(assetMap, assetParams));
  }

  @Test
  public void testFilterAsset_GetEmptyAssetParams() {
    when(searchUtils.getFullAssetAttributeName(any(String.class))).thenReturn(any(String.class));
    List<String> emptyAssetParamsList = underTest.getEmptyAssetParams(getPopulatedMultiValueMap());

    assertThat(emptyAssetParamsList.size(), equalTo(1));
  }

  @Test
  public void testFilterAsset_GetNonEmptyAssetParams() {
    when(searchUtils.getFullAssetAttributeName(CLASSIFIER_ATTRIBUTE_NAME)).thenReturn(CLASSIFIER_ATTRIBUTE_NAME);
    when(searchUtils.getFullAssetAttributeName(EXTENSION_ATTRIBUTE_NAME)).thenReturn(EXTENSION_ATTRIBUTE_NAME);
    when(searchUtils.getFullAssetAttributeName(ARTIFACT_ID_ATTRIBUTE_NAME)).thenReturn(ARTIFACT_ID_ATTRIBUTE_NAME);
    Map<String, String> nonEmptyAssetParamsList = underTest.getNonEmptyAssetParams(getPopulatedMultiValueMap());

    assertThat(nonEmptyAssetParamsList.size(), equalTo(2));
    assertTrue(nonEmptyAssetParamsList.containsKey(ARTIFACT_ID_ATTRIBUTE_NAME));
    assertThat(nonEmptyAssetParamsList.get(ARTIFACT_ID_ATTRIBUTE_NAME), equalTo("foo"));
    assertTrue(nonEmptyAssetParamsList.containsKey(EXTENSION_ATTRIBUTE_NAME));
    assertThat(nonEmptyAssetParamsList.get(EXTENSION_ATTRIBUTE_NAME), equalTo("jar"));
    assertFalse(nonEmptyAssetParamsList.containsKey(CLASSIFIER_ATTRIBUTE_NAME));
  }

  private MultivaluedMap<String, String> getPopulatedMultiValueMap() {
    MultivaluedMap<String, String>  assetParams = new MultivaluedHashMap<>();
    assetParams.add(CLASSIFIER_ATTRIBUTE_NAME, "");
    assetParams.add(EXTENSION_ATTRIBUTE_NAME, "jar");
    assetParams.add(ARTIFACT_ID_ATTRIBUTE_NAME, "foo");
    return assetParams;
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
