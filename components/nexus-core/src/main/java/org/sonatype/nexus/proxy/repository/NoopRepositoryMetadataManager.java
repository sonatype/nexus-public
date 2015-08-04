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
package org.sonatype.nexus.proxy.repository;

import org.sonatype.nexus.proxy.ResourceStoreRequest;

/**
 * A simple "noop" implementation of {@link RepositoryMetadataManager} that simply does nothing. This is what is
 * "served" by default by {@link AbstractRepository#getRepositoryMetadataManager()} method. Overriding that method to
 * return some "native" {@link RepositoryMetadataManager} for given repository type is the responsibility of the
 * developer, if needed.
 *
 * @author cstamas
 * @since 2.1
 */
public class NoopRepositoryMetadataManager
    implements RepositoryMetadataManager
{
  @Override
  public boolean recreateMetadata(final ResourceStoreRequest request) {
    return false; // noop
  }

  @Override
  public boolean expireMetadataCaches(final ResourceStoreRequest request) {
    return false; // noop
  }

  @Override
  public boolean expireNotFoundMetadataCaches(final ResourceStoreRequest request) {
    return false; // noop
  }
}
