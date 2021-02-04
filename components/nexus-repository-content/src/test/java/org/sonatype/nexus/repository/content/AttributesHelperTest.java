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
package org.sonatype.nexus.repository.content;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.AttributesMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.repository.content.AttributeOperation.APPEND;
import static org.sonatype.nexus.repository.content.AttributeOperation.OVERLAY;
import static org.sonatype.nexus.repository.content.AttributeOperation.PREPEND;
import static org.sonatype.nexus.repository.content.AttributeOperation.REMOVE;
import static org.sonatype.nexus.repository.content.AttributeOperation.SET;
import static org.sonatype.nexus.repository.content.AttributesHelper.applyAttributeChange;

/**
 * Test {@link AttributesHelper}.
 */
public class AttributesHelperTest
    extends TestSupport
{
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private AttributesMap attributes;

  @Before
  public void setUp() {
    attributes = new AttributesMap();
    attributes.set("notalist", "text");
    attributes.set("notamap", "text");
  }

  @Test
  public void testSimpleChanges() {
    assertThat(attributes.get("myvalue"), is(nullValue()));

    assertTrue(applyAttributeChange(attributes, SET, "myvalue", 1));
    assertFalse(applyAttributeChange(attributes, SET, "myvalue", 1));

    assertTrue(applyAttributeChange(attributes, REMOVE, "myvalue", null));
    assertFalse(applyAttributeChange(attributes, REMOVE, "myvalue", null));
  }

  @Test
  public void testListChanges() {
    assertThat(attributes.get("mylist"), is(nullValue()));

    assertTrue(applyAttributeChange(attributes, APPEND, "mylist", 2));
    assertThat((List<?>) attributes.get("mylist"), contains(2));

    assertTrue(applyAttributeChange(attributes, PREPEND, "mylist", 3));
    assertThat((List<?>) attributes.get("mylist"), contains(3, 2));

    assertTrue(applyAttributeChange(attributes, APPEND, "mylist", 1));
    assertThat((List<?>) attributes.get("mylist"), contains(3, 2, 1));

    try {
      applyAttributeChange(attributes, APPEND, "notalist", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Cannot append to non-list attribute"));
    }

    try {
      applyAttributeChange(attributes, PREPEND, "notalist", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Cannot prepend to non-list attribute"));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testMapChanges() throws IOException {
    assertThat(attributes.get("mymap"), is(nullValue()));

    Map<String, Object> overlay1 = objectMapper.readValue(
        "{"
        + "\"one\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"test\" : \"value1\""
        + "}}},"
        + "\"two\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"test\" : \"value2\""
        + "}}},"
        + "\"three\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"test\" : \"value3\""
        + "}}}}",
        Map.class);

    Map<String, Object> overlay2 = objectMapper.readValue(
        "{"
        + "\"one\" : {"
          + "\"added\" : \"value!\""
        + "},"
        + "\"two\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"added\" : \"value!\""
        + "}}},"
        + "\"added\" : \"value!\""
        + "}",
        Map.class);

    Map<String, Object> overlay3 = objectMapper.readValue(
        "{"
        + "\"two\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"test\" : \"value2 - overwrite not allow!\""
        + "}}}}",
        Map.class);


    Map<String, Object> expected = objectMapper.readValue(
        "{"
        + "\"one\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"test\" : \"value1\""
          + "}},"
          + "\"added\" : \"value!\""
        + "},"
        + "\"two\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"test\" : \"value2\","
              + "\"added\" : \"value!\""
        + "}}},"
        + "\"three\" : {"
          + "\"alpha\" : {"
            + "\"beta\" : {"
              + "\"test\" : \"value3\""
        + "}}},"
        + "\"added\" : \"value!\""
        + "}",
        Map.class);

    assertTrue(applyAttributeChange(attributes, OVERLAY, "mymap", overlay1));
    assertThat((Map<?, ?>) attributes.get("mymap"), is(overlay1));
    assertFalse(applyAttributeChange(attributes, OVERLAY, "mymap", overlay1));

    assertTrue(applyAttributeChange(attributes, OVERLAY, "mymap", overlay2));
    assertThat((Map<?, ?>) attributes.get("mymap"), is(expected));
    assertFalse(applyAttributeChange(attributes, OVERLAY, "mymap", overlay2));

    try {
      // cannot overlay map attribute with non-map value
      applyAttributeChange(attributes, OVERLAY, "mymap", "text");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("Conflict: cannot overlay "));
    }

    // can overlay existing entry with a different value
    assertTrue(applyAttributeChange(attributes, OVERLAY, "mymap", overlay3));
    assertFalse(applyAttributeChange(attributes, OVERLAY, "mymap", overlay3));

    try {
      // cannot overlay onto non-map attribute
      applyAttributeChange(attributes, OVERLAY, "notamap", overlay1);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("Conflict: cannot overlay "));
    }
  }

  @Test
  public void testNullsNotAllowed() {

    try {
      applyAttributeChange(attributes, null, "myvalue", 1);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      applyAttributeChange(attributes, SET, null, 1);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      applyAttributeChange(attributes, SET, "myvalue", null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      applyAttributeChange(attributes, APPEND, "mylist", null);
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      applyAttributeChange(attributes, PREPEND, "mylist", null);
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      applyAttributeChange(attributes, OVERLAY, "mymap", null);
    }
    catch (NullPointerException e) {
      // expected
    }
  }
}
