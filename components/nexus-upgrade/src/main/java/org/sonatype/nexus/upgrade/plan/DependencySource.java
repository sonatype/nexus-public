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
package org.sonatype.nexus.upgrade.plan;

import java.util.Collection;
import java.util.List;

/**
 * {@link Dependency} source.
 *
 * @since 3.1
 */
public interface DependencySource<T>
{
  /**
   * Returns a list of dependencies.
   */
  List<Dependency<T>> getDependencies();

  /**
   * Allow {@link DependencySource} to become aware of what it depends on.
   */
  interface DependsOnAware<T>
  {
    void setDependsOn(Collection<T> dependsOn);
  }

  /**
   * Allow {@link DependencySource} to become aware of unresolved dependencies.
   */
  interface UnresolvedDependencyAware<T>
  {
    void setUnresolvedDependencies(Collection<Dependency<T>> unresolved) throws Exception;
  }
}
