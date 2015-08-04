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
package org.sonatype.nexus.yum.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import org.sonatype.nexus.proxy.repository.Repository;

/**
 * @since yum 3.0
 */
public final class RepositoryUtils
{

  private RepositoryUtils() {
  }

  public static File getBaseDir(Repository repository)
      throws URISyntaxException, MalformedURLException
  {
    String localUrl = repository.getLocalUrl();
    if (isFile(localUrl)) {
      return new File(localUrl);
    }
    return new File(new URL(localUrl).toURI());
  }

  private static boolean isFile(String localUrl) {
    return localUrl.startsWith("/");
  }

}
