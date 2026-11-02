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

/**
 * Emitted when a repository is restored from Trash.
 * <p>
 * Distinct from {@link RepositoryRestoredEvent}, which fires for boot-time repository restoration and
 * failed-repository retry -- neither of which is a user-initiated Trash restore. This event extends
 * {@link RepositoryCreatedEvent} so that subscribers which materialize a repository on creation (search-index
 * rebuild, tracking, etc.) still run on a Trash restore, while letting consumers that care about the lifecycle
 * distinction (e.g. the repository webhook and audit) report it as a restore rather than a creation (NEXUS-55410).
 */
public class RepositoryTrashRestoredEvent
    extends RepositoryCreatedEvent
{
  public RepositoryTrashRestoredEvent(final Repository repository) {
    super(repository);
  }
}
