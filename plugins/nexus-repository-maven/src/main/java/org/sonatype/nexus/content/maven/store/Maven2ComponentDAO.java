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
  void extendSchema();

  /**
   * Updates the maven base_version of the given component in the content data store.
   *
   * @param component the component to update
   */
  void updateBaseVersion(Maven2ComponentData component);

  /**
   * Selects snapshot components ids last used before provided date
   *
   * @param repositoryId the repository to select from
   * @param olderThan    selects component before this date
   * @param limit        limit the selection
   * @return snapshot components last used before provided date
   */
  Collection<Integer> selectUnusedSnapshots(@Param("repositoryId") int repositoryId,
                                            @Param("olderThan") LocalDate olderThan,
                                            @Param("limit") long limit);
}
