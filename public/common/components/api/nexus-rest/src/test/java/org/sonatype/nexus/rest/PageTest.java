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
package org.sonatype.nexus.rest;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Jackson (de)serialization contract for {@link Page}.
 *
 * <p>
 * These tests guard against a regression where a second {@code @JsonProperty}-annotated
 * constructor was added: Jackson 2.12+ then sees two competing property-based creators and
 * fails type introspection with {@code IllegalArgumentException: Conflicting property-based
 * creators}. That failure is not local — it aborts Spring context refresh at Nexus startup
 * (surfaced via Swagger's model resolver) and shuts the whole application down. A simple
 * round-trip through {@link ObjectMapper} reproduces it, because introspection happens on the
 * first read/write of the type.
 */
public class PageTest
{
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void serializesWithTotalWhenPresent() throws Exception {
    Page<String> page = new Page<>(List.of("a", "b"), "token", 4888L);

    String json = mapper.writeValueAsString(page);

    assertThat(json, containsString("\"total\":4888"));
    assertThat(json, containsString("\"continuationToken\":\"token\""));
  }

  @Test
  public void omitsTotalWhenAbsent() throws Exception {
    Page<String> page = new Page<>(List.of("a"), "token");

    String json = mapper.writeValueAsString(page);

    assertThat(json, not(containsString("\"total\"")));
  }

  @Test
  public void deserializesFromJsonWithTotal() throws Exception {
    String json = "{\"items\":[\"a\",\"b\"],\"continuationToken\":\"token\",\"total\":4888}";

    Page<String> page = mapper.readValue(json, new TypeReference<Page<String>>()
    {
    });

    assertThat(page.getItems(), contains("a", "b"));
    assertThat(page.getContinuationToken(), is("token"));
    assertThat(page.getTotal(), is(4888L));
  }

  @Test
  public void deserializesFromJsonWithoutTotal() throws Exception {
    String json = "{\"items\":[\"a\"],\"continuationToken\":\"token\"}";

    Page<String> page = mapper.readValue(json, new TypeReference<Page<String>>()
    {
    });

    assertThat(page.getItems(), contains("a"));
    assertThat(page.getContinuationToken(), is("token"));
    assertThat(page.getTotal(), is(nullValue()));
  }

  @Test
  public void pagesDifferingOnlyInTotalAreNotEqual() {
    Page<String> withTotal = new Page<>(List.of("a", "b"), "token", 4888L);
    Page<String> withoutTotal = new Page<>(List.of("a", "b"), "token");

    assertThat(withTotal.equals(withoutTotal), is(false));
    assertThat(withoutTotal.equals(withTotal), is(false));
  }

  @Test
  public void pagesEqualInEveryFieldIncludingTotalAreEqual() {
    Page<String> one = new Page<>(List.of("a", "b"), "token", 4888L);
    Page<String> two = new Page<>(List.of("a", "b"), "token", 4888L);

    assertThat(one.equals(two), is(true));
    assertThat(one.hashCode(), is(two.hashCode()));
  }

  @Test
  public void pagesDifferingOnlyInTotalHaveDifferentHashCodes() {
    Page<String> withTotal = new Page<>(List.of("a", "b"), "token", 4888L);
    Page<String> withoutTotal = new Page<>(List.of("a", "b"), "token");

    assertThat(withTotal.hashCode() == withoutTotal.hashCode(), is(false));
  }

  @Test
  public void toStringIncludesTotal() {
    Page<String> page = new Page<>(List.of("a", "b"), "token", 4888L);

    assertThat(page.toString(), containsString("total=4888"));
  }
}
