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
package org.sonatype.nexus.obr.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Access details for OBR metadata hosted on a local or remote site.
 */
public interface ObrSite
{
  /**
   * Returns the URL of the OBR metadata, may be local (file:) or remote.
   *
   * @return the metadata URL
   */
  URL getMetadataUrl();

  /**
   * Returns the path to the OBR metadata, relative to the hosting site.
   *
   * @return the relative path
   */
  String getMetadataPath();

  /**
   * Opens a new stream to the OBR metadata, caller must close the stream.
   *
   * @return a new input stream
   */
  InputStream openStream()
      throws IOException;
}
