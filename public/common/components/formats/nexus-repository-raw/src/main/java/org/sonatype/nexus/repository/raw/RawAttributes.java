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
package org.sonatype.nexus.repository.raw;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

public class RawAttributes
{
  @ApiModelProperty(value = "Content Disposition",
      allowableValues = "INLINE,ATTACHMENT", example = "ATTACHMENT")
  @NotEmpty
  private final String contentDisposition;

  @ApiModelProperty(value = "Whether to forward query parameters to the upstream repository",
      example = "true")
  @JsonInclude(Include.NON_NULL)
  private final Boolean forwardQueryParameters;

  @ApiModelProperty(value = "List of query parameter names to exclude from forwarding (case-insensitive)",
      example = "[\"apiKey\", \"token\"]")
  @JsonInclude(Include.NON_EMPTY)
  private final List<String> excludedQueryParameters;

  @JsonCreator
  public RawAttributes(
      @JsonProperty("contentDisposition") final String contentDisposition,
      @JsonProperty("forwardQueryParameters") final Boolean forwardQueryParameters,
      @JsonProperty("excludedQueryParameters") final List<String> excludedQueryParameters)
  {
    this.contentDisposition = contentDisposition;
    this.forwardQueryParameters = forwardQueryParameters;
    this.excludedQueryParameters = excludedQueryParameters;
  }

  public RawAttributes(final String contentDisposition) {
    this(contentDisposition, null, null);
  }

  public String getContentDisposition() {
    return contentDisposition;
  }

  public Boolean getForwardQueryParameters() {
    return forwardQueryParameters;
  }

  public List<String> getExcludedQueryParameters() {
    return excludedQueryParameters;
  }
}
