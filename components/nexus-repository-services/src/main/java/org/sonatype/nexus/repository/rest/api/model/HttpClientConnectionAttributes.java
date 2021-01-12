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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model specifying the HTTP connection used by a proxy repository.
 *
 * @since 3.20
 */
public class HttpClientConnectionAttributes
{
  @ApiModelProperty(value = "Total retries if the initial connection attempt suffers a timeout", example = "0",
      allowableValues = "range[0,10]")
  @Min(0L)
  @Max(10L)
  protected final Integer retries;

  @ApiModelProperty(value = "Custom fragment to append to User-Agent header in HTTP requests", example = "")
  protected final String userAgentSuffix;

  @ApiModelProperty(value = "Seconds to wait for activity before stopping and retrying the connection", example = "60",
      allowableValues = "range[1,3600]")
  @Min(1L)
  @Max(3600L)
  protected final Integer timeout;

  @ApiModelProperty(value = "Whether to enable redirects to the same location (may be required by some servers)",
      example = "false")
  protected final Boolean enableCircularRedirects;

  @ApiModelProperty(value = "Whether to allow cookies to be stored and used", example = "false")
  protected final Boolean enableCookies;

  @ApiModelProperty(value = "Use certificates stored in the Nexus Repository Manager truststore to connect to external systems",
      example = "false")
  protected final Boolean useTrustStore;

  @JsonCreator
  public HttpClientConnectionAttributes(
      @JsonProperty("retries") final Integer retries,
      @JsonProperty("userAgentSuffix") final String userAgentSuffix,
      @JsonProperty("timeout") final Integer timeout,
      @JsonProperty("enableCircularRedirects") final Boolean enableCircularRedirects,
      @JsonProperty("enableCookies") final Boolean enableCookies,
      @JsonProperty("useTrustStore") final Boolean useTrustStore)
  {
    this.retries = retries;
    this.userAgentSuffix = userAgentSuffix;
    this.timeout = timeout;
    this.enableCircularRedirects = enableCircularRedirects;
    this.enableCookies = enableCookies;
    this.useTrustStore = useTrustStore;
  }

  public Integer getRetries() {
    return retries;
  }

  public String getUserAgentSuffix() {
    return userAgentSuffix;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public Boolean getEnableCircularRedirects() {
    return enableCircularRedirects;
  }

  public Boolean getEnableCookies() {
    return enableCookies;
  }

  public Boolean getUseTrustStore() {
    return useTrustStore;
  }
}
