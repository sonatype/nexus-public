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
package org.sonatype.nexus.repository.move;

import java.util.List;

import javax.annotation.Nullable;

/**
 * @since 3.28
 */
public interface ChangeRepositoryBlobStoreStore
{
  /**
   * @return all {@link ChangeRepositoryBlobStoreConfiguration} objects.
   */
  List<ChangeRepositoryBlobStoreConfiguration> list();

  /**
   * Persist a new {@link ChangeRepositoryBlobStoreConfiguration} object.
   */
  void create(final ChangeRepositoryBlobStoreConfiguration configuration);

  @Nullable
  ChangeRepositoryBlobStoreConfiguration findByRepositoryName(String repository);

  /**
   * Finds a ChangeRepository Blob Store task by blob store name, it could be source, or target blob store
   * @param blobStoreName
   * @return
   */
  @Nullable
  List<ChangeRepositoryBlobStoreConfiguration> findByBlobStoreName(final String blobStoreName);

  void add(final String repository, final String sourceBlobStoreName, final String targetBlobStoreName);

  void update(final String repository, final String sourceBlobStoreName, final String targetBlobStoreName);

  void remove(final String repository);
}
