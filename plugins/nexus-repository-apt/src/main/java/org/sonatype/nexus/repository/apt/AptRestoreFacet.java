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
package org.sonatype.nexus.repository.apt;

import java.io.IOException;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.repository.Facet;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;
import org.sonatype.nexus.repository.view.Content;

/**
 * @since 3.17
 */
@Facet.Exposed
public interface AptRestoreFacet extends Facet
{
  Content restore(final AssetBlob assetBlob, final String path) throws IOException;

  boolean assetExists(final String path);

  Query getComponentQuery(final Blob blob) throws IOException;

  boolean componentRequired(final String name);
}
