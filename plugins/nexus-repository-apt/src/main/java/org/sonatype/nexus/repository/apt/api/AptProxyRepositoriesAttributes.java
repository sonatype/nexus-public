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
package org.sonatype.nexus.repository.apt.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotEmpty;

/**
 * REST API model for apt-specific proxy attributes.
 *
 * @since 3.20
 */
public class AptProxyRepositoriesAttributes
    extends AptHostedRepositoriesAttributes
{
  @ApiModelProperty(value = "Whether this repository is flat", example = "false")
  @NotEmpty
  protected final Boolean flat;

  @JsonCreator
  public AptProxyRepositoriesAttributes(
      @JsonProperty("distribution") final String distribution,
      @JsonProperty("flat") final Boolean flat)
  {
    super(distribution);
    this.flat = flat;
  }

  public Boolean getFlat() {
    return flat;
  }
}
