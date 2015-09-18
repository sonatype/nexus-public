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
package org.sonatype.nexus.repository.purge;

import org.sonatype.nexus.repository.Facet;

/**
 * Find & delete components and assets that were not used/accessed for a number of days.
 *
 * @since 3.0
 */
@Facet.Exposed
public interface PurgeUnusedFacet
    extends Facet
{
  /**
   * Find & delete components and assets that were not used/accessed for a number of days.
   *
   * @param numberOfDays number of days from the moment the method is invoked. Must be > 0.
   */
  void purgeUnused(int numberOfDays);
}
