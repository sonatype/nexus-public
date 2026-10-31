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

import org.sonatype.nexus.repository.config.UniqueRepositoryName;
import org.sonatype.nexus.validation.constraint.NamePatternConstants;
import org.sonatype.nexus.validation.group.Create;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * @since 3.20
 */
public abstract class AbstractRepositoryApiRequest<S extends StorageAttributes>
    implements RepositoryApiRequest
{
  @Schema(description = "A unique identifier for this repository", example = "internal",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @Pattern(regexp = NamePatternConstants.REGEX, message = NamePatternConstants.MESSAGE)
  @NotEmpty
  @UniqueRepositoryName(groups = Create.class)
  protected String name;

  @Schema(description = "Component format used by this repository", hidden = true)
  @NotEmpty
  protected String format;

  @Schema(description = "Controls if deployments of and updates to artifacts are allowed", hidden = true)
  @NotEmpty
  protected String type;

  @Schema(description = "Whether this repository accepts incoming requests", example = "true",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull
  protected Boolean online;

  @NotNull
  @Valid
  protected final S storage;

  @JsonCreator
  public AbstractRepositoryApiRequest(
      @JsonProperty("name") final String name,
      @JsonProperty("format") final String format,
      @JsonProperty("type") final String type,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final S storage)
  {
    this.name = name;
    this.format = format;
    this.type = type;
    this.online = online;
    this.storage = storage;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getFormat() {
    return format;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public Boolean getOnline() {
    return online;
  }

  public S getStorage() {
    return storage;
  }
}
