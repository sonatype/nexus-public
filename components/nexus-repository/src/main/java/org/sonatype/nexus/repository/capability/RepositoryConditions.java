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
package org.sonatype.nexus.repository.capability;

import java.util.function.Supplier;

import org.sonatype.nexus.capability.Condition;

/**
 * Factory of {@link Condition}s related to repositories.
 *
 * @since 3.0
 */
public interface RepositoryConditions
{
  /**
   * Creates a new condition that is satisfied when a repository is in service.
   *
   * @param repositoryName getter for repository name (usually condition specific property)
   * @return created condition
   */
  Condition repositoryIsOnline(Supplier<String> repositoryName);

  /**
   * Creates a new condition that is satisfied when a repository exists.
   *
   * @param repositoryName getter for repository name (usually condition specific property)
   * @return created condition
   */
  Condition repositoryExists(Supplier<String> repositoryName);
}
