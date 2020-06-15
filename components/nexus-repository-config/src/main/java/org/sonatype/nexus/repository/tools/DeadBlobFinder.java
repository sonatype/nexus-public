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
package org.sonatype.nexus.repository.tools;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.sonatype.nexus.repository.Repository;

/**
 * Examines Asset metadata and confirms the sha1 of all referenced blobs. Reports on any instances where
 * Blob binary is missing or indicates a different sha1 than that stored in the DB.
 * @since 3.3
 */
public interface DeadBlobFinder<A>
{
  /**
   * Based on the db metadata, confirm that all Blobs exist and sha1 values match. Can optionally ignore any records
   * that don't have a blobRef, which is expected for NuGet search results.
   * @parem repository  The Repository to inspect
   */
  default List<DeadBlobResult<A>> find(@NotNull final Repository repository) {
    return find(repository, true);
  }

  /**
   * Based on the db metadata, confirm that all Blobs exist and sha1 values match. Can optionally ignore any records
   * that don't have a blobRef, which is expected for NuGet search results.
   * @parem repository  The Repository to inspect
   * @param ignoreMissingBlobRefs
   */
  List<DeadBlobResult<A>> find(@NotNull final Repository repository, boolean ignoreMissingBlobRefs);
}
