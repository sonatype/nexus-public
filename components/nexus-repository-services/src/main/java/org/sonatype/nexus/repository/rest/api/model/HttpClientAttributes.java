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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model for describing HTTP connection properties for proxy repositories.
 *
 * @since 3.20
 */
public class HttpClientAttributes
{
  @ApiModelProperty(value = "Whether to block outbound connections on the repository", example = "false")
  @NotNull
  protected final Boolean blocked;

  @ApiModelProperty(
      value = "Whether to auto-block outbound connections if remote peer is detected as unreachable/unresponsive",
      example = "true")
  @NotNull
  protected final Boolean autoBlock;

  @Valid
  protected final HttpClientConnectionAttributes connection;

  @Valid
  protected final HttpClientConnectionAuthenticationAttributes authentication;

  @JsonCreator
  public HttpClientAttributes(
      @JsonProperty("blocked") final Boolean blocked,
      @JsonProperty("autoBlock") final Boolean autoBlock,
      @JsonProperty("connection") final HttpClientConnectionAttributes connection,
      @JsonProperty("authentication") final HttpClientConnectionAuthenticationAttributes authentication)
  {
    this.blocked = blocked;
    this.autoBlock = autoBlock;
    this.connection = connection;
    this.authentication = authentication;
  }

  public Boolean getBlocked() {
    return blocked;
  }

  public Boolean getAutoBlock() {
    return autoBlock;
  }

  public HttpClientConnectionAttributes getConnection() {
    return connection;
  }

  public HttpClientConnectionAuthenticationAttributes getAuthentication() {
    return authentication;
  }
}
