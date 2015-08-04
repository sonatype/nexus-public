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
package org.sonatype.nexus.yum.client;

/**
 * Interface to access nexus-yum-repository-plugin functionality in tests
 *
 * @since yum 3.0
 */
public interface Yum
{

  /**
   * @param repositoryId Nexus repository (cannot be null)
   * @param alias        alias name (cannot be null)
   * @return the version behind the given alias
   */
  String getAlias(String repositoryId, String alias);

  /**
   * Creates for the given repository an alias to a specific
   *
   * @param repositoryId Nexus repository (cannot be null)
   * @param alias        alias name (cannot be null)
   * @param version      version to be aliased
   */
  void createOrUpdateAlias(String repositoryId, String alias, String version);

}
