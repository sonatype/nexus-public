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
package org.sonatype.nexus.repositories.metadata;

import java.io.IOException;

import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.repository.metadata.MetadataHandlerException;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;

public interface NexusRepositoryMetadataHandler
{
  /**
   * Will get the repository metadata from the passed remote repository root. If none found, null is returned.
   *
   * @param url the repository root of the remote repository.
   * @return the metadata, or null if not found.
   * @throws MetadataHandlerException if some validation or other non-io problem occurs
   * @throws IOException              if some IO problem occurs (except file not found).
   */
  RepositoryMetadata readRemoteRepositoryMetadata(String url)
      throws MetadataHandlerException,
             IOException;

  /**
   * Returns the Nexus repository metadata.
   */
  RepositoryMetadata readRepositoryMetadata(String repositoryId)
      throws NoSuchRepositoryException,
             MetadataHandlerException,
             IOException;

  /**
   * Writes/updates the Nexus repository metadata.
   */
  void writeRepositoryMetadata(String repositoryId, RepositoryMetadata repositoryMetadata)
      throws NoSuchRepositoryException,
             MetadataHandlerException,
             IOException;
}
