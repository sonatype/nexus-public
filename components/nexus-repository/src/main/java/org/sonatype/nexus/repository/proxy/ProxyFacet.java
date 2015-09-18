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
package org.sonatype.nexus.repository.proxy;

import java.io.IOException;
import java.net.URI;

import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Context;

/**
 * A
 *
 * @since 3.0
 */
@Facet.Exposed
public interface ProxyFacet
    extends Facet
{
  /**
   * Obtain the content which the user has requested, either by retrieving cached content, or by fetching new or
   * updated content from the upstream repository.
   */
  Content get(Context context) throws IOException;

  /**
   * Returns the root of the remote repository.
   */
  URI getRemoteUrl();

  /**
   * Invalidates all entries from proxy cache, causing all subsequent requests to attempt to fetch new or updated
   * content, if any.
   */
  void invalidateProxyCaches();
}
