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
package org.sonatype.nexus.repository.search.sql.query;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link SearchCursor} keyset pagination cursor.
 */
class SearchCursorTest
{
  @Test
  void testEncodeAndDecode() {
    // Create a cursor
    SearchCursor original = SearchCursor.from("maven2", 12345);

    // Encode it
    String encoded = original.encode();
    assertThat(encoded, notNullValue());
    assertThat(encoded.isEmpty(), is(false));

    // Decode it
    Optional<SearchCursor> decoded = SearchCursor.decode(encoded);
    assertTrue(decoded.isPresent());
    assertThat(decoded.get().getLastFormat(), equalTo("maven2"));
    assertThat(decoded.get().getLastComponentId(), equalTo(12345));
  }

  @Test
  void testDecodeNullToken() {
    Optional<SearchCursor> result = SearchCursor.decode(null);
    assertFalse(result.isPresent());
  }

  @Test
  void testDecodeEmptyToken() {
    Optional<SearchCursor> result = SearchCursor.decode("");
    assertFalse(result.isPresent());
  }

  @Test
  void testDecodeLegacyNumericToken() {
    // Legacy offset-based tokens are pure numeric strings like "50", "100"
    // These should return empty so we fall back to offset-based pagination
    Optional<SearchCursor> result = SearchCursor.decode("50");
    assertFalse(result.isPresent());

    result = SearchCursor.decode("100");
    assertFalse(result.isPresent());

    result = SearchCursor.decode("0");
    assertFalse(result.isPresent());
  }

  @Test
  void testDecodeInvalidBase64() {
    // Invalid base64 should return empty
    Optional<SearchCursor> result = SearchCursor.decode("not-valid-base64!!!");
    assertFalse(result.isPresent());
  }

  @Test
  void testDecodeInvalidJson() {
    // Valid base64 but invalid JSON should return empty
    String invalidJson = java.util.Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString("not json".getBytes());
    Optional<SearchCursor> result = SearchCursor.decode(invalidJson);
    assertFalse(result.isPresent());
  }

  @Test
  void testFromFactoryMethod() {
    SearchCursor cursor = SearchCursor.from("npm", 99999);
    assertThat(cursor.getLastFormat(), equalTo("npm"));
    assertThat(cursor.getLastComponentId(), equalTo(99999));
  }

  @Test
  void testConstructor() {
    SearchCursor cursor = new SearchCursor("docker", 54321);
    assertThat(cursor.getLastFormat(), equalTo("docker"));
    assertThat(cursor.getLastComponentId(), equalTo(54321));
  }

  @Test
  void testEquals() {
    SearchCursor cursor1 = SearchCursor.from("maven2", 100);
    SearchCursor cursor2 = SearchCursor.from("maven2", 100);
    SearchCursor cursor3 = SearchCursor.from("maven2", 200);
    SearchCursor cursor4 = SearchCursor.from("npm", 100);

    assertThat(cursor1.equals(cursor2), is(true));
    assertThat(cursor1.equals(cursor3), is(false));
    assertThat(cursor1.equals(cursor4), is(false));
    assertThat(cursor1.equals(null), is(false));
    assertThat(cursor1.equals("not a cursor"), is(false));
  }

  @Test
  void testHashCode() {
    SearchCursor cursor1 = SearchCursor.from("maven2", 100);
    SearchCursor cursor2 = SearchCursor.from("maven2", 100);

    assertThat(cursor1.hashCode(), equalTo(cursor2.hashCode()));
  }

  @Test
  void testToString() {
    SearchCursor cursor = SearchCursor.from("pypi", 777);
    String str = cursor.toString();

    assertThat(str.contains("pypi"), is(true));
    assertThat(str.contains("777"), is(true));
  }

  @Test
  void testRoundTripWithDifferentFormats() {
    // Test various format names to ensure encoding/decoding works correctly
    String[] formats = {"maven2", "npm", "pypi", "docker", "nuget", "go", "helm", "raw"};
    int componentId = 1;

    for (String format : formats) {
      SearchCursor original = SearchCursor.from(format, componentId++);
      String encoded = original.encode();
      Optional<SearchCursor> decoded = SearchCursor.decode(encoded);

      assertTrue(decoded.isPresent(), "Should decode for format: " + format);
      assertThat(decoded.get(), equalTo(original));
    }
  }

  @Test
  void testRoundTripWithLargeComponentId() {
    // Test with large component IDs that might exist in enterprise deployments
    SearchCursor original = SearchCursor.from("maven2", Integer.MAX_VALUE);
    String encoded = original.encode();
    Optional<SearchCursor> decoded = SearchCursor.decode(encoded);

    assertTrue(decoded.isPresent());
    assertThat(decoded.get().getLastComponentId(), equalTo(Integer.MAX_VALUE));
  }

  @Test
  void testEncodingIsUrlSafe() {
    // The encoded token should be URL-safe (no +, /, =)
    SearchCursor cursor = SearchCursor.from("test-format", 12345);
    String encoded = cursor.encode();

    assertFalse(encoded.contains("+"), "Encoded token should not contain +");
    assertFalse(encoded.contains("/"), "Encoded token should not contain /");
    assertFalse(encoded.contains("="), "Encoded token should not contain = (padding)");
  }
}
