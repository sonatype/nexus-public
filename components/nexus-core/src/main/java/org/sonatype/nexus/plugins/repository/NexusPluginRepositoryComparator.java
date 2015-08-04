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
package org.sonatype.nexus.plugins.repository;

import java.util.Comparator;

/**
 * {@link Comparator} that places two {@link NexusPluginRepository} in order of priority; smallest number first.
 */
@Deprecated
final class NexusPluginRepositoryComparator
    implements Comparator<NexusPluginRepository>
{
  // ----------------------------------------------------------------------
  // Public methods
  // ----------------------------------------------------------------------

  public int compare(final NexusPluginRepository o1, final NexusPluginRepository o2) {
    final int order = o1.getPriority() - o2.getPriority();
    if (0 == order) {
      return o1.getId().compareTo(o2.getId());
    }
    return order;
  }
}
