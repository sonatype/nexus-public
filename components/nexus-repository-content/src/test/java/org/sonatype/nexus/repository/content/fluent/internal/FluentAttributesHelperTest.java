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
package org.sonatype.nexus.repository.content.fluent.internal;

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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.sonatype.nexus.repository.content.fluent.AttributeChange.APPEND;
import static org.sonatype.nexus.repository.content.fluent.AttributeChange.MERGE;
import static org.sonatype.nexus.repository.content.fluent.AttributeChange.PREPEND;
import static org.sonatype.nexus.repository.content.fluent.AttributeChange.REMOVE;
import static org.sonatype.nexus.repository.content.fluent.AttributeChange.SET;

/**
 * Test {@link FluentAttributesHelper}.
 */
public class FluentAttributesHelperTest
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

    FluentAttributesHelper.apply(attributes, SET, "myvalue", 1);

    FluentAttributesHelper.apply(attributes, REMOVE, "myvalue", null);

  }

  @Test
  public void testListChanges() {
    assertThat(attributes.get("mylist"), is(nullValue()));

    FluentAttributesHelper.apply(attributes, APPEND, "mylist", 2);
    assertThat((List<?>) attributes.get("mylist"), contains(2));

    FluentAttributesHelper.apply(attributes, PREPEND, "mylist", 3);
    assertThat((List<?>) attributes.get("mylist"), contains(3, 2));

    FluentAttributesHelper.apply(attributes, APPEND, "mylist", 1);
    assertThat((List<?>) attributes.get("mylist"), contains(3, 2, 1));

    try {
      FluentAttributesHelper.apply(attributes, APPEND, "notalist", 1);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Cannot append to non-list attribute"));
    }

    try {
      FluentAttributesHelper.apply(attributes, PREPEND, "notalist", 1);
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

    FluentAttributesHelper.apply(attributes, MERGE, "mymap", overlay1);
    assertThat((Map<?, ?>) attributes.get("mymap"), is(overlay1));

    FluentAttributesHelper.apply(attributes, MERGE, "mymap", overlay2);
    assertThat((Map<?, ?>) attributes.get("mymap"), is(expected));

    try {
      // cannot merge non-map value
      FluentAttributesHelper.apply(attributes, MERGE, "mymap", "text");
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("Conflict: cannot merge "));
    }

    try {
      // cannot overwrite existing entry with a different value
      FluentAttributesHelper.apply(attributes, MERGE, "mymap", overlay3);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("Conflict: cannot merge "));
    }

    try {
      // cannot merge into non-map attribute
      FluentAttributesHelper.apply(attributes, MERGE, "notamap", overlay1);
      fail("Expected IllegalArgumentException");
    }
    catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), containsString("Conflict: cannot merge "));
    }
  }

  @Test
  public void testNullsNotAllowed() {

    try {
      FluentAttributesHelper.apply(attributes, null, "myvalue", 1);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      FluentAttributesHelper.apply(attributes, SET, null, 1);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      FluentAttributesHelper.apply(attributes, SET, "myvalue", null);
      fail("Expected NullPointerException");
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      FluentAttributesHelper.apply(attributes, APPEND, "mylist", null);
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      FluentAttributesHelper.apply(attributes, PREPEND, "mylist", null);
    }
    catch (NullPointerException e) {
      // expected
    }

    try {
      FluentAttributesHelper.apply(attributes, MERGE, "mymap", null);
    }
    catch (NullPointerException e) {
      // expected
    }
  }
}
