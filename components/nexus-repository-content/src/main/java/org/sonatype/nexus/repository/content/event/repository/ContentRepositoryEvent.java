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

import org.sonatype.nexus.repository.content.ContentRepository;
import org.sonatype.nexus.repository.content.store.ContentStoreEvent;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.repository.content.store.InternalIds.contentRepositoryId;

/**
 * Base {@link ContentRepository} event.
 *
 * @since 3.26
 */
public class ContentRepositoryEvent
    extends ContentStoreEvent
{
  private final ContentRepository contentRepository;

  protected ContentRepositoryEvent(final ContentRepository contentRepository) {
    super(contentRepositoryId(contentRepository));
    this.contentRepository = checkNotNull(contentRepository);
  }

  public ContentRepository getContentRepository() {
    return contentRepository;
  }

  @Override
  public String toString() {
    return "ContentRepositoryEvent{" +
        "contentRepository=" + contentRepository +
        "} " + super.toString();
  }
}
