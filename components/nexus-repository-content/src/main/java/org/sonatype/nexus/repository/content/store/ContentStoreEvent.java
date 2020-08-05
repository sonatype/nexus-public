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

import org.sonatype.nexus.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base event emitted by content stores.
 *
 * @since 3.26
 */
public class ContentStoreEvent
{
  final int contentRepositoryId;

  private Repository repository;

  protected ContentStoreEvent(final int contentRepositoryId) {
    this.contentRepositoryId = contentRepositoryId;
  }

  public Repository getRepository() {
    checkState(this.repository != null, "Repository has not been set");
    return repository;
  }

  void setRepository(final Repository repository) {
    checkState(this.repository == null, "Repository is already set");
    this.repository = checkNotNull(repository);
  }
}
