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
package org.sonatype.nexus.repository.rest.api;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;

/**
 * Helper utility for contentDisposition handling in repository API converters.
 * Provides consistent logic for determining contentDisposition during create/update operations.
 */
public final class ContentDispositionHelper
{
  public static final String ATTACHMENT = "ATTACHMENT";

  private ContentDispositionHelper() {
    // Utility class
  }

  /**
   * Determines the contentDisposition value to use based on request and existing repository state.
   *
   * Behavior:
   * - If explicit value provided in request, use it
   * - If UPDATE (existing repo): preserve existing value (including null for legacy repos)
   * - If CREATE (no existing repo): default to ATTACHMENT
   *
   * @param requestedDisposition the disposition from the request (may be null)
   * @param repositoryManager the repository manager for looking up existing repos
   * @param repositoryName the repository name
   * @param formatAttributesKey the attributes key for the format (e.g., "maven", "raw")
   * @return the contentDisposition value to set (may be null for legacy repos)
   */
  public static String resolveContentDisposition(
      final String requestedDisposition,
      final RepositoryManager repositoryManager,
      final String repositoryName,
      final String formatAttributesKey)
  {
    // If explicit value provided, use it
    if (requestedDisposition != null) {
      return requestedDisposition;
    }

    // Check for existing repository (update vs create)
    Repository existingRepo = repositoryManager.get(repositoryName);
    if (existingRepo != null) {
      // UPDATE: preserve existing value (including null for legacy repos)
      return existingRepo.getConfiguration()
          .attributes(formatAttributesKey)
          .get("contentDisposition", String.class);
    }

    // CREATE: default to ATTACHMENT
    return ATTACHMENT;
  }
}
