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
package org.sonatype.nexus.repository.internal.search.index;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.search.index.SearchIndexUpdateManager;

import static java.util.Objects.requireNonNull;

/**
 * Service that handles finding Repositories that need to be re-indexed.
 *
 * Formats need to implement {@link SearchIndexUpdateManager} to define the logic for the format, otherwise the default
 * implementation is to never re-index.
 *
 * @since 3.37
 */
@Named
public class SearchUpdateService
    extends ComponentSupport
{
  private final Map<String, SearchIndexUpdateManager> searchIndexUpdateManagers;

  @Inject
  public SearchUpdateService(final Map<String, SearchIndexUpdateManager> searchIndexUpdateManagers) {
    this.searchIndexUpdateManagers = requireNonNull(searchIndexUpdateManagers);
  }

  public boolean needsReindex(final Repository repository) {
    SearchIndexUpdateManager updateManager = searchIndexUpdateManagers.get(repository.getFormat().getValue());
    if (updateManager != null) {
      return updateManager.needsReindex(repository);
    }
    return false;
  }

  public void doneReindexing(final Repository repository) {
    SearchIndexUpdateManager updateManager = searchIndexUpdateManagers.get(repository.getFormat().getValue());
    if (updateManager != null) {
      updateManager.doneReindexing(repository);
    }
  }
}
