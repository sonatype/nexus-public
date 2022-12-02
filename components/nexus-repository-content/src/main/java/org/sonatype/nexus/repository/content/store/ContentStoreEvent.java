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

import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.sonatype.nexus.common.event.Event;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base event emitted by content stores.
 *
 * @since 3.26
 */
public class ContentStoreEvent
    implements Event
{
  final int contentRepositoryId;

  private Supplier<Optional<Repository>> repositorySupplier;

  protected ContentStoreEvent(final int contentRepositoryId) {
    this.contentRepositoryId = contentRepositoryId;
  }

  @Nullable
  public String getFormat() {
    return getRepository().map(Repository::getFormat).map(Format::getValue).orElse(null);
  }

  /**
   * May provide the Repository, repeatedly calling this method may provide different results.
   */
  public Optional<Repository> getRepository() {
    checkState(this.repositorySupplier != null, "Repository supplier has not been set");
    return repositorySupplier.get();
  }

  void setRepositorySupplier(final Supplier<Optional<Repository>> repositorySupplier) {
    checkState(this.repositorySupplier == null, "Repository supplier is already set");
    this.repositorySupplier = checkNotNull(repositorySupplier);
  }

  @Override
  public String toString() {
    Optional<Repository> repository = repositorySupplier == null ? null : repositorySupplier.get();
    return "ContentStoreEvent{" +
        "contentRepositoryId=" + contentRepositoryId +
        ", repository=" + repository +
        '}';
  }
}
