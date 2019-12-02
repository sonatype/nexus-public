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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * REST API model for describing authentication for HTTP connections used by a proxy repository.
 *
 * @since 3.20
 */
@JsonIgnoreProperties(value = {"password"}, allowSetters = true)
public class HttpClientConnectionAuthenticationAttributes
{
  @ApiModelProperty(value = "Authentication type", allowableValues = "username,ntlm")
  @NotEmpty
  protected final String type;

  @ApiModelProperty
  protected final String username;

  @ApiModelProperty
  protected final String password;

  @ApiModelProperty
  protected final String ntlmHost;

  @ApiModelProperty
  protected final String ntlmDomain;

  @JsonCreator
  public HttpClientConnectionAuthenticationAttributes(
      @JsonProperty("type") final String type,
      @JsonProperty("username") final String username,
      @JsonProperty("password") final String password,
      @JsonProperty("ntlmHost") final String ntlmHost,
      @JsonProperty("ntlmDomain") final String ntlmDomain)
  {
    this.type = type;
    this.username = username;
    this.password = password;
    this.ntlmHost = ntlmHost;
    this.ntlmDomain = ntlmDomain;
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
}
