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

import java.io.Closeable;
import java.io.IOException;
import java.net.URL;

import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.osgi.service.obr.Resource;

/**
 * An {@link XmlPullParser} that knows how to parse OBR resources.
 */
public interface ObrParser
    extends XmlPullParser, Closeable
{
  /**
   * Returns the URL of the OBR metadata, may be local (file:) or remote.
   *
   * @return the metadata URL
   */
  URL getMetadataUrl();

  /**
   * Returns the maximum allowed depth of nested OBR referrals.
   *
   * @return the maximum depth
   */
  int getMaxDepth();

  /**
   * Parses an OBR resource from the underlying OBR metadata stream.
   *
   * @return the parsed resource
   */
  Resource parseResource()
      throws IOException;
}
