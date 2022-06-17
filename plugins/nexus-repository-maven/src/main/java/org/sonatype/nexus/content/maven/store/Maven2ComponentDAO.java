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

import org.sonatype.nexus.repository.content.store.ComponentDAO;

import org.apache.ibatis.annotations.Param;

/**
 * @since 3.25
 */
public interface Maven2ComponentDAO
    extends ComponentDAO
{
  /**
   * Adds base_version column. See {@see Maven2ComponentDAO.xml}
   */
  @Override
  void extendSchema();

  /**
   * Updates the maven base_version of the given component in the content data store.
   *
   * @param component the component to update
   */
  void updateBaseVersion(Maven2ComponentData component);

  /**
   * Find all GAVs that qualify for deletion.
   *
   * @param repositoryId the repository to select from
   * @param minimumRetained the minimum number of snapshots to keep.
   * @return all GAVs that qualify for deletion
   *
   * @since 3.30
   */
  Set<GAV> findGavsWithSnaphots(@Param("repositoryId") final int repositoryId,
                                @Param("minimumRetained") final int minimumRetained);

  /**
   * Find components by Group Artifact Version(GAVs)
   *
   * @param repositoryId the repository to select from
   * @param name artifact name
   * @param group artifact group
   * @param baseVersion artifact version
   * @param releaseVersion artifact releaseVersion
   * @return all components by Group Artifact Version(GAVs)
   *
   * @since 3.30
   */
  List<Maven2ComponentData> findComponentsForGav(@Param("repositoryId") final int repositoryId,
                                                 @Param("name") final String name,
                                                 @Param("group") final String group,
                                                 @Param("baseVersion") final String baseVersion,
                                                 @Param("releaseVersion") final String releaseVersion);

  /**
   * Retrieve known base versions for a provided GA.
   *
   * @param repositoryId the repository containing the components
   * @param namespace    the namespace for the components
   * @param name         the name for the components
   *
   * @return a unique set of base versions
   */
  Set<String> getBaseVersions(
      @Param("repositoryId") int repositoryId,
      @Param("namespace") String namespace,
      @Param("name") String name);

  /**
   * Find snapshots to delete for which a release version exists
   *
   * @param repositoryId the repository to select from
   * @param gracePeriod an optional period to keep snapshots around
   * @return array of snapshot components IDs to delete for which a release version exists
   *
   * @since 3.30
   */
  int[] selectSnapshotsAfterRelease(@Param("repositoryId") final int repositoryId,
                                   @Param("gracePeriod") final int gracePeriod);
  /**
   * Selects snapshot components ids last used before provided date
   *
   * @param repositoryId the repository to select from
   * @param olderThan    selects component before this date
   * @param limit        limit the selection
   * @return snapshot components last used before provided date
   *
   * @since 3.30
   */
  Collection<Integer> selectUnusedSnapshots(@Param("repositoryId") int repositoryId,
                                            @Param("olderThan") LocalDate olderThan,
                                            @Param("limit") long limit);
}
