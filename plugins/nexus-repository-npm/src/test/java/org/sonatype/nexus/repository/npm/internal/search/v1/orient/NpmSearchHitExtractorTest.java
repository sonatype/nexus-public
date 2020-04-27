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
package org.sonatype.nexus.repository.npm.internal.search.v1.orient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.npm.internal.search.v1.orient.NpmSearchHitExtractor;

import org.elasticsearch.search.SearchHit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class NpmSearchHitExtractorTest
    extends TestSupport
{
  @Mock
  SearchHit searchHit;

  Map<String, Object> formatAttributes;

  Map<String, Object> contentAttributes;

  NpmSearchHitExtractor underTest = new NpmSearchHitExtractor();

  @Before
  public void setUp() {
    formatAttributes = new LinkedHashMap<>();
    contentAttributes = new LinkedHashMap<>();

    Map<String, Object> entityAttributes = new LinkedHashMap<>();
    entityAttributes.put("npm", formatAttributes);
    entityAttributes.put("content", contentAttributes);

    Map<String, Object> asset = new LinkedHashMap<>();
    asset.put("attributes", entityAttributes);

    List<Map<String, Object>> assets = new ArrayList<>();
    assets.add(asset);

    Map<String, Object> source = new LinkedHashMap<>();
    source.put("assets", assets);

    when(searchHit.getSource()).thenReturn(source);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testWithEmptyAssetList() {
    Map<String, Object> source = searchHit.getSource();
    List<Map<String, Object>> assets = (List<Map<String, Object>>) source.get("assets");
    assets.clear();

    assertThat(underTest.extractName(searchHit), is(nullValue()));
    assertThat(underTest.extractVersion(searchHit), is(nullValue()));
    assertThat(underTest.extractDescription(searchHit), is(nullValue()));
    assertThat(underTest.extractHomepage(searchHit), is(nullValue()));
    assertThat(underTest.extractRepositoryUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractBugsUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
    assertThat(underTest.extractLastModified(searchHit), is(nullValue()));
    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }

  @Test
  public void testWithoutAssetList() {
    Map<String, Object> source = searchHit.getSource();
    source.remove("assets");

    assertThat(underTest.extractName(searchHit), is(nullValue()));
    assertThat(underTest.extractVersion(searchHit), is(nullValue()));
    assertThat(underTest.extractDescription(searchHit), is(nullValue()));
    assertThat(underTest.extractHomepage(searchHit), is(nullValue()));
    assertThat(underTest.extractRepositoryUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractBugsUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
    assertThat(underTest.extractLastModified(searchHit), is(nullValue()));
    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testWithoutEntityAttributes() {
    Map<String, Object> source = searchHit.getSource();
    List<Map<String, Object>> assets = (List<Map<String, Object>>) source.get("assets");
    Map<String, Object> asset = assets.get(0);
    asset.remove("attributes");

    assertThat(underTest.extractName(searchHit), is(nullValue()));
    assertThat(underTest.extractVersion(searchHit), is(nullValue()));
    assertThat(underTest.extractDescription(searchHit), is(nullValue()));
    assertThat(underTest.extractHomepage(searchHit), is(nullValue()));
    assertThat(underTest.extractRepositoryUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractBugsUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
    assertThat(underTest.extractLastModified(searchHit), is(nullValue()));
    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testWithoutFormatAttributes() {
    Map<String, Object> source = searchHit.getSource();
    List<Map<String, Object>> assets = (List<Map<String, Object>>) source.get("assets");
    Map<String, Object> asset = assets.get(0);
    Map<String, Object> entityAttributes = (Map<String, Object>) asset.get("attributes");
    entityAttributes.remove("npm");

    assertThat(underTest.extractName(searchHit), is(nullValue()));
    assertThat(underTest.extractVersion(searchHit), is(nullValue()));
    assertThat(underTest.extractDescription(searchHit), is(nullValue()));
    assertThat(underTest.extractHomepage(searchHit), is(nullValue()));
    assertThat(underTest.extractRepositoryUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractBugsUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
    assertThat(underTest.extractLastModified(searchHit), is(nullValue()));
    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testWithoutContentAttributes() {
    Map<String, Object> source = searchHit.getSource();
    List<Map<String, Object>> assets = (List<Map<String, Object>>) source.get("assets");
    Map<String, Object> asset = assets.get(0);
    Map<String, Object> entityAttributes = (Map<String, Object>) asset.get("attributes");
    entityAttributes.remove("content");

    assertThat(underTest.extractName(searchHit), is(nullValue()));
    assertThat(underTest.extractVersion(searchHit), is(nullValue()));
    assertThat(underTest.extractDescription(searchHit), is(nullValue()));
    assertThat(underTest.extractHomepage(searchHit), is(nullValue()));
    assertThat(underTest.extractRepositoryUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractBugsUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
    assertThat(underTest.extractLastModified(searchHit), is(nullValue()));
    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }

  @Test
  public void testExtractFieldsPresent() {
    formatAttributes.put("name", "foo");
    formatAttributes.put("version", "1.2.3");
    formatAttributes.put("description", "A simple test.");
    formatAttributes.put("homepage", "http://www.example.com/foo");
    formatAttributes.put("repository_url", "http://www.example.com/foo.git");
    formatAttributes.put("bugs_url", "http://www.example.com/foo/bugs");
    formatAttributes.put("author", "Foo Bar <foo@example.com> (http://www.example.com/foo)");
    formatAttributes.put("keywords", "keyword1 keyword2 keyword3");

    contentAttributes.put("last_modified", 300499200L);

    assertThat(underTest.extractName(searchHit), is("foo"));
    assertThat(underTest.extractVersion(searchHit), is("1.2.3"));
    assertThat(underTest.extractDescription(searchHit), is("A simple test."));
    assertThat(underTest.extractHomepage(searchHit), is("http://www.example.com/foo"));
    assertThat(underTest.extractRepositoryUrl(searchHit), is("http://www.example.com/foo.git"));
    assertThat(underTest.extractBugsUrl(searchHit), is("http://www.example.com/foo/bugs"));
    assertThat(underTest.extractAuthorName(searchHit), is("Foo Bar"));
    assertThat(underTest.extractAuthorEmail(searchHit), is("foo@example.com"));
    assertThat(underTest.extractLastModified(searchHit), is(new DateTime(300499200L)));
    assertThat(underTest.extractKeywords(searchHit), containsInAnyOrder("keyword1", "keyword2", "keyword3"));
  }

  @Test
  public void testExtractFieldsAbsent() {
    assertThat(underTest.extractName(searchHit), is(nullValue()));
    assertThat(underTest.extractVersion(searchHit), is(nullValue()));
    assertThat(underTest.extractDescription(searchHit), is(nullValue()));
    assertThat(underTest.extractHomepage(searchHit), is(nullValue()));
    assertThat(underTest.extractRepositoryUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractBugsUrl(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
    assertThat(underTest.extractLastModified(searchHit), is(nullValue()));
    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }

  @Test
  public void testAuthorExtractionWhenNameAndEmailPresent() {
    formatAttributes.put("author", "Foo Bar <foo@example.com>");

    assertThat(underTest.extractAuthorName(searchHit), is("Foo Bar"));
    assertThat(underTest.extractAuthorEmail(searchHit), is("foo@example.com"));
  }

  @Test
  public void testAuthorExtractionWhenNamePresent() {
    formatAttributes.put("author", "Foo Bar");

    assertThat(underTest.extractAuthorName(searchHit), is("Foo Bar"));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
  }

  @Test
  public void testAuthorExtractionWhenNameAndUrlPresent() {
    formatAttributes.put("author", "Foo Bar (http://www.example.com/foo)");

    assertThat(underTest.extractAuthorName(searchHit), is("Foo Bar"));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
  }

  @Test
  public void testAuthorExtractionWhenEmailPresent() {
    formatAttributes.put("author", "<foo@example.com>");

    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is("foo@example.com"));
  }

  @Test
  public void testAuthorExtractionWhenEmailIncorrect() {
    formatAttributes.put("author", "<foo@example.com");

    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
  }

  @Test
  public void testAuthorExtractionWhenEmailBlank() {
    formatAttributes.put("author", "<  >");

    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
  }

  @Test
  public void testAuthorExtractionWhenEmailAndUrlPresent() {
    formatAttributes.put("author", "<foo@example.com> (http://www.example.com/foo)");

    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is("foo@example.com"));
  }

  @Test
  public void testAuthorExtractionWhenUrlPresent() {
    formatAttributes.put("author", "(http://www.example.com/foo)");

    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
  }

  @Test
  public void testAuthorExtractionWhenEmpty() {
    formatAttributes.put("author", "");

    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
  }

  @Test
  public void testAuthorExtractionWhenNull() {
    assertThat(underTest.extractAuthorName(searchHit), is(nullValue()));
    assertThat(underTest.extractAuthorEmail(searchHit), is(nullValue()));
  }

  @Test
  public void testLastModifiedExtractionWhenNull() {
    assertThat(underTest.extractLastModified(searchHit), is(nullValue()));
  }

  @Test
  public void testKeywordsExtractionWhenSinglePresent() {
    formatAttributes.put("keywords", "keyword1");

    assertThat(underTest.extractKeywords(searchHit), containsInAnyOrder("keyword1"));
  }

  @Test
  public void testKeywordsExtractionWhenEmpty() {
    formatAttributes.put("keywords", "");

    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }

  @Test
  public void testKeywordsExtractionWhenNull() {
    assertThat(underTest.extractKeywords(searchHit), is(empty()));
  }
}
