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
package org.sonatype.nexus.repository.config.internal;

import org.sonatype.nexus.common.event.EventWithSource;
import org.sonatype.nexus.repository.config.Configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Internal implementation of {@link org.sonatype.nexus.repository.config.ConfigurationSoftDeletedEvent}.
 * Used for HA replication of soft-delete operations — remote nodes stop and untrack the repository
 * without deleting its stored data.
 */
public class ConfigurationSoftDeletedEvent
    extends EventWithSource
    implements org.sonatype.nexus.repository.config.ConfigurationSoftDeletedEvent
{
  private String repositoryName;

  public ConfigurationSoftDeletedEvent() {
    // deserialization
  }

  public ConfigurationSoftDeletedEvent(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @Override
  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(final String repositoryName) {
    this.repositoryName = repositoryName;
  }

  @JsonIgnore
  @Override
  public Configuration getConfiguration() {
    throw new UnsupportedOperationException("Configuration is not available in the event, use DB instead");
  }
}
