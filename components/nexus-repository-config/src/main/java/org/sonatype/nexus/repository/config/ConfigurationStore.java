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
package org.sonatype.nexus.repository.config;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sonatype.goodies.lifecycle.Lifecycle;

/**
 * {@link Configuration} store.
 *
 * @since 3.0
 */
public interface ConfigurationStore
  extends Lifecycle
{
  List<Configuration> list();

  void create(Configuration configuration);

  void update(Configuration configuration);

  void delete(Configuration configuration);

  /**
   * Create a new {@link Configuration} instance.
   *
   * @since 3.21
   */
  Configuration newConfiguration();

  Collection<Configuration> readByNames(Set<String> repositoryNames);

  /**
   * Checks whether the database contains an entry with the provided name.<br>
   *
   * Note: Underlying implementations may return {@code false} if unsupported.
   */
  boolean exists(String repositoryName);

  /**
   * Read all repository configuration by given {@code recipeName}
   * @param recipeName recipe name for collecting configurations
   * @return configurations filtered by {@code recipeName}
   */
  Collection<Configuration> readByRecipe(String recipeName);
}
