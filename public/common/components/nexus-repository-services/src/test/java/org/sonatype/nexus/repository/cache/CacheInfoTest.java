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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.common.collect.AttributesMap;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for {@link CacheInfo}.
 */
public class CacheInfoTest
{
  @Test
  public void testFromMapWithValidData() {
    DateTime now = DateTime.now();
    Map<String, String> map = new HashMap<>();
    map.put(CacheInfo.LAST_VERIFIED, now.toString());
    map.put(CacheInfo.CACHE_TOKEN, "test-token");

    CacheInfo result = CacheInfo.fromMap(map);

    assertThat(result, is(notNullValue()));
    assertThat(result.getLastVerified().toString(), equalTo(now.toString()));
    assertThat(result.getCacheToken(), equalTo("test-token"));
  }

  @Test
  public void testFromMapWithNullMap() {
    CacheInfo result = CacheInfo.fromMap((Map<String, String>) null);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void testFromMapWithEmptyMap() {
    Map<String, String> emptyMap = Collections.emptyMap();
    CacheInfo result = CacheInfo.fromMap(emptyMap);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void testFromMapWithMissingLastVerified() {
    Map<String, String> map = new HashMap<>();
    map.put(CacheInfo.CACHE_TOKEN, "token-only");

    CacheInfo result = CacheInfo.fromMap(map);

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testFromMapWithOnlyLastVerified() {
    DateTime now = DateTime.now();
    Map<String, String> map = new HashMap<>();
    map.put(CacheInfo.LAST_VERIFIED, now.toString());

    CacheInfo result = CacheInfo.fromMap(map);

    assertThat(result, is(notNullValue()));
    assertThat(result.getLastVerified().toString(), equalTo(now.toString()));
    assertThat(result.getCacheToken(), is(nullValue()));
  }

  @Test
  public void testFromMapAttributesMapWithValidData() {
    DateTime now = DateTime.now();
    AttributesMap map = new AttributesMap();
    map.set(CacheInfo.LAST_VERIFIED, now.toString());
    map.set(CacheInfo.CACHE_TOKEN, "test-token");

    CacheInfo result = CacheInfo.fromMap(map);

    assertThat(result, is(notNullValue()));
    assertThat(result.getLastVerified().toString(), equalTo(now.toString()));
    assertThat(result.getCacheToken(), equalTo("test-token"));
  }

  @Test
  public void testFromMapAttributesMapWithNullMap() {
    CacheInfo result = CacheInfo.fromMap((AttributesMap) null);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void testFromMapAttributesMapWithEmptyMap() {
    AttributesMap emptyMap = new AttributesMap();
    CacheInfo result = CacheInfo.fromMap(emptyMap);
    assertThat(result, is(nullValue()));
  }

  @Test
  public void testFromMapAttributesMapWithMissingLastVerified() {
    AttributesMap map = new AttributesMap();
    map.set(CacheInfo.CACHE_TOKEN, "token-only");

    CacheInfo result = CacheInfo.fromMap(map);

    assertThat(result, is(nullValue()));
  }

  @Test
  public void testFromMapAttributesMapWithOnlyLastVerified() {
    DateTime now = DateTime.now();
    AttributesMap map = new AttributesMap();
    map.set(CacheInfo.LAST_VERIFIED, now.toString());

    CacheInfo result = CacheInfo.fromMap(map);

    assertThat(result, is(notNullValue()));
    assertThat(result.getLastVerified().toString(), equalTo(now.toString()));
    assertThat(result.getCacheToken(), is(nullValue()));
  }

  @Test
  public void testIsInvalidatedTrue() {
    CacheInfo info = new CacheInfo(DateTime.now(), CacheInfo.INVALIDATED);
    assertThat(info.isInvalidated(), is(true));
  }

  @Test
  public void testIsInvalidatedFalse() {
    CacheInfo info = new CacheInfo(DateTime.now(), "valid-token");
    assertThat(info.isInvalidated(), is(false));
  }

  @Test
  public void testIsInvalidatedFalseWhenNullToken() {
    CacheInfo info = new CacheInfo(DateTime.now(), null);
    assertThat(info.isInvalidated(), is(false));
  }

  @Test
  public void testToMap() {
    DateTime now = DateTime.now();
    CacheInfo info = new CacheInfo(now, "test-token");

    Map<String, String> result = info.toMap();

    assertThat(result.get(CacheInfo.LAST_VERIFIED), equalTo(now.toString()));
    assertThat(result.get(CacheInfo.CACHE_TOKEN), equalTo("test-token"));
    assertThat(result.size(), equalTo(2));
  }

  @Test
  public void testToMapWithoutToken() {
    DateTime now = DateTime.now();
    CacheInfo info = new CacheInfo(now, null);

    Map<String, String> result = info.toMap();

    assertThat(result.get(CacheInfo.LAST_VERIFIED), equalTo(now.toString()));
    assertThat(result.containsKey(CacheInfo.CACHE_TOKEN), is(false));
    assertThat(result.size(), equalTo(1));
  }

  @Test
  public void testRoundTrip() {
    DateTime now = DateTime.now();
    CacheInfo original = new CacheInfo(now, "test-token");

    Map<String, String> map = original.toMap();
    CacheInfo restored = CacheInfo.fromMap(map);

    assertThat(restored.getLastVerified().toString(), equalTo(original.getLastVerified().toString()));
    assertThat(restored.getCacheToken(), equalTo(original.getCacheToken()));
  }
}
