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

import org.sonatype.nexus.plugins.RepositoryType;
import org.sonatype.nexus.proxy.registry.ContentClass;

/**
 * A Shadow Repository is a special repository type that usually points to a master repository and transforms it in
 * some
 * way (look at Maven1 to Maven2 layout changing repo).
 *
 * @author cstamas
 */
@RepositoryType(pathPrefix = "shadows")
public interface ShadowRepository
    extends Repository
{
  /**
   * The content class that is expected to have the repository set as master for this ShadowRepository.
   */
  ContentClass getMasterRepositoryContentClass();

  /**
   * Gets sync at startup.
   */
  boolean isSynchronizeAtStartup();

  /**
   * Sets sync at start.
   */
  void setSynchronizeAtStartup(boolean value);

  /**
   * Returns the master.
   */
  Repository getMasterRepository();

  /**
   * Sets the master.
   */
  void setMasterRepository(Repository repository)
      throws IncompatibleMasterRepositoryException;

  /**
   * Triggers syncing with master repository.
   */
  void synchronizeWithMaster();
}
