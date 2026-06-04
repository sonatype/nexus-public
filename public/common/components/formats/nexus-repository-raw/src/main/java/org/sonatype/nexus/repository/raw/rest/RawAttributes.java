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
package org.sonatype.nexus.repository.raw.rest;

import java.util.List;

import javax.validation.constraints.Size;

import org.sonatype.nexus.repository.raw.ContentDisposition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model of raw attributes for repositories API
 *
 * @since 3.25
 */
public class RawAttributes
{

  public static final String CONTENT_DISPOSITION = "contentDisposition";

  public static final String FORWARD_QUERY_PARAMETERS = "forwardQueryParameters";

  public static final String EXCLUDED_QUERY_PARAMETERS = "excludedQueryParameters";

  @ApiModelProperty(value = "Content Disposition",
      allowableValues = "INLINE,ATTACHMENT", example = "ATTACHMENT")
  private final ContentDisposition contentDisposition;

  @ApiModelProperty(value = "Whether to forward query parameters to the upstream repository",
      example = "true")
  private final Boolean forwardQueryParameters;

  @ApiModelProperty(
      value = "List of query parameter names to exclude from forwarding (case-insensitive). Maximum 100 entries.",
      example = "[\"apiKey\", \"token\"]")
  @Size(max = 100, message = "excludedQueryParameters may contain at most 100 entries")
  private final List<String> excludedQueryParameters;

  @JsonCreator
  public RawAttributes(
      @JsonProperty(CONTENT_DISPOSITION) final ContentDisposition contentDisposition,
      @JsonProperty(FORWARD_QUERY_PARAMETERS) final Boolean forwardQueryParameters,
      @JsonProperty(EXCLUDED_QUERY_PARAMETERS) final List<String> excludedQueryParameters)
  {
    this.contentDisposition = contentDisposition;
    this.forwardQueryParameters = forwardQueryParameters;
    this.excludedQueryParameters = excludedQueryParameters;
  }

  /**
   * Backward-compatible constructor for existing code that only sets contentDisposition
   */
  public RawAttributes(final ContentDisposition contentDisposition) {
    this(contentDisposition, null, null);
  }

  public ContentDisposition getContentDisposition() {
    return contentDisposition;
  }

  public Boolean getForwardQueryParameters() {
    return forwardQueryParameters;
  }

  public List<String> getExcludedQueryParameters() {
    return excludedQueryParameters;
  }
}
