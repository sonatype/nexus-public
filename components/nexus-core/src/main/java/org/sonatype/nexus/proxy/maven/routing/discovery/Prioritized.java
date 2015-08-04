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
package org.sonatype.nexus.proxy.maven.routing.discovery;

import java.util.Comparator;

/**
 * Prioritized support, when ordering of components is essential.
 *
 * @author cstamas
 * @since 2.4
 */
public interface Prioritized
{
  /**
   * Returns the priority of this instance.
   *
   * @return the priority of this instance.
   */
  int getPriority();

  // ==

  /**
   * Comparator for {@link Prioritized} instances.
   */
  public static class PriorityOrderingComparator<T extends Prioritized>
      implements Comparator<T>
  {
    @Override
    public int compare(T o1, T o2) {
      return o1.getPriority() - o2.getPriority();
    }
  }
}
