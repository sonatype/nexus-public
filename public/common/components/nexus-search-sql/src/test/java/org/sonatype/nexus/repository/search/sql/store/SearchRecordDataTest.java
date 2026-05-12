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
package org.sonatype.nexus.repository.search.sql.store;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

class SearchRecordDataTest
{
  private final SearchRecordData underTest = new SearchRecordData();

  @Test
  void testKeywords() {
    underTest.addKeyword("foo");
    underTest.addKeyword("foo");

    assertThat(underTest.getKeywords(), contains("'foo'"));
  }

  @Test
  void testKeywords_emptyCases() {
    underTest.addKeyword(null);
    underTest.addKeyword("");
    underTest.addKeywords(null);
    underTest.addKeywords(List.of());
    underTest.addKeywords(List.of(""));

    assertThat(underTest.getKeywords(), empty());
  }

  @Test
  void testAddFormatFieldValue1() {
    underTest.addFormatFieldValue1("foo.bar", true);
    underTest.addFormatFieldValue1("test.asdf");

    assertThat(underTest.getFormatFieldValues1(), contains("'foo.bar'", "'test.asdf'", "'test':1 'asdf':2"));
  }

  @Test
  void testAddFormatFieldValue2() {
    underTest.addFormatFieldValue2("test.asdf");

    assertThat(underTest.getFormatFieldValues2(), contains("'test.asdf'", "'test':1 'asdf':2"));
  }

  @Test
  void testAddFormatFieldValue3() {
    underTest.addFormatFieldValue3("test.asdf");

    assertThat(underTest.getFormatFieldValues3(), contains("'test.asdf'", "'test':1 'asdf':2"));
  }

  @Test
  void testAddFormatFieldValue4() {
    underTest.addFormatFieldValue4("foo.bar", true);
    underTest.addFormatFieldValue4("test.asdf");

    assertThat(underTest.getFormatFieldValues4(), contains("'foo.bar'", "'test.asdf'", "'test':1 'asdf':2"));
  }

  @Test
  void testAddFormatFieldValue5() {
    underTest.addFormatFieldValue5("test.asdf");

    assertThat(underTest.getFormatFieldValues5(), contains("'test.asdf'", "'test':1 'asdf':2"));
  }

  @Test
  void testAddFormatFieldValue6() {
    underTest.addFormatFieldValue6("foo.bar", true);
    underTest.addFormatFieldValue6("test.asdf");

    assertThat(underTest.getFormatFieldValues6(), contains("'foo.bar'", "'test.asdf'", "'test':1 'asdf':2"));
  }

  @Test
  void testAddFormatFieldValue7() {
    underTest.addFormatFieldValue7("test.asdf");

    assertThat(underTest.getFormatFieldValues7(), contains("'test.asdf'", "'test':1 'asdf':2"));
  }

  @Test
  void testAddVersionNames() {
    underTest.addVersionNames("1.0.0");
    underTest.addVersionNames("1-0-0"); // different version format, but same tokens

    assertThat(underTest.getVersionNames(), contains("'1.0.0'", "'1':1 '0':2 '0':3", "'1-0-0'"));
  }

  @Test
  void testAddVersionNames_emptyCases() {
    underTest.addVersionNames(null);
    underTest.addVersionNames("");
    underTest.addVersionNames("   ");

    assertThat(underTest.getVersionNames(), empty());
  }

  @Test
  void testAddVersionNames_tokenization() {
    underTest.addVersionNames("1.2.3-alpha");

    assertThat(underTest.getVersionNames(), contains("'1.2.3-alpha'", "'1':1 '2':2 '3':3 'alpha':4"));
  }

  @Test
  void testAddVersionNames_hashVersion() {
    underTest.addVersionNames("6944e1c");

    assertThat(underTest.getVersionNames(), contains("'6944e1c'"));
  }

  @Test
  void testAddVersionNames_longHashVersion() {
    underTest.addVersionNames("a1b2c3d4e5f6789");

    assertThat(underTest.getVersionNames(), contains("'a1b2c3d4e5f6789'"));
  }

  @Test
  void testAddVersionNames_hashWithSuffix() {
    underTest.addVersionNames("1a2b3c4-snapshot");

    assertThat(underTest.getVersionNames(), contains("'1a2b3c4-snapshot'", "'1a2b3c4':1 'snapshot':2"));
  }

  @Test
  void testAddNamespaceNames() {
    underTest.addNamespaceNames("com.example");
    underTest.addNamespaceNames("com.example"); // duplicate should not be added again

    assertThat(underTest.getNamespaceNames(), contains("'com.example'", "'com':1 'example':2"));
  }

  @Test
  void testAddNamespaceNames_emptyCases() {
    underTest.addNamespaceNames(null);
    underTest.addNamespaceNames("");
    underTest.addNamespaceNames("   ");

    assertThat(underTest.getNamespaceNames(), empty());
  }

  @Test
  void testAddNamespaceNames_tokenization() {
    underTest.addNamespaceNames("com.example.library");

    assertThat(underTest.getNamespaceNames(), contains("'com.example.library'", "'com':1 'example':2 'library':3"));
  }

  @Test
  void testAddTokens() {
    assertTokens("test.bar", "'test.bar'", "'test':1 'bar':2");
    assertTokens("test/bar", "'test/bar'", "'test':1 'bar':2");
    assertTokens("test\\bar", "'test\\\\bar'", "'test':1 'bar':2");
    assertTokens("test bar", "'test bar'", "'test':1 'bar':2");
    assertTokens("te'st.bar", "'te\\'st.bar'", "'te\\'st':1 'bar':2");

    List<String> result = new ArrayList<>();
    underTest.addTokens("test/bar", result, true);
    assertThat(result, contains("'test/bar'"));
  }

  @Test
  void testAddTokens_longPhraseByteLimitExceeded() {
    String longPhrase = "a".repeat(3000);
    List<String> result = new ArrayList<>();

    underTest.addTokens(longPhrase, result, false);

    assertThat(result, empty());
  }

  @Test
  void testAddTokens_tokenizedStringExceedsByteLimit() {
    StringBuilder phrase = new StringBuilder();
    for (int i = 0; i < 500; i++) {
      phrase.append("keyword").append(i).append(" ");
    }

    List<String> result = new ArrayList<>();
    underTest.addTokens(phrase.toString(), result, false);

    assertThat(result.size(), greaterThan(0));
    String tokenizedString = result.get(result.size() - 1);
    int byteLength = tokenizedString.getBytes(StandardCharsets.UTF_8).length;
    assertThat(byteLength, lessThanOrEqualTo(2046));
  }

  @Test
  void testAddTokens_utf8MultiByteCharacters() {
    String multiBytePhrase = "react-router angular-cli vue-loader";
    List<String> result = new ArrayList<>();

    underTest.addTokens(multiBytePhrase, result, false);

    assertThat(result, contains("'react-router angular-cli vue-loader'",
        "'react':1 'router':2 'angular':3 'cli':4 'vue':5 'loader':6"));
  }

  @Test
  void testAddTokens_utf8LongMultiByteString() {
    String longMultiByte = "keyword".repeat(350);
    List<String> result = new ArrayList<>();

    underTest.addTokens(longMultiByte, result, false);

    assertThat(result, empty());
  }

  @Test
  void testAddTokens_partialTokenization() {
    StringBuilder phrase = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      phrase.append("token").append(i).append(".");
    }

    List<String> result = new ArrayList<>();
    underTest.addTokens(phrase.toString(), result, false);

    assertThat(result.size(), greaterThan(0));
    for (String entry : result) {
      int byteLength = entry.getBytes(StandardCharsets.UTF_8).length;
      assertThat("Entry exceeds byte limit: " + byteLength, byteLength, lessThanOrEqualTo(2046));
    }
  }

  @Test
  void testAddPathWithPostgreSQLEscaping() {
    underTest.addPath("path");
    underTest.addPath("PATH");
    underTest.addPath("test\\bar");
    underTest.addPath("te'st");

    assertThat(underTest.getPaths(), containsInAnyOrder("'path'", "'test\\\\bar'", "'te\\'st'"));
  }

  @Test
  void testAddPathWithH2NoEscaping() {
    SearchRecordData h2RecordData = new SearchRecordData(false);

    h2RecordData.addPath("path");
    h2RecordData.addPath("PATH");
    h2RecordData.addPath("test\\bar");
    h2RecordData.addPath("te'st");

    // only needs to lowercase the path because the escaping will be done by the Object mapper for JSON type
    // (ListHandlerType)
    assertThat(h2RecordData.getPaths(), containsInAnyOrder("path", "test\\bar", "te'st"));
  }

  private void assertTokens(final String token, final String... entries) {
    List<String> result = new ArrayList<>();

    underTest.addTokens(token, result, false);

    assertThat(result, contains(entries));
  }
}
