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
package org.sonatype.nexus.repository.json;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;

import org.junit.Before;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class MergeObjectMapperTest
    extends TestSupport
{
  private MergeObjectMapper underTest;

  @Before
  public void setUp() {
    underTest = new MergeObjectMapper();
  }

  @Test
  public void read_EmptyJson() throws IOException {
    InputStream inputStream = new ByteArrayInputStream("{}".getBytes());
    assertThat(underTest.read(inputStream).size(), is(0));
  }

  @Test
  public void read_Json() throws IOException {
    try (InputStream inputStream = getClass().getResourceAsStream("merge-multi-depth-first.json")) {
      verifyReadFirstJson(underTest.read(inputStream));
    }
  }

  @Test
  public void merge_Multiple_InputStreams_Into_Map() throws IOException {
    verifyMergeMultipleContents(this::mergeInputStreamsWhileStreaming);
  }

  @Test
  public void merging_Multiple_InputStreams_WithMultiDepthJson() throws IOException {
    verifyMergingMultipleContentsWithMultiDepthJson(this::mergeInputStreamsWhileStreaming);
  }

  private void verifyMergeMultipleContents(Function<List<InputStream>, NestedAttributesMap> function)
      throws IOException
  {
    try (InputStream recessive = getClass().getResourceAsStream("merge-streaming-payload-recessive.json");
         InputStream dominant = getClass().getResourceAsStream("merge-streaming-payload-dominant.json")) {
      verifyMergeMultipleContentsResult(function.apply(asList(recessive, dominant)));
    }
  }

  private void verifyReadFirstJson(final NestedAttributesMap result) {
    assertThat(result.size(), equalTo(7));
    assertThat(result.get("name"), equalTo("first"));

    List maintainers = (List) result.get("maintainers");
    assertThat(((Map) maintainers.get(0)).get("email"), equalTo("first@example.com"));
    assertThat(((Map) maintainers.get(0)).get("name"), equalTo("first"));
    assertThat(((Map) maintainers.get(1)).get("email"), equalTo("first2@example.com"));
    assertThat(((Map) maintainers.get(1)).get("name"), equalTo("first2"));

    List keywords = (List) result.get("keywords");
    assertThat(keywords.get(0), equalTo("array"));
    assertThat(keywords.get(1), equalTo("first"));

    NestedAttributesMap mulitDepth = result.child("multi-depth");
    NestedAttributesMap first = mulitDepth.child("first");

    assertThat(first.get("title"), equalTo("This is the first depth"));

    NestedAttributesMap reverseMulitDepth = result.child("reverse-multi-depth");
    first = reverseMulitDepth.child("first");
    NestedAttributesMap second = first.child("second");
    NestedAttributesMap third = second.child("third");

    assertThat(first.get("title"), equalTo("This is the first depth"));
    assertThat(second.get("title"), equalTo("This is the second depth"));
    assertThat(third.get("title"), equalTo("This is the third depth"));

    NestedAttributesMap mulitDepthMerge = result.child("multi-depth-merge");
    first = mulitDepthMerge.child("first");
    assertThat(first.get("title"), equalTo("This is the first depth"));

    NestedAttributesMap dependencies = result.child("dependencies");
    assertThat(dependencies.get("equire(\"orchestrator\""), equalTo("*"));
    assertThat(dependencies.get("@types/node"), equalTo("*"));
    assertThat(dependencies.get("@types/orchestrator"), equalTo("*"));

    NestedAttributesMap funckyFieldName = dependencies.child("funcky(\"fieldname\"");
    NestedAttributesMap godeep = funckyFieldName.child("godeep");
    first = godeep.child("first");
    assertThat(first.get("title"), equalTo("This is the first depth"));
  }

  @SuppressWarnings("unchecked")
  private void verifyMergeMultipleContentsResult(final NestedAttributesMap result) {
    assertThat(result.backing(), hasEntry("_id", "id"));
    assertThat(result.backing(), hasEntry("_rev", "rev"));
    assertThat(result.backing(), hasEntry("name", "dominant"));
    assertThat(result.child("dist-tags").get("latest"), equalTo("1.0.0"));
    assertThat(result.child("versions").child("1.0").backing(), hasEntry("foo", "baz"));
    assertThat(result.child("versions").child("1.0").backing(), hasEntry("foo2", "bar2"));

    assertThat(result.backing(), hasEntry("notindominant", "always present"));
    assertThat(result.backing(), hasEntry("notinrecessive", "always present"));
    assertThat(result.child("not").child("in").backing(), hasEntry("dominant", "always present"));

    assertThat((List<String>) result.child("languages").child("en").get("alphabet"), contains("a", "b", "c"));

    assertThat((List<String>) result.get("alphabet"), contains("a", "b", "c"));
    assertThat((List<String>) result.get("recessive-numbers"), contains(1, 2, 3));
    assertThat((List<String>) result.get("dominant-numbers"), contains(9, 8, 7));

    Map<String, String> map = ((List<Map<String, String>>) result.get("maintainers")).get(0);
    assertThat(map, hasEntry("name", "j1"));
    assertThat(map, hasEntry("email", "j1@sonatype.com"));

    map = ((List<Map<String, String>>) result.get("maintainers")).get(1);
    assertThat(map, hasEntry("name", "j2"));
    assertThat(map, hasEntry("email", "j2@sonatype.com"));

    map = ((List<Map<String, String>>) result.get("recessive-maintainers")).get(0);
    assertThat(map, hasEntry("name", "jeremy"));
    assertThat(map, hasEntry("email", "jeremy@sonatype.com"));

    map = ((List<Map<String, String>>) result.get("dominant-maintainers")).get(0);
    assertThat(map, hasEntry("name", "nate"));
    assertThat(map, hasEntry("email", "nate@sonatype.com"));

    List<String> list = ((List<List<String>>) result.get("array-of-arrays")).get(0);
    assertThat(list, contains("ab", "cd", "ef"));

    list = ((List<List<String>>) result.get("array-of-arrays")).get(1);
    assertThat(list, contains("uv", "wx", "yz"));

    list = ((List<List<String>>) result.get("recessive-array-of-arrays")).get(0);
    assertThat(list, contains("gh", "ij", "kl"));

    list = ((List<List<String>>) result.get("recessive-array-of-arrays")).get(1);
    assertThat(list, contains("op", "qr", "st"));

    list = ((List<List<String>>) result.get("dominant-array-of-arrays")).get(0);
    assertThat(list, contains("ab", "cd", "ef"));

    list = ((List<List<String>>) result.get("dominant-array-of-arrays")).get(1);
    assertThat(list, contains("uv", "wx", "yz"));

    assertThat((List<Object>) result.get("null-array"), contains(nullValue(), nullValue(), nullValue()));

    assertThat((List<String>) result.get("alphabet"), contains("a", "b", "c"));

    List<Object> objects = (List<Object>) result.get("mixed-array");
    assertThat(objects.get(0), equalTo("circle"));
    assertThat(objects.get(1), equalTo(3.14));
    assertThat(((Map<String, String>) objects.get(2)), hasEntry("color", "blue"));
  }

  private void verifyMergingMultipleContentsWithMultiDepthJson(Function<List<InputStream>, NestedAttributesMap> function)
      throws IOException
  {
    try (InputStream inputStream1 = getClass().getResourceAsStream("merge-multi-depth-first.json");
         InputStream inputStream2 = getClass().getResourceAsStream("merge-multi-depth-second.json");
         InputStream inputStream3 = getClass().getResourceAsStream("merge-multi-depth-third.json")) {
      verifyMergingMultipleContentsWithMultiDepthJsonResult(
          function.apply(asList(inputStream1, inputStream2, inputStream3)));
    }
  }

  private void verifyMergingMultipleContentsWithMultiDepthJsonResult(final NestedAttributesMap result) {
    assertThat(result.size(), equalTo(7));
    assertThat(result.get("name"), equalTo("third"));

    List maintainers = (List) result.get("maintainers");
    assertThat(((Map) maintainers.get(0)).get("email"), equalTo("third@example.com"));
    assertThat(((Map) maintainers.get(0)).get("name"), equalTo("third"));

    List keywords = (List) result.get("keywords");
    assertThat(keywords.get(0), equalTo("array"));
    assertThat(keywords.get(1), equalTo("third"));

    NestedAttributesMap mulitDepth = result.child("multi-depth");
    NestedAttributesMap first = mulitDepth.child("first");
    NestedAttributesMap second = first.child("second");
    NestedAttributesMap third = second.child("third");

    assertThat(first.get("title"), equalTo("This is the first depth"));
    assertThat(second.get("title"), equalTo("This is the second depth"));
    assertThat(third.get("title"), equalTo("This is the third depth"));

    NestedAttributesMap reverseMulitDepth = result.child("reverse-multi-depth");
    first = reverseMulitDepth.child("first");
    assertThat(first.get("title"), equalTo("This is the first depth"));

    NestedAttributesMap mulitDepthMerge = result.child("multi-depth-merge");
    first = mulitDepthMerge.child("first");

    assertThat(first.get("summary"), equalTo("We have here the first depth from the third response"));

    NestedAttributesMap dependencies = result.child("dependencies");
    assertThat(dependencies.get("equire(\"orchestrator\""), equalTo("*"));
    assertThat(dependencies.get("@types/node"), equalTo("*"));
    assertThat(dependencies.get("@types/orchestrator"), equalTo("*"));

    NestedAttributesMap funckyFieldName = dependencies.child("funcky(\"fieldname\"");
    assertThat(funckyFieldName.get("test"), equalTo("value"));
  }

  private NestedAttributesMap mergeInputStreamsWhileStreaming(final List<InputStream> inputStreams) {
    try {
      return underTest.merge(inputStreams);
    }
    catch (IOException e) {
      fail(e.getMessage());
    }

    return null;
  }
}
