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

import jakarta.validation.constraints.NotNull;

import org.sonatype.nexus.validation.constraint.UriString;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

/**
 * REST API model describing a proxy repository.
 *
 * @since 3.20
 */
public class ProxyAttributes
{
  @Schema(description = "Location of the remote repository being proxied",
      example = "https://remote.repository.com")
  @UriString
  @NotEmpty
  protected final String remoteUrl;

  @Schema(description = "How long to cache artifacts before rechecking the remote repository (in minutes)",
      example = "1440")
  @NotNull
  protected final Integer contentMaxAge;

  @Schema(description = "How long to cache metadata before rechecking the remote repository (in minutes)",
      example = "1440")
  @NotNull
  protected final Integer metadataMaxAge;

  @Schema(description = "When true, preserves encoded characters like %2B (plus), %23 (hash), and %20 (space) " +
      "in their encoded form when proxying to the remote repository. " +
      "Enable this when proxying to AWS S3, Cloudflare CDN, or Azure Blob Storage, which require encoded characters to remain encoded. "
      +
      "When false (default), uses standard encoding that preserves literal + characters (works for crates.io and most remotes). "
      +
      "SECURITY NOTE: Only enable this for trusted remote repositories. Path traversal sequences (..) in redirects are automatically normalized.",
      example = "false")
  protected final boolean preserveEncodedCharacters;

  @JsonCreator
  public ProxyAttributes(
      @JsonProperty("remoteUrl") final String remoteUrl,
      @JsonProperty("contentMaxAge") final Integer contentMaxAge,
      @JsonProperty("metadataMaxAge") final Integer metadataMaxAge,
      @JsonProperty("preserveEncodedCharacters") final Boolean preserveEncodedCharacters)
  {
    this.remoteUrl = remoteUrl;
    this.contentMaxAge = contentMaxAge;
    this.metadataMaxAge = metadataMaxAge;
    this.preserveEncodedCharacters = preserveEncodedCharacters != null ? preserveEncodedCharacters : false;
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

  public boolean getPreserveEncodedCharacters() {
    return preserveEncodedCharacters;
  }
}
