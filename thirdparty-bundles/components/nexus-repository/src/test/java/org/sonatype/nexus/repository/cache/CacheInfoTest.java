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
package org.sonatype.nexus.repository.cache;

import java.util.Date;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.Asset;

import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

/**
 * Tests {@link CacheInfo}.
 */
public class CacheInfoTest
    extends TestSupport
{
  @Test
  public void nothingToExtract() {
    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object>newHashMap());
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    CacheInfo cacheInfo = CacheInfo.extractFromAsset(asset);
    assertThat(cacheInfo, nullValue());
  }

  @Test
  public void lastVerifiedOnlyExtract() {
    final DateTime now = DateTime.now();
    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object>newHashMap());
    attributes.child(CacheInfo.CACHE).set(CacheInfo.LAST_VERIFIED, now.toDate());
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    CacheInfo cacheInfo = CacheInfo.extractFromAsset(asset);
    assertThat(cacheInfo, notNullValue());
    assertThat(cacheInfo.getLastVerified(), equalTo(now));
  }

  @Test
  public void cacheTokenOnlyExtract() {
    final String cacheToken = "foo-bar";
    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object>newHashMap());
    attributes.child(CacheInfo.CACHE).set(CacheInfo.CACHE_TOKEN, cacheToken);
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    CacheInfo cacheInfo = CacheInfo.extractFromAsset(asset);
    assertThat(cacheInfo, nullValue());
  }

  @Test
  public void lastVerifiedAndCacheTokenExtract() {
    final DateTime now = DateTime.now();
    final String cacheToken = "foo-bar";
    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object>newHashMap());
    attributes.child(CacheInfo.CACHE).set(CacheInfo.LAST_VERIFIED, now.toDate());
    attributes.child(CacheInfo.CACHE).set(CacheInfo.CACHE_TOKEN, cacheToken);
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    CacheInfo cacheInfo = CacheInfo.extractFromAsset(asset);
    assertThat(cacheInfo, notNullValue());
    assertThat(cacheInfo.getLastVerified(), equalTo(now));
    assertThat(cacheInfo.getCacheToken(), equalTo(cacheToken));
  }

  @Test
  public void lastVerifiedAndCacheTokenApply() {
    final DateTime now = DateTime.now();
    final String cacheToken = "foo-bar";
    NestedAttributesMap attributes = new NestedAttributesMap(P_ATTRIBUTES, Maps.<String, Object>newHashMap());
    Asset asset = mock(Asset.class);
    when(asset.attributes()).thenReturn(attributes);
    CacheInfo cacheInfo = new CacheInfo(now, cacheToken);
    CacheInfo.applyToAsset(asset, cacheInfo);
    assertThat(asset.attributes().child(CacheInfo.CACHE).get(CacheInfo.LAST_VERIFIED, Date.class),
        equalTo(now.toDate()));
    assertThat(asset.attributes().child(CacheInfo.CACHE).get(CacheInfo.CACHE_TOKEN, String.class),
        equalTo(cacheToken));
  }
}
