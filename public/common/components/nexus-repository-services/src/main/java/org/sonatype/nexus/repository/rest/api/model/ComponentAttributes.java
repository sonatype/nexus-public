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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * REST API model for attributes applying to components.
 *
 * @since 3.30
 */
public class ComponentAttributes
{
  @Schema(
      description = "Components in this repository count as proprietary for namespace conflict attacks (requires Sonatype Nexus Firewall)")
  protected Boolean proprietaryComponents;

  @JsonCreator
  public ComponentAttributes(
      @JsonProperty(value = "proprietaryComponents" /*
                                                     * NEXUS-46395 TODO: defaultValue moved to @Schema in OpenAPI
                                                     * 3.x: @Parameter(schema = @Schema(defaultValue = "false"))
                                                     */) final Boolean proprietaryComponents)
  {
    this.proprietaryComponents = proprietaryComponents;
  }

  public Boolean getProprietaryComponents() {
    return proprietaryComponents;
  }
}
