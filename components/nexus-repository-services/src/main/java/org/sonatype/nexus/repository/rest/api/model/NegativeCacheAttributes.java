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

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model of a proxy repository's negative cache settings.
 *
 * @since 3.20
 */
public class NegativeCacheAttributes
{
  @ApiModelProperty(value = "Whether to cache responses for content not present in the proxied repository",
      example = "true")
  @NotNull
  protected final Boolean enabled;

  @ApiModelProperty(value = "How long to cache the fact that a file was not found in the repository (in minutes)",
      example = "1440")
  @NotNull
  protected final Integer timeToLive;

  @JsonCreator
  public NegativeCacheAttributes(
      @JsonProperty("enabled") final Boolean enabled,
      @JsonProperty("timeToLive") final Integer timeToLive)
  {
    this.enabled = enabled;
    this.timeToLive = timeToLive;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public Integer getTimeToLive() {
    return timeToLive;
  }
}
