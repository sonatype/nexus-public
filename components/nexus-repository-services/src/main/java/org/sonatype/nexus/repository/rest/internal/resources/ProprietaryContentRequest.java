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

package org.sonatype.nexus.repository.rest.internal.resources;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 3.30
 */
public class ProprietaryContentRequest
{
  @NotNull
  private final List<String> proprietary;

  @NotNull
  private final List<String> nonProprietary;

  @JsonCreator
  public ProprietaryContentRequest(
      @JsonProperty("proprietary") final List<String> proprietary,
      @JsonProperty("nonProprietary") final List<String> nonProprietary)
  {
    this.proprietary = proprietary;
    this.nonProprietary = nonProprietary;
  }

  public List<String> getProprietary() {
    return proprietary;
  }

  public List<String> getNonProprietary() {
    return nonProprietary;
  }
}
