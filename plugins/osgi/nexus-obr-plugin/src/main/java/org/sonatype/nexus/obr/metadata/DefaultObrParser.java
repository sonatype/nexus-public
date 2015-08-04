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
import java.net.URL;

import org.sonatype.nexus.proxy.LocalStorageException;

import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.osgi.impl.bundle.obr.resource.RepositoryImpl;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.service.obr.Resource;

public class DefaultObrParser
    extends MXParser
    implements ObrParser
{
  private final URL metadataUrl;

  private final int maxDepth;

  private final RepositoryImpl obr;

  public DefaultObrParser(final ObrSite site, final int maxDepth, final boolean relative)
      throws XmlPullParserException, IOException
  {
    setInput(new XmlStreamReader(site.openStream()));

    metadataUrl = site.getMetadataUrl();
    this.maxDepth = maxDepth;

    // only allow absolute context for remote OBRs
    URL contextUrl = metadataUrl;
    if (relative || "file".equals(contextUrl.getProtocol())) {
      contextUrl = new URL("file:" + site.getMetadataPath());
    }

    obr = new RepositoryImpl(contextUrl);
  }

  public URL getMetadataUrl() {
    return metadataUrl;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public Resource parseResource()
      throws IOException
  {
    try {
      return new ResourceImpl(obr, this);
    }
    catch (final XmlPullParserException e) {
      throw new LocalStorageException("Error parsing OBR resource", e);
    }
  }

  public void close()
      throws IOException
  {
    reader.close();
  }
}
