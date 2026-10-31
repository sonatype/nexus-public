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
package org.sonatype.nexus.repository.config.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ConfigurationData}
 */
class ConfigurationDataTest
{
  private ConfigurationData underTest;

  @BeforeEach
  void setUp() {
    underTest = new ConfigurationData();
  }

  @Test
  void testSetAttributes_ConvertsHashMapToConcurrentHashMap() {
    Map<String, Map<String, Object>> attributes = new HashMap<>();
    attributes.put("test", new HashMap<>());

    underTest.setAttributes(attributes);

    assertTrue(underTest.getAttributes() instanceof ConcurrentHashMap);
    assertEquals(attributes, underTest.getAttributes());
  }

  @Test
  void testSetAttributes_PreservesConcurrentHashMap() {
    Map<String, Map<String, Object>> attributes = new ConcurrentHashMap<>();
    attributes.put("test", new ConcurrentHashMap<>());

    underTest.setAttributes(attributes);

    assertSame(attributes, underTest.getAttributes());
  }

  @Test
  void testSetAttributes_AcceptsNull() {
    underTest.setAttributes(null);

    assertNull(underTest.getAttributes());
  }

  @Test
  void testAttributes_CreatesConcurrentHashMapWhenNull() {
    assertNull(underTest.getAttributes());

    underTest.attributes("test");

    assertNotNull(underTest.getAttributes());
    assertTrue(underTest.getAttributes() instanceof ConcurrentHashMap);
  }

  @Test
  void testAttributes_CreatesNestedConcurrentHashMap() {
    underTest.attributes("test");

    Map<String, Map<String, Object>> attributes = underTest.getAttributes();
    assertNotNull(attributes);
    assertTrue(attributes.get("test") instanceof ConcurrentHashMap);
  }

  @Test
  void testAttributes_ReturnsExistingNestedMap() {
    underTest.attributes("test").set("key", "value");

    Map<String, Map<String, Object>> attributes = underTest.getAttributes();
    assertNotNull(attributes);
    assertEquals("value", attributes.get("test").get("key"));
  }

  @Test
  void testAttributes_ThreadSafe() throws InterruptedException {
    Thread[] threads = new Thread[10];
    for (int i = 0; i < threads.length; i++) {
      final int index = i;
      threads[i] = new Thread(() -> {
        underTest.attributes("key" + index).set("subkey", "value" + index);
      });
    }

    for (Thread thread : threads) {
      thread.start();
    }

    for (Thread thread : threads) {
      thread.join();
    }

    Map<String, Map<String, Object>> attributes = underTest.getAttributes();
    assertNotNull(attributes);
    assertTrue(attributes instanceof ConcurrentHashMap);

    for (int i = 0; i < 10; i++) {
      String key = "key" + i;
      assertTrue(attributes.containsKey(key));
      assertTrue(attributes.get(key) instanceof ConcurrentHashMap);
      assertEquals("value" + i, attributes.get(key).get("subkey"));
    }
  }

  @Test
  void testCopy_DeepCopiesAttributes() {
    Map<String, Map<String, Object>> originalAttributes = new HashMap<>();
    Map<String, Object> nested = new HashMap<>();
    nested.put("key", "value");
    originalAttributes.put("test", nested);

    underTest.setAttributes(originalAttributes);

    ConfigurationData copy = underTest.copy();

    assertNotNull(copy.getAttributes());
    assertEquals(originalAttributes, copy.getAttributes());
    assertNull(copy.getId()); // ID should not be copied
  }

  @Test
  void testCopy_ModifyingCopyDoesNotAffectOriginal() {
    underTest.attributes("test").set("key", "original");

    ConfigurationData copy = underTest.copy();
    copy.attributes("test").set("key", "modified");

    assertEquals("original", underTest.getAttributes().get("test").get("key"));
    assertEquals("modified", copy.getAttributes().get("test").get("key"));
  }
}
