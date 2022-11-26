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
package org.sonatype.nexus.content.maven.store;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.datastore.api.DataSessionSupplier;
import org.sonatype.nexus.repository.content.store.ComponentStore;
import org.sonatype.nexus.transaction.Transactional;

import com.google.inject.assistedinject.Assisted;

import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED;

/**
 * @since 3.29
 */
public class Maven2ComponentStore
    extends ComponentStore<Maven2ComponentDAO>
{
  @Inject
  public Maven2ComponentStore(
      final DataSessionSupplier sessionSupplier,
      @Named(DATASTORE_CLUSTERED_ENABLED_NAMED) final boolean clustered,
      @Assisted final String storeName)
  {
    super(sessionSupplier, clustered, storeName, Maven2ComponentDAO.class);
  }

  /**
   * Updates the maven base_version of the given component in the content data store.
   *
   * @param component the component to update
   */
  @Transactional
  public void updateBaseVersion(final Maven2ComponentData component)
  {
    dao().updateBaseVersion(component);
  }

  @Transactional
  public Set<GAV> findGavsWithSnaphots(final int repositoryId, final int minimumRetained) {
    return dao().findGavsWithSnaphots(repositoryId, minimumRetained);
  }

  @Transactional
  public List<Maven2ComponentData> findComponentsForGav(final int repositoryId,
                                                        final String name,
                                                        final String group,
                                                        final String baseVersion,
                                                        final String releaseVersion)
  {
    return dao().findComponentsForGav(repositoryId, name, group, baseVersion, releaseVersion);
  }

  @Transactional
  public Set<String> getBaseVersions(final int repositoryId, final String namespace, final String name) {
    return dao().getBaseVersions(repositoryId, namespace, name);
  }

  @Transactional
  public int[] selectSnapshotsAfterRelease(final int repositoryId, final int gracePeriod) {
    return dao().selectSnapshotsAfterRelease(gracePeriod, repositoryId);
  }

  /**
   * Selects snapshot components ids last used before provided date
   *
   * @param repositoryId the repository to select from
   * @param olderThan    selects component before this date
   * @param limit        limit the selection
   * @return snapshot components last used before provided date
   */
  @Transactional
  public Collection<Integer> selectUnusedSnapshots(final int repositoryId,
                                                   final LocalDate olderThan,
                                                   final long limit)
  {
    return dao().selectUnusedSnapshots(repositoryId, olderThan, limit);
  }
}
