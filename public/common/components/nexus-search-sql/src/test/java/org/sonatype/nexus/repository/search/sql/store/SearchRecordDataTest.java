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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.search.sql.SearchAssetRecord;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

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
    for (String entry : result) {
      int byteLength = entry.getBytes(StandardCharsets.UTF_8).length;
      assertThat("Entry exceeds byte limit: " + byteLength, byteLength, lessThanOrEqualTo(2046));
    }
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

  // ── Uploaders ────────────────────────────────────────────────────────────

  @Test
  void testAddUploader() {
    underTest.addUploader("admin");
    assertThat(underTest.getUploaders(), containsInAnyOrder("'admin'"));
  }

  @Test
  void testAddUploader_emptyCases() {
    underTest.addUploader(null);
    underTest.addUploader("");
    underTest.addUploader("   ");
    assertThat(underTest.getUploaders(), empty());
  }

  @Test
  void testAddUploaderIp() {
    underTest.addUploaderIp("192.168.1.1");
    assertThat(underTest.getUploaderIps(), containsInAnyOrder("'192.168.1.1'"));
  }

  @Test
  void testAddUploaderIp_emptyCases() {
    underTest.addUploaderIp(null);
    underTest.addUploaderIp("");
    underTest.addUploaderIp("   ");
    assertThat(underTest.getUploaderIps(), empty());
  }

  // ── Hash fields ──────────────────────────────────────────────────────────

  @Test
  void testAddMd5() {
    underTest.addMd5("abc123");
    assertThat(underTest.getMd5(), containsInAnyOrder("abc123"));
  }

  @Test
  void testAddMd5_emptyCases() {
    underTest.addMd5(null);
    underTest.addMd5("");
    underTest.addMd5("   ");
    assertThat(underTest.getMd5(), empty());
  }

  @Test
  void testAddSha1() {
    underTest.addSha1("sha1value");
    assertThat(underTest.getSha1(), containsInAnyOrder("sha1value"));
  }

  @Test
  void testAddSha1_emptyCases() {
    underTest.addSha1(null);
    underTest.addSha1("");
    underTest.addSha1("   ");
    assertThat(underTest.getSha1(), empty());
  }

  @Test
  void testAddSha256() {
    underTest.addSha256("sha256value");
    assertThat(underTest.getSha256(), containsInAnyOrder("sha256value"));
  }

  @Test
  void testAddSha256_emptyCases() {
    underTest.addSha256(null);
    underTest.addSha256("");
    assertThat(underTest.getSha256(), empty());
  }

  @Test
  void testAddSha512() {
    underTest.addSha512("sha512value");
    assertThat(underTest.getSha512(), containsInAnyOrder("sha512value"));
  }

  @Test
  void testAddSha512_emptyCases() {
    underTest.addSha512(null);
    underTest.addSha512("");
    assertThat(underTest.getSha512(), empty());
  }

  // ── Tags ─────────────────────────────────────────────────────────────────

  @Test
  void testSetTags() {
    underTest.setTags(List.of("release", "stable"));
    assertThat(underTest.getTags(), containsInAnyOrder("release", "stable"));
  }

  @Test
  void testSetTags_empty() {
    underTest.setTags(List.of());
    assertThat(underTest.getTags(), empty());
  }

  // ── Setters / getters ────────────────────────────────────────────────────

  @Test
  void testRepositoryIdSetterGetter() {
    underTest.setRepositoryId(42);
    assertThat(underTest.getRepositoryId(), is(42));
  }

  @Test
  void testComponentIdSetterGetter() {
    underTest.setComponentId(99);
    assertThat(underTest.getComponentId(), is(99));
  }

  @Test
  void testFormatSetterGetter() {
    underTest.setFormat("maven2");
    assertThat(underTest.getFormat(), is("maven2"));
  }

  @Test
  void testNamespaceSetterGetter() {
    underTest.setNamespace("com.example");
    assertThat(underTest.getNamespace(), is("com.example"));
  }

  @Test
  void testComponentNameSetterGetter() {
    underTest.setComponentName("my-component");
    assertThat(underTest.getComponentName(), is("my-component"));
  }

  @Test
  void testAliasComponentName() {
    underTest.addAliasComponentName("alias1");
    assertThat(underTest.getAliasComponentNames(), containsInAnyOrder("'alias1'"));
  }

  @Test
  void testAliasComponentName_emptyCases() {
    underTest.addAliasComponentName(null);
    underTest.addAliasComponentName("");
    underTest.addAliasComponentName("   ");
    assertThat(underTest.getAliasComponentNames(), empty());
  }

  @Test
  void testComponentKindSetterGetter() {
    underTest.setComponentKind("binary");
    assertThat(underTest.getComponentKind(), is("binary"));
  }

  @Test
  void testVersionSetterGetter() {
    underTest.setVersion("1.0.0");
    assertThat(underTest.getVersion(), is("1.0.0"));
  }

  @Test
  void testNormalisedVersionSetterGetter() {
    underTest.setNormalisedVersion("000001.000000.000000");
    assertThat(underTest.getNormalisedVersion(), is("000001.000000.000000"));
  }

  @Test
  void testLastModifiedSetterGetter() {
    OffsetDateTime now = OffsetDateTime.now();
    underTest.setLastModified(now);
    assertThat(underTest.getLastModified(), is(now));
  }

  @Test
  void testRepositoryNameSetterGetter() {
    underTest.setRepositoryName("maven-releases");
    assertThat(underTest.getRepositoryName(), is("maven-releases"));
  }

  @Test
  void testPrereleaseSetterGetter() {
    underTest.setPrerelease(true);
    assertThat(underTest.isPrerelease(), is(true));
    underTest.setPrerelease(false);
    assertThat(underTest.isPrerelease(), is(false));
  }

  @Test
  void testEntityVersionSetterGetter() {
    underTest.setEntityVersion(7);
    assertThat(underTest.getEntityVersion(), is(7));
  }

  @Test
  void testAttributesSetterGetter() {
    NestedAttributesMap attrs = new NestedAttributesMap("attributes", new HashMap<>());
    attrs.set("key", "value");
    underTest.setAttributes(attrs);
    assertThat(underTest.attributes(), notNullValue());
    assertThat(underTest.attributes().get("key"), is("value"));
  }

  // ── newAssetRecord / addSearchAssetRecord ─────────────────────────────────

  @Test
  void testNewAssetRecord() {
    underTest.setRepositoryId(1);
    underTest.setComponentId(2);
    underTest.setFormat("maven2");
    SearchAssetRecord assetRecord = underTest.newAssetRecord();
    assertThat(assetRecord, notNullValue());
    assertThat(underTest.getSearchAssetRecords(), hasSize(1));
  }

  @Test
  void testAddSearchAssetRecord_nonNull() {
    // Directly calls addSearchAssetRecord (non-null path) — distinct from testNewAssetRecord
    SearchRecordData other = new SearchRecordData(1, 2, "maven2");
    SearchAssetRecord record = other.newAssetRecord();
    underTest.addSearchAssetRecord(record);
    assertThat(underTest.getSearchAssetRecords(), hasSize(1));
    assertThat(underTest.getSearchAssetRecords().iterator().next(), is(record));
  }

  @Test
  void testAddSearchAssetRecord_null_ignored() {
    underTest.addSearchAssetRecord(null);
    assertThat(underTest.getSearchAssetRecords(), empty());
  }

  // ── Constructors ─────────────────────────────────────────────────────────

  @Test
  void testConstructorWithRepositoryIdAndFormat() {
    SearchRecordData data = new SearchRecordData(10, "npm");
    assertThat(data.getRepositoryId(), is(10));
    assertThat(data.getFormat(), is("npm"));
    // componentId is not set by this constructor
    assertThat(data.getComponentId(), nullValue());
  }

  @Test
  void testConstructorWithRepositoryIdComponentIdAndFormat() {
    SearchRecordData data = new SearchRecordData(10, 20, "npm");
    assertThat(data.getRepositoryId(), is(10));
    assertThat(data.getComponentId(), is(20));
    assertThat(data.getFormat(), is("npm"));
  }

  // ── Path byte limit ───────────────────────────────────────────────────────

  @Test
  void testAddPath_byteLimitExceeded_pathNotAdded() {
    String largePath = "/" + "a".repeat(SearchRecordData.MAX_TSVECTOR_BYTES - 10);
    underTest.addPath(largePath);
    int countAfterFirst = underTest.getPaths().size();

    underTest.addPath("/another/path");
    assertThat(underTest.getPaths().size(), is(countAfterFirst));
  }

  // ── equals / hashCode / toString ─────────────────────────────────────────

  @Test
  void testEquals_sameObject() {
    assertThat(underTest, equalTo(underTest));
  }

  @Test
  void testEquals_twoIdenticallyConstructedObjects() {
    // NestedAttributesMap uses reference equality, so share the same instance to get
    // a meaningful test of all other fields being equal.
    SearchRecordData a = new SearchRecordData(1, 2, "maven2");
    SearchRecordData b = new SearchRecordData(1, 2, "maven2");
    b.setAttributes(a.attributes());
    assertThat(a, equalTo(b));
  }

  @Test
  void testEquals_differentRepositoryId() {
    SearchRecordData other = new SearchRecordData();
    underTest.setRepositoryId(1);
    other.setRepositoryId(2);
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentComponentId() {
    SearchRecordData other = new SearchRecordData();
    underTest.setComponentId(1);
    other.setComponentId(2);
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormat() {
    SearchRecordData other = new SearchRecordData();
    underTest.setFormat("maven2");
    other.setFormat("npm");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentNamespace() {
    SearchRecordData other = new SearchRecordData();
    underTest.setNamespace("com.example");
    other.setNamespace("org.other");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentNamespaceNames() {
    SearchRecordData other = new SearchRecordData();
    underTest.addNamespaceNames("com.example");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentComponentName() {
    SearchRecordData other = new SearchRecordData();
    underTest.setComponentName("my-component");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentAliasComponentNames() {
    SearchRecordData other = new SearchRecordData();
    underTest.addAliasComponentName("alias");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentComponentKind() {
    SearchRecordData other = new SearchRecordData();
    underTest.setComponentKind("binary");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentVersion() {
    SearchRecordData other = new SearchRecordData();
    underTest.setVersion("1.0.0");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentVersionNames() {
    SearchRecordData other = new SearchRecordData();
    underTest.addVersionNames("1.0.0");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentNormalisedVersion() {
    SearchRecordData other = new SearchRecordData();
    underTest.setNormalisedVersion("000001.000000.000000");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentLastModified() {
    SearchRecordData other = new SearchRecordData();
    underTest.setLastModified(OffsetDateTime.now());
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentRepositoryName() {
    SearchRecordData other = new SearchRecordData();
    underTest.setRepositoryName("maven-releases");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentPrerelease() {
    SearchRecordData other = new SearchRecordData();
    underTest.setPrerelease(true);
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentUploaders() {
    SearchRecordData other = new SearchRecordData();
    underTest.addUploader("admin");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentUploaderIps() {
    SearchRecordData other = new SearchRecordData();
    underTest.addUploaderIp("127.0.0.1");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentPaths() {
    SearchRecordData other = new SearchRecordData();
    underTest.addPath("/org/example");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentKeywords() {
    SearchRecordData other = new SearchRecordData();
    underTest.addKeyword("test");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentMd5() {
    SearchRecordData other = new SearchRecordData();
    underTest.addMd5("abc123");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentSha1() {
    SearchRecordData other = new SearchRecordData();
    underTest.addSha1("sha1val");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentSha256() {
    SearchRecordData other = new SearchRecordData();
    underTest.addSha256("sha256val");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentSha512() {
    SearchRecordData other = new SearchRecordData();
    underTest.addSha512("sha512val");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentEntityVersion() {
    SearchRecordData other = new SearchRecordData();
    underTest.setEntityVersion(5);
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentTags() {
    SearchRecordData other = new SearchRecordData();
    underTest.setTags(List.of("release"));
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormatFieldValues1() {
    SearchRecordData other = new SearchRecordData();
    underTest.addFormatFieldValue1("value1", true);
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormatFieldValues2() {
    SearchRecordData other = new SearchRecordData();
    underTest.addFormatFieldValue2("value2");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormatFieldValues3() {
    SearchRecordData other = new SearchRecordData();
    underTest.addFormatFieldValue3("value3");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormatFieldValues4() {
    SearchRecordData other = new SearchRecordData();
    underTest.addFormatFieldValue4("value4", true);
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormatFieldValues5() {
    SearchRecordData other = new SearchRecordData();
    underTest.addFormatFieldValue5("value5");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormatFieldValues6() {
    SearchRecordData other = new SearchRecordData();
    underTest.addFormatFieldValue6("value6", true);
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_differentFormatFieldValues7() {
    SearchRecordData other = new SearchRecordData();
    underTest.addFormatFieldValue7("value7");
    assertThat(underTest, not(equalTo(other)));
  }

  @Test
  void testEquals_nullAndOtherType() {
    assertThat(underTest, not(equalTo(null)));
    assertThat(underTest, not(equalTo("not a SearchRecordData")));
  }

  @Test
  void testHashCode_stableAcrossCallsOnSameObject() {
    assertThat(underTest.hashCode(), is(underTest.hashCode()));
  }

  @Test
  void testHashCode_changesWhenFieldChanges() {
    int before = underTest.hashCode();
    underTest.setRepositoryId(1);
    assertThat(underTest.hashCode(), is(not(before)));
  }

  @Test
  void testToString_notNull() {
    underTest.setRepositoryId(1);
    underTest.setComponentId(2);
    underTest.setFormat("maven2");
    assertThat(underTest.toString(), notNullValue());
  }

  private void assertTokens(final String token, final String... entries) {
    List<String> result = new ArrayList<>();

    underTest.addTokens(token, result, false);

    assertThat(result, contains(entries));
  }
}
