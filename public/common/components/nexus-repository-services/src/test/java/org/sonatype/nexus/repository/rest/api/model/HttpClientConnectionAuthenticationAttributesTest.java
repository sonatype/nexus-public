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
package org.sonatype.nexus.repository.rest.api.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Locks in the credential-leak protection on outbound REST responses.
 *
 * <p>
 * NEXUS-46395 regression check: the OpenAPI 3.x sweep dropped
 * {@code @JsonProperty(access = Access.WRITE_ONLY)} from {@code password} and
 * {@code bearerToken}, leaving only {@code @Schema(accessMode = WRITE_ONLY)}, which is
 * OpenAPI documentation only. Without {@code @JsonProperty(access = WRITE_ONLY)} both
 * fields end up in JSON serialization output for any GET that returns this DTO (e.g.
 * repository configuration listings). These tests assert the field is actually absent
 * from the serialized form so the regression cannot reappear silently.
 */
public class HttpClientConnectionAuthenticationAttributesTest
{
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void passwordIsNotSerialized() throws Exception {
    HttpClientConnectionAuthenticationAttributes attrs = new HttpClientConnectionAuthenticationAttributes(
        "username", "alice", "supersecret", null, null, null);
    String json = mapper.writeValueAsString(attrs);

    assertThat(json, not(containsString("supersecret")));
    assertThat(json, not(containsString("\"password\"")));
  }

  @Test
  public void bearerTokenIsNotSerialized() throws Exception {
    HttpClientConnectionAuthenticationAttributes attrs = new HttpClientConnectionAuthenticationAttributes(
        "bearer", null, null, null, null, "tok-abc-123");
    String json = mapper.writeValueAsString(attrs);

    assertThat(json, not(containsString("tok-abc-123")));
    assertThat(json, not(containsString("\"bearerToken\"")));
  }

  @Test
  public void passwordIsAcceptedOnDeserialization() throws Exception {
    String json = "{\"type\":\"username\",\"username\":\"alice\",\"password\":\"supersecret\"}";
    HttpClientConnectionAuthenticationAttributes attrs =
        mapper.readValue(json, HttpClientConnectionAuthenticationAttributes.class);

    assertThat(attrs.getPassword(), is(equalTo("supersecret")));
    assertThat(attrs.getUsername(), is(equalTo("alice")));
  }

  @Test
  public void bearerTokenIsAcceptedOnDeserialization() throws Exception {
    String json = "{\"type\":\"bearer\",\"bearerToken\":\"tok-abc-123\"}";
    HttpClientConnectionAuthenticationAttributes attrs =
        mapper.readValue(json, HttpClientConnectionAuthenticationAttributes.class);

    assertThat(attrs.getBearerToken(), is(equalTo("tok-abc-123")));
  }
}
