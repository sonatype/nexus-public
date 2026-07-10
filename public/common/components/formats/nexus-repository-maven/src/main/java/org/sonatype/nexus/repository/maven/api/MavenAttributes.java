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
package org.sonatype.nexus.repository.maven.api;

import org.sonatype.nexus.swagger.SwaggerEditionVisibility;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

/**
 * REST API model for describing maven specific repository properties.
 *
 * @since 3.20
 */
@JsonFilter(SwaggerEditionVisibility.NAME)
public class MavenAttributes
{
  @Schema(description = "What type of artifacts does this repository store?",
      example = "MIXED")
  @NotEmpty
  @Pattern(regexp = "RELEASE|SNAPSHOT|MIXED", message = "must be one of RELEASE, SNAPSHOT, MIXED")
  protected final String versionPolicy;

  @Schema(description = "Validate that all paths are maven artifact or metadata paths",
      example = "STRICT")
  @NotEmpty
  @Pattern(regexp = "STRICT|PERMISSIVE", message = "must be one of STRICT, PERMISSIVE")
  protected final String layoutPolicy;

  @Schema(description = "Content Disposition", example = "ATTACHMENT")
  @SwaggerEditionVisibility(cloud = false, note = "Remove content-disposition from cloud")
  @Pattern(regexp = "INLINE|ATTACHMENT", message = "must be one of INLINE, ATTACHMENT")
  private final String contentDisposition;

  @JsonCreator
  public MavenAttributes(
      @JsonProperty("versionPolicy") final String versionPolicy,
      @JsonProperty("layoutPolicy") final String layoutPolicy,
      @JsonProperty("contentDisposition") final String contentDisposition)
  {
    this.versionPolicy = versionPolicy;
    this.layoutPolicy = layoutPolicy;
    this.contentDisposition = contentDisposition;
  }

  public String getVersionPolicy() {
    return versionPolicy;
  }

  public String getLayoutPolicy() {
    return layoutPolicy;
  }

  public String getContentDisposition() {
    return contentDisposition;
  }
}
