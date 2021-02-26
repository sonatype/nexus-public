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

import javax.validation.constraints.NotNull;

import org.sonatype.nexus.repository.types.HostedType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * API Hosted Repository for simple formats which do not have custom attributes for hosted repositories.
 *
 * @since 3.20
 */
@JsonIgnoreProperties(value = {"format", "type", "url"}, allowGetters = true)
public class SimpleApiHostedRepository
    extends AbstractApiRepository
{
  @NotNull
  protected final HostedStorageAttributes storage;

  protected final CleanupPolicyAttributes cleanup;

  protected final ComponentAttributes component;

  @JsonCreator
  public SimpleApiHostedRepository(
      @JsonProperty("name") final String name,
      @JsonProperty("format") final String format,
      @JsonProperty("url") final String url,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final HostedStorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("component") final ComponentAttributes component)
  {
    super(name, format, HostedType.NAME, url, online);
    this.storage = storage;
    this.cleanup = cleanup;
    this.component = component;
  }

  public CleanupPolicyAttributes getCleanup() {
    return cleanup;
  }

  public HostedStorageAttributes getStorage() {
    return storage;
  }

  public ComponentAttributes getComponent() {
    return component;
  }
}
