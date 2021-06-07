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
package org.sonatype.nexus.repository.apt.rest;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.sonatype.nexus.repository.apt.api.AptHostedRepositoriesAttributes;
import org.sonatype.nexus.repository.apt.AptFormat;
import org.sonatype.nexus.repository.rest.api.model.ComponentAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedRepositoryApiRequest;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HostedStorageAttributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @since 3.20
 */
@JsonIgnoreProperties({"format", "type"})
public class AptHostedRepositoryApiRequest
    extends HostedRepositoryApiRequest
{
  @NotNull
  @Valid
  protected final AptHostedRepositoriesAttributes apt;

  @NotNull
  @Valid
  protected final AptSigningRepositoriesAttributes aptSigning;

  @JsonCreator
  public AptHostedRepositoryApiRequest(
      @JsonProperty("name") final String name,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final HostedStorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("apt") final AptHostedRepositoriesAttributes apt,
      @JsonProperty("aptSigning") final AptSigningRepositoriesAttributes aptSigning,
      @JsonProperty("component") final ComponentAttributes componentAttributes)
  {
    super(name, AptFormat.NAME, online, storage, cleanup, componentAttributes);
    this.apt = apt;
    this.aptSigning = aptSigning;
  }

  public AptHostedRepositoriesAttributes getApt() {
    return apt;
  }

  public AptSigningRepositoriesAttributes getAptSigning() {
    return aptSigning;
  }
}
