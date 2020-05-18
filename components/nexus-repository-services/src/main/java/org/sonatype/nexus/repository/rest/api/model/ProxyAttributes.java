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
import org.hibernate.validator.constraints.NotEmpty;

/**
 * REST API model describing a proxy repository.
 *
 * @since 3.20
 */
public class ProxyAttributes
{
  @ApiModelProperty(value = "Location of the remote repository being proxied", example = "https://remote.repository.com")
  @NotEmpty
  protected final String remoteUrl;

  @ApiModelProperty(value = "How long to cache artifacts before rechecking the remote repository (in minutes)",
      example = "1440")
  @NotNull
  protected final Integer contentMaxAge;

  @ApiModelProperty(value = "How long to cache metadata before rechecking the remote repository (in minutes)",
      example = "1440")
  @NotNull
  protected final Integer metadataMaxAge;

  @JsonCreator
  public ProxyAttributes(
      @JsonProperty("remoteUrl") final String remoteUrl,
      @JsonProperty("contentMaxAge") final Integer contentMaxAge,
      @JsonProperty("metadataMaxAge") final Integer metadataMaxAge)
  {
    this.remoteUrl = remoteUrl;
    this.contentMaxAge = contentMaxAge;
    this.metadataMaxAge = metadataMaxAge;
  }

  public String getRemoteUrl() {
    return remoteUrl;
  }

  public Integer getContentMaxAge() {
    return contentMaxAge;
  }

  public Integer getMetadataMaxAge() {
    return metadataMaxAge;
  }
}
