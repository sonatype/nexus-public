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
package org.sonatype.nexus.repository.pypi.internal;

import org.sonatype.nexus.repository.Facet;

/**
 * Persistence for PyPI indexes.
 *
 * @since 3.next
 */
@Facet.Exposed
public interface PyPiIndexFacet
    extends Facet
{
  /**
   * Deletes the root index asset
   */
  void deleteRootIndex();

  /**
   * Deletes an index related to a given package
   *
   * @param packageName - packageName to use for finding the index to be removed
   */
  void deleteIndex(String packageName);
}
