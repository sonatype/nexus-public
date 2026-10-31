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
package org.sonatype.nexus.repository.content.event.repository;

import org.sonatype.nexus.common.event.EventWithSource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Event sent whenever a {@link Repository}'s attributes change between all nodes.
 */
public class ContentRepositoryAttributesDesEvent
    extends EventWithSource
{
  private final String repoName;

  private final String key;

  @JsonCreator
  public ContentRepositoryAttributesDesEvent(
      @JsonProperty("repoName") final String repoName,
      @Nullable @JsonProperty("key") final String key)
  {
    this.repoName = requireNonNull(repoName);
    this.key = key;
  }

  @Nullable
  public String getKey() {
    return key;
  }

  public String getRepoName() {
    return repoName;
  }
}
