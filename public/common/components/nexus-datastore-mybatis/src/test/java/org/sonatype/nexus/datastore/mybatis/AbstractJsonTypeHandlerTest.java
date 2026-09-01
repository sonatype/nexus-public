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
package org.sonatype.nexus.datastore.mybatis;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link AbstractJsonTypeHandler}.
 */
public class AbstractJsonTypeHandlerTest
{
  /**
   * Test subclass to access the configured ObjectMapper.
   */
  private static class TestJsonTypeHandler
      extends AbstractJsonTypeHandler<Map<String, Object>>
  {
    // No-arg constructor, uses buildObjectMapper from parent
  }

  private ObjectMapper getObjectMapper(final TestJsonTypeHandler handler) throws Exception {
    Field objectMapperField = AbstractJsonTypeHandler.class.getDeclaredField("objectMapper");
    objectMapperField.setAccessible(true);
    return (ObjectMapper) objectMapperField.get(handler);
  }

  @Test
  public void configuresMaxStringLengthConstraint() throws Exception {
    TestJsonTypeHandler handler = new TestJsonTypeHandler();
    ObjectMapper mapper = getObjectMapper(handler);

    int maxStringLength = mapper.getFactory().streamReadConstraints().getMaxStringLength();
    assertThat(maxStringLength, is(equalTo(20_000_000)));
  }

  @Test
  public void roundTripsStringUnderLimit() throws Exception {
    TestJsonTypeHandler handler = new TestJsonTypeHandler();

    // Create a map with a 100-character string (well under the 20MB limit)
    Map<String, Object> input = new HashMap<>();
    input.put("key", "x".repeat(100));

    // Round-trip through JSON serialization/deserialization
    byte[] json = handler.writeToPlainJson(input);
    @SuppressWarnings("unchecked")
    Map<String, Object> output = (Map<String, Object>) handler.readFromPlainJson(json);

    assertThat(output.get("key"), is(equalTo("x".repeat(100))));
  }
}
