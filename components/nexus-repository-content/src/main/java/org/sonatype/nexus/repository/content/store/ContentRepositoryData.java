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
package org.sonatype.nexus.repository.content.store;

import org.sonatype.nexus.common.entity.EntityId;
import org.sonatype.nexus.repository.content.ContentRepository;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link ContentRepository} data backed by the content data store.
 *
 * @since 3.20
 */
public class ContentRepositoryData
    extends AbstractRepositoryContent
    implements ContentRepository
{
  private EntityId configRepositoryId;

  // ContentRepository API

  @Override
  public EntityId configRepositoryId() {
    return configRepositoryId;
  }

  @Override
  public Integer contentRepositoryId() {
    return repositoryId;
  }

  // MyBatis setters + validation

  /**
   * Sets the config repository id.
   */
  public void setConfigRepositoryId(final EntityId configRepositoryId) {
    this.configRepositoryId = checkNotNull(configRepositoryId);
  }

  @Override
  public String toString() {
    return "ContentRepositoryData{" +
        "configRepositoryId=" + configRepositoryId +
        "} " + super.toString();
  }
}
