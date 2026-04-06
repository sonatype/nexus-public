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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.sonatype.goodies.testsupport.TestSupport;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link CacheAttributeUtils}.
 */
public class CacheAttributeUtilsTest
    extends TestSupport
{
  @Test
  public void extractLastVerified_withJodaDateTimeString() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> cacheAttributes = new HashMap<>();
    cacheAttributes.put("last_verified", "2024-02-27T10:30:00.000Z");
    attributes.put("cache", cacheAttributes);

    Date result = CacheAttributeUtils.extractLastVerified(attributes);

    assertThat(result, notNullValue());
    assertThat(result.getTime(), is(new org.joda.time.DateTime("2024-02-27T10:30:00.000Z").toDate().getTime()));
  }

  @Test
  public void extractLastVerified_withLongTimestamp() {
    long timestamp = System.currentTimeMillis();
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> cacheAttributes = new HashMap<>();
    cacheAttributes.put("last_verified", timestamp);
    attributes.put("cache", cacheAttributes);

    Date result = CacheAttributeUtils.extractLastVerified(attributes);

    assertThat(result, notNullValue());
    assertThat(result.getTime(), is(timestamp));
  }

  @Test
  public void extractLastVerified_withNullAttributes() {
    Date result = CacheAttributeUtils.extractLastVerified(null);

    assertThat(result, nullValue());
  }

  @Test
  public void extractLastVerified_withDirectCacheMap() {
    // Test when the attributes map IS the cache map (direct access mode)
    long timestamp = System.currentTimeMillis();
    Map<String, Object> cacheMap = new HashMap<>();
    cacheMap.put("last_verified", timestamp);

    Date result = CacheAttributeUtils.extractLastVerified(cacheMap);

    assertThat(result, notNullValue());
    assertThat(result.getTime(), is(timestamp));
  }

  @Test
  public void extractLastVerified_withDirectCacheMap_JodaString() {
    // Test when the attributes map IS the cache map with Joda DateTime string
    Map<String, Object> cacheMap = new HashMap<>();
    cacheMap.put("last_verified", "2024-02-27T10:30:00.000Z");

    Date result = CacheAttributeUtils.extractLastVerified(cacheMap);

    assertThat(result, notNullValue());
    assertThat(result.getTime(), is(new org.joda.time.DateTime("2024-02-27T10:30:00.000Z").toDate().getTime()));
  }

  @Test
  public void extractLastVerified_withMissingCacheAttributes() {
    Map<String, Object> attributes = new HashMap<>();

    Date result = CacheAttributeUtils.extractLastVerified(attributes);

    assertThat(result, nullValue());
  }

  @Test
  public void extractLastVerified_withMissingLastVerified() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> cacheAttributes = new HashMap<>();
    attributes.put("cache", cacheAttributes);

    Date result = CacheAttributeUtils.extractLastVerified(attributes);

    assertThat(result, nullValue());
  }

  @Test
  public void extractLastVerified_withInvalidValue() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> cacheAttributes = new HashMap<>();
    cacheAttributes.put("last_verified", new Object());
    attributes.put("cache", cacheAttributes);

    Date result = CacheAttributeUtils.extractLastVerified(attributes);

    assertThat(result, nullValue());
  }

  @Test
  public void extractLastVerifiedAsOptional_withValidValue() {
    Map<String, Object> attributes = new HashMap<>();
    Map<String, Object> cacheAttributes = new HashMap<>();
    long timestamp = System.currentTimeMillis();
    cacheAttributes.put("last_verified", timestamp);
    attributes.put("cache", cacheAttributes);

    Optional<Date> result = CacheAttributeUtils.extractLastVerifiedAsOptional(attributes);

    assertThat(result.isPresent(), is(true));
    assertThat(result.get().getTime(), is(timestamp));
  }

  @Test
  public void extractLastVerifiedAsOptional_withNullValue() {
    Optional<Date> result = CacheAttributeUtils.extractLastVerifiedAsOptional(null);

    assertThat(result.isPresent(), is(false));
  }
}
