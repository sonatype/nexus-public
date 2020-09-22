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
package org.sonatype.repository.helm.internal.createindex;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Facet.Exposed;

/**
 * Facet interface for rebuilding Helm index.yaml files
 *
 * @since 3.next
 */
@Exposed
public interface CreateIndexFacet
    extends Facet
{
  /**
   * Mark the helm index yaml as invalidated such that it will be rebuilt after waiting for a configured amount of time
   * to prevent unnecessary successive rebuilds of the metadata.
   */
  void invalidateIndex();
}
