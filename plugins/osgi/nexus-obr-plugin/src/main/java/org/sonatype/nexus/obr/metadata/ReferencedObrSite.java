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
 * An OBR site that's referenced from another OBR by a referral tag.
 */
public class ReferencedObrSite
    extends AbstractObrSite
{
  private final URL url;

  /**
   * Creates a referenced OBR site based on the given URL.
   *
   * @param url the metadata URL
   */
  public ReferencedObrSite(final URL url) {
    this.url = url;
  }

  public URL getMetadataUrl() {
    return url;
  }

  public String getMetadataPath() {
    return "";
  }

  @Override
  protected InputStream openRawStream()
      throws IOException
  {
    return url.openStream();
  }

  @Override
  protected String getContentType() {
    try {
      return url.openConnection().getContentType();
    }
    catch (final IOException e) {
      return null;
    }
  }
}
