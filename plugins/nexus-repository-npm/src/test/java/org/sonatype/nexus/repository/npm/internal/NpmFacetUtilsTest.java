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
package org.sonatype.nexus.repository.npm.internal;

import java.util.Map;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.npm.internal.orient.NpmFacetUtils;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Response;
import org.sonatype.nexus.repository.view.payloads.StringPayload;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;
import static org.sonatype.nexus.repository.view.ContentTypes.APPLICATION_JSON;

public class NpmFacetUtilsTest
    extends TestSupport
{
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  Repository repository1, repository2, repository3;

  @Mock
  Response response1, response2, response3;

  @Test
  public void mergeDistTagResponse_singleEntry() throws Exception {
    when(response1.getPayload()).thenReturn(new Content(new StringPayload("{\"latest\":\"1.0.1\"}", APPLICATION_JSON)));

    final Response mergedResponse = NpmFacetUtils
        .mergeDistTagResponse(ImmutableMap.of(repository1, response1));

    final byte[] content = IOUtils.toByteArray(mergedResponse.getPayload().openInputStream());
    final Map<String, String> actual = objectMapper.readValue(content, new TypeReference<Map<String, String>>() { });
    assertThat(actual.get("latest"), containsString("1.0.1"));
  }

  @Test
  public void mergeDistTagResponse_multipleEntriesSingleTag() throws Exception {
    when(response1.getPayload()).thenReturn(new Content(new StringPayload("{\"latest\":\"1.0.0\"}", APPLICATION_JSON)));
    when(response2.getPayload()).thenReturn(new Content(new StringPayload("{\"latest\":\"1.0.1\"}", APPLICATION_JSON)));

    final Response mergedResponse = NpmFacetUtils
        .mergeDistTagResponse(ImmutableMap.of(repository1, response1, repository2, response2));

    final byte[] content = IOUtils.toByteArray(mergedResponse.getPayload().openInputStream());
    final Map<String, String> actual = objectMapper.readValue(content, new TypeReference<Map<String, String>>() { });
    assertThat(actual.get("latest"), containsString("1.0.1"));
  }

  @Test
  public void mergeDistTagResponse_reverseOrder() throws Exception {
    when(response1.getPayload()).thenReturn(new Content(new StringPayload("{\"latest\":\"1.0.1\"}", APPLICATION_JSON)));
    when(response2.getPayload()).thenReturn(new Content(new StringPayload("{\"latest\":\"1.0.0\"}", APPLICATION_JSON)));

    final Response mergedResponse = NpmFacetUtils
        .mergeDistTagResponse(ImmutableMap.of(repository1, response1, repository2, response2));

    final byte[] content = IOUtils.toByteArray(mergedResponse.getPayload().openInputStream());
    final Map<String, String> actual = objectMapper.readValue(content, new TypeReference<Map<String, String>>() { });
    assertThat(actual.get("latest"), containsString("1.0.1"));
  }

  @Test
  public void mergeDistTagResponse_multipleEntries() throws Exception {
    when(response1.getPayload()).thenReturn(new Content(
        new StringPayload("{\"latest\":\"1.0.2\",\"hello\":\"1.2.3\",\"world\":\"4.5.6\"}", APPLICATION_JSON)));
    when(response2.getPayload())
        .thenReturn(new Content(new StringPayload("{\"latest\":\"1.0.1\",\"world\":\"5.5.5\"}", APPLICATION_JSON)));
    when(response3.getPayload())
        .thenReturn(new Content(new StringPayload("{\"latest\":\"1.0.0\",\"hello\":\"2.2.2\"}", APPLICATION_JSON)));

    final Response mergedResponse = NpmFacetUtils
        .mergeDistTagResponse(ImmutableMap.of(repository1, response1, repository2, response2, repository3, response3));

    final byte[] content = IOUtils.toByteArray(mergedResponse.getPayload().openInputStream());
    final Map<String, String> actual = objectMapper.readValue(content, new TypeReference<Map<String, String>>() { });
    assertThat(actual.get("latest"), containsString("1.0.2"));
    assertThat(actual.get("hello"), containsString("2.2.2"));
    assertThat(actual.get("world"), containsString("5.5.5"));
  }
}
