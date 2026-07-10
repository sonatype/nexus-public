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
package org.sonatype.nexus.rest.jackson2.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ObjectMapperProvider} to verify proper configuration of Jackson modules.
 */
public class ObjectMapperProviderTest
{
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    ObjectMapperProvider provider = new ObjectMapperProvider(List.of());
    objectMapper = provider.get();
  }

  @Test
  public void testObjectMapperIsConfigured() {
    assertThat(objectMapper, notNullValue());
  }

  @Test
  public void testNestedAttributesMapSerializationWithoutMetadata() throws Exception {
    // Test that NestedAttributesMap serializes without parent/key/backing metadata
    Map<String, Object> backing = new HashMap<>();
    backing.put("attr1", "value1");
    backing.put("attr2", "value2");

    NestedAttributesMap attributesMap = new NestedAttributesMap("test", backing);

    String json = objectMapper.writeValueAsString(attributesMap);

    assertThat(json, notNullValue());
    assertThat(json, containsString("attr1"));
    assertThat(json, containsString("value1"));
    // Verify that internal metadata is NOT serialized
    assertThat(json, not(containsString("\"parent\"")));
    assertThat(json, not(containsString("\"key\"")));
    assertThat(json, not(containsString("\"backing\"")));
  }

  @Test
  public void testNestedAttributesMapRoundTrip() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    backing.put("published-by", "jenkins");
    backing.put("version", "1.0.0");

    NestedAttributesMap original = new NestedAttributesMap("attributes", backing);

    String json = objectMapper.writeValueAsString(original);
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);

    assertThat(deserialized, notNullValue());
    assertThat(deserialized.get("published-by"), is("jenkins"));
    assertThat(deserialized.get("version"), is("1.0.0"));
  }

  @Test
  public void testJodaDateTimeSerialization() throws Exception {
    // Test that Joda DateTime is properly handled (NEXUS-49563)
    DateTime dateTime = new DateTime(2025, 11, 14, 10, 30, 0);

    String json = objectMapper.writeValueAsString(dateTime);

    assertThat(json, notNullValue());
    assertThat(json, containsString("2025"));

    // Verify deserialization
    DateTime deserialized = objectMapper.readValue(json, DateTime.class);
    assertThat(deserialized, notNullValue());
    assertEquals(dateTime.getYear(), deserialized.getYear());
    assertEquals(dateTime.getMonthOfYear(), deserialized.getMonthOfYear());
    assertEquals(dateTime.getDayOfMonth(), deserialized.getDayOfMonth());
  }

  @Test
  public void testJavaTimeModuleIsRegistered() throws Exception {
    // Verify that JavaTimeModule is working
    java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

    String json = objectMapper.writeValueAsString(now);
    assertThat(json, notNullValue());

    java.time.OffsetDateTime deserialized = objectMapper.readValue(json, java.time.OffsetDateTime.class);
    assertThat(deserialized, notNullValue());
  }

  @Test
  public void testComplexObjectWithNestedAttributesMapAndDateTime() throws Exception {
    // Test a realistic scenario: object containing both NestedAttributesMap and DateTime
    TestObject testObject = new TestObject();

    Map<String, Object> backing = new HashMap<>();
    backing.put("key1", "value1");
    testObject.attributes = new NestedAttributesMap("test", backing);
    testObject.created = new DateTime(2025, 11, 14, 10, 0, 0);
    testObject.name = "test-object";

    String json = objectMapper.writeValueAsString(testObject);

    assertThat(json, notNullValue());
    assertThat(json, containsString("key1"));
    assertThat(json, containsString("test-object"));
    assertThat(json, containsString("2025"));
    // Verify internal metadata not serialized
    assertThat(json, not(containsString("\"backing\"")));

    TestObject deserialized = objectMapper.readValue(json, TestObject.class);
    assertThat(deserialized, notNullValue());
    assertThat(deserialized.name, is("test-object"));
    assertThat(deserialized.attributes.get("key1"), is("value1"));
    assertThat(deserialized.created.getYear(), is(2025));
  }

  // Test class for complex object serialization
  public static class TestObject
  {
    public String name;

    public NestedAttributesMap attributes;

    public DateTime created;
  }
}
