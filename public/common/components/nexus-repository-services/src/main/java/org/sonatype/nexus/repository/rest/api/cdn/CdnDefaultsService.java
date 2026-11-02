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
package org.sonatype.nexus.repository.rest.api.cdn;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.config.Configuration;

import static org.sonatype.nexus.repository.config.ConfigurationConstants.GLOBAL_DISTRIBUTION;
import static org.sonatype.nexus.repository.config.ConfigurationConstants.STORAGE;

/**
 * Service for managing CDN defaults settings.
 */
public interface CdnDefaultsService
{
  /**
   * Returns whether new repositories default to Global Distribution enabled. Returns false when no capability
   * instance exists.
   */
  boolean getDefaultForNewRepositories();

  /**
   * Sets the default. Creates the capability instance on first write.
   */
  void setDefaultForNewRepositories(boolean value);

  /**
   * Writes the capability default into {@code configuration.attributes("storage").globalDistribution} only when
   * that value is absent from the map. Called on repository creation from both the REST API path
   * ({@code AbstractRepositoriesApiResource.createRepository}) and the ExtDirect classic-UI path
   * ({@code RepositoryUiService.create}). Existing (explicit true or false) values are preserved.
   *
   * <p>
   * The gate for whether this method has any effect is upstream: the service bean is only registered when
   * Nexus is running on an Enterprise edition. On non-Enterprise editions the bean is absent, callers
   * autowire null, and this method is never invoked.
   */
  default void applyDefaultIfAbsent(final Configuration configuration) {
    NestedAttributesMap storage = configuration.attributes(STORAGE);
    if (storage.get(GLOBAL_DISTRIBUTION) == null) {
      storage.set(GLOBAL_DISTRIBUTION, getDefaultForNewRepositories());
    }
  }
}
