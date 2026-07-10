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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

/**
 * REST API model for describing authentication for HTTP connections used by a proxy repository.
 *
 * @since 3.20
 */
public class HttpClientConnectionAuthenticationAttributes
{
  @Schema(description = "Authentication type")
  @NotEmpty
  protected final String type;

  @Schema
  protected final String username;

  @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
  protected final String password;

  @Schema
  protected final String ntlmHost;

  @Schema
  protected final String ntlmDomain;

  @Schema
  protected final String bearerToken;

  @JsonCreator
  public HttpClientConnectionAuthenticationAttributes(
      @JsonProperty("type") final String type,
      @JsonProperty("username") final String username,
      // NEXUS-46395: Access.WRITE_ONLY keeps Jackson from serializing this field on outbound
      // GET responses (e.g. repository configuration listings). The @Schema(accessMode=WRITE_ONLY)
      // annotation on the field is OpenAPI documentation only and does not influence Jackson;
      // the JsonProperty access mode is the actual gate.
      @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY) final String password,
      @JsonProperty("ntlmHost") final String ntlmHost,
      @JsonProperty("ntlmDomain") final String ntlmDomain,
      @JsonProperty(value = "bearerToken", access = JsonProperty.Access.WRITE_ONLY) final String bearerToken)
  {
    this.type = type;
    this.username = username;
    this.password = password;
    this.ntlmHost = ntlmHost;
    this.ntlmDomain = ntlmDomain;
    this.bearerToken = bearerToken;
  }

  public String getType() {
    return type;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getNtlmHost() {
    return ntlmHost;
  }

  public String getNtlmDomain() {
    return ntlmDomain;
  }

  public String getBearerToken() {
    return bearerToken;
  }

}
