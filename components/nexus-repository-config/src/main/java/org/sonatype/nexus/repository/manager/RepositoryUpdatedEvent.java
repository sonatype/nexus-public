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
package org.sonatype.nexus.repository.manager;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryEvent;
import org.sonatype.nexus.repository.config.Configuration;

/**
 * Emitted when a repository has been updated.
 *
 * @since 3.0
 */
public class RepositoryUpdatedEvent
  extends RepositoryEvent
{
  private final Configuration oldConfiguration;

  public RepositoryUpdatedEvent(final Repository repository, final Configuration oldConfiguration) {
    super(repository);
    this.oldConfiguration = oldConfiguration;
  }

  /**
   * The previous configuration of the Repository.
   *
   * @since 3.21
   */
  public Configuration getOldConfiguration() {
    return oldConfiguration;
  }
}
