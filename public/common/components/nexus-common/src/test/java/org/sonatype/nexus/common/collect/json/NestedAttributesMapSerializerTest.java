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
package org.sonatype.nexus.common.collect.json;

import java.util.HashMap;
import java.util.Map;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link NestedAttributesMapSerializer} and {@link NestedAttributesMapDeserializer}.
 */
public class NestedAttributesMapSerializerTest

{
  private ObjectMapper objectMapper;

  @Before
  public void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new SimpleModule()
        .addSerializer(NestedAttributesMap.class, new NestedAttributesMapSerializer())
        .addDeserializer(NestedAttributesMap.class, new NestedAttributesMapDeserializer()));
  }

  @Test
  public void testSerializeSimpleMap() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    backing.put("key1", "value1");
    backing.put("key2", "value2");

    NestedAttributesMap attributesMap = new NestedAttributesMap("test", backing);

    String json = objectMapper.writeValueAsString(attributesMap);

    // Verify the JSON doesn't contain parent/key metadata, only the backing map
    assertNotNull(json);
    assertTrue(json.contains("\"key1\":\"value1\""));
    assertTrue(json.contains("\"key2\":\"value2\""));
    assertTrue(!json.contains("\"parent\""));
    assertTrue(!json.contains("\"key\":\"test\""));
    assertTrue(!json.contains("\"backing\""));
  }

  @Test
  public void testSerializeWithSpecialCharacters() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    backing.put("published-by", "jenkins");
    backing.put("description", "Test with \"quotes\" and \nnewlines");
    backing.put("path", "C:\\Program Files\\test");

    NestedAttributesMap attributesMap = new NestedAttributesMap("attributes", backing);

    String json = objectMapper.writeValueAsString(attributesMap);

    // Verify the JSON is valid and can be parsed back
    assertNotNull(json);

    // Deserialize back to verify round-trip
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertNotNull(deserialized);
    assertEquals("jenkins", deserialized.get("published-by"));
    assertEquals("Test with \"quotes\" and \nnewlines", deserialized.get("description"));
    assertEquals("C:\\Program Files\\test", deserialized.get("path"));
  }

  @Test
  public void testSerializeNestedMaps() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    backing.put("simple", "value");

    Map<String, Object> nested = new HashMap<>();
    nested.put("nested-key", "nested-value");
    backing.put("nested", nested);

    NestedAttributesMap attributesMap = new NestedAttributesMap("root", backing);

    String json = objectMapper.writeValueAsString(attributesMap);

    // Verify the JSON structure
    assertNotNull(json);
    assertTrue(json.contains("\"simple\":\"value\""));
    assertTrue(json.contains("\"nested\""));
    assertTrue(json.contains("\"nested-key\":\"nested-value\""));

    // Verify round-trip
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertNotNull(deserialized);
    assertEquals("value", deserialized.get("simple"));

    @SuppressWarnings("unchecked")
    Map<String, Object> deserializedNested = (Map<String, Object>) deserialized.get("nested");
    assertNotNull(deserializedNested);
    assertEquals("nested-value", deserializedNested.get("nested-key"));
  }

  @Test
  public void testSerializeNullMap() throws Exception {
    NestedAttributesMap attributesMap = null;
    String json = objectMapper.writeValueAsString(attributesMap);
    assertEquals("null", json);
  }

  @Test
  public void testDeserializeNull() throws Exception {
    String json = "null";
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertEquals(null, deserialized);
  }

  @Test
  public void testRoundTripWithComplexData() throws Exception {
    // Create a complex nested structure similar to what tags might have
    Map<String, Object> backing = new HashMap<>();
    backing.put("published-by", "jenkins");
    backing.put("version", "1.0.0");
    backing.put("timestamp", System.currentTimeMillis());

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("author", "test-user");
    metadata.put("description", "Complex test with special chars: <>&\"'\n\t");
    backing.put("metadata", metadata);

    NestedAttributesMap original = new NestedAttributesMap("attributes", backing);

    // Serialize
    String json = objectMapper.writeValueAsString(original);
    assertNotNull(json);

    // Deserialize
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertNotNull(deserialized);

    // Verify all values preserved
    assertEquals(original.get("published-by"), deserialized.get("published-by"));
    assertEquals(original.get("version"), deserialized.get("version"));
    assertEquals(original.get("timestamp"), deserialized.get("timestamp"));

    @SuppressWarnings("unchecked")
    Map<String, Object> deserializedMetadata = (Map<String, Object>) deserialized.get("metadata");
    assertNotNull(deserializedMetadata);

    @SuppressWarnings("unchecked")
    Map<String, Object> originalMetadata = (Map<String, Object>) original.get("metadata");
    assertEquals(originalMetadata.get("author"), deserializedMetadata.get("author"));
    assertEquals(originalMetadata.get("description"), deserializedMetadata.get("description"));
  }

  @Test
  public void testSerializeEmptyMap() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    NestedAttributesMap attributesMap = new NestedAttributesMap("empty", backing);

    String json = objectMapper.writeValueAsString(attributesMap);
    assertNotNull(json);
    assertEquals("{}", json.trim());

    // Verify round-trip
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertNotNull(deserialized);
    assertTrue(deserialized.backing().isEmpty());
  }

  @Test
  public void testSerializeWithVariousDataTypes() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    backing.put("stringValue", "test");
    backing.put("intValue", 42);
    backing.put("longValue", 123456789L);
    backing.put("doubleValue", 3.14159);
    backing.put("booleanValue", true);
    backing.put("nullValue", null);

    NestedAttributesMap attributesMap = new NestedAttributesMap("types", backing);

    String json = objectMapper.writeValueAsString(attributesMap);
    assertNotNull(json);

    // Verify round-trip preserves types
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertNotNull(deserialized);
    assertEquals("test", deserialized.get("stringValue"));
    assertEquals(42, deserialized.get("intValue"));
    assertEquals(123456789L, ((Number) deserialized.get("longValue")).longValue());
    assertEquals(3.14159, ((Number) deserialized.get("doubleValue")).doubleValue(), 0.00001);
    assertEquals(true, deserialized.get("booleanValue"));
    assertEquals(null, deserialized.get("nullValue"));
  }

  @Test
  public void testSerializeDeeplyNestedStructure() throws Exception {
    Map<String, Object> backing = new HashMap<>();

    // Create a 3-level deep nested structure
    Map<String, Object> level1 = new HashMap<>();
    Map<String, Object> level2 = new HashMap<>();
    Map<String, Object> level3 = new HashMap<>();

    level3.put("deepValue", "found-me");
    level2.put("level3", level3);
    level1.put("level2", level2);
    backing.put("level1", level1);
    backing.put("topLevel", "value");

    NestedAttributesMap attributesMap = new NestedAttributesMap("root", backing);

    String json = objectMapper.writeValueAsString(attributesMap);
    assertNotNull(json);

    // Verify round-trip
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertNotNull(deserialized);

    assertEquals("value", deserialized.get("topLevel"));

    @SuppressWarnings("unchecked")
    Map<String, Object> l1 = (Map<String, Object>) deserialized.get("level1");
    assertNotNull(l1);

    @SuppressWarnings("unchecked")
    Map<String, Object> l2 = (Map<String, Object>) l1.get("level2");
    assertNotNull(l2);

    @SuppressWarnings("unchecked")
    Map<String, Object> l3 = (Map<String, Object>) l2.get("level3");
    assertNotNull(l3);

    assertEquals("found-me", l3.get("deepValue"));
  }

  @Test
  public void testNestedAttributesMapChildMethodAfterDeserialization() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    backing.put("key1", "value1");

    NestedAttributesMap original = new NestedAttributesMap("root", backing);

    // Serialize and deserialize
    String json = objectMapper.writeValueAsString(original);
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);

    // Use child() method on deserialized map
    NestedAttributesMap child = deserialized.child("newChild");
    assertNotNull(child);
    child.set("childKey", "childValue");

    // Verify the child is accessible
    assertEquals("childValue", deserialized.child("newChild").get("childKey"));
  }

  @Test
  public void testSerializeWithUnicodeCharacters() throws Exception {
    Map<String, Object> backing = new HashMap<>();
    backing.put("emoji", "🚀 rocket");
    backing.put("chinese", "你好世界");
    backing.put("arabic", "مرحبا بالعالم");
    backing.put("mixed", "Hello 世界 🌍");

    NestedAttributesMap attributesMap = new NestedAttributesMap("unicode", backing);

    String json = objectMapper.writeValueAsString(attributesMap);
    assertNotNull(json);

    // Verify round-trip preserves unicode
    NestedAttributesMap deserialized = objectMapper.readValue(json, NestedAttributesMap.class);
    assertNotNull(deserialized);
    assertEquals("🚀 rocket", deserialized.get("emoji"));
    assertEquals("你好世界", deserialized.get("chinese"));
    assertEquals("مرحبا بالعالم", deserialized.get("arabic"));
    assertEquals("Hello 世界 🌍", deserialized.get("mixed"));
  }
}
