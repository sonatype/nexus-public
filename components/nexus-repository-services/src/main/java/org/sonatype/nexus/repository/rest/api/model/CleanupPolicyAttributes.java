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

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;

/**
 * REST API model which describes cleanup policies for a repository.
 *
 * @since 3.20
 */
public class CleanupPolicyAttributes
{
  @ApiModelProperty(value = "Components that match any of the applied policies will be deleted",
      dataType = "[Ljava.lang.String;")
  protected Collection<String> policyNames;

  @JsonCreator
  public CleanupPolicyAttributes(@JsonProperty("policyNames") final Collection<String> policyNames) {
    this.policyNames = policyNames;
  }

  public Collection<String> getPolicyNames() {
    return policyNames;
  }
}
