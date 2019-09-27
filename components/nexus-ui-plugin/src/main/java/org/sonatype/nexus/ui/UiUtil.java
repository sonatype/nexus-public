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
package org.sonatype.nexus.ui;

import java.net.URL;
import java.util.Enumeration;

import javax.annotation.Nullable;

import org.eclipse.sisu.space.ClassSpace;

/**
 * @since 3.next
 */
public class UiUtil
{
  /**
   * This method gets the hashed version of a file name from the class space for the current plugin.
   *
   * For example, nexus-frontend-bundle.js would lookup a file named nexus-frontend-bundle.{hash}.js and return that.
   *
   * @param unhashedFilename the filename without the hash included
   * @param space the class space for the current plugin
   * @return the hashed filename from the file system for the current plugin
   * @return null if no matching file was found
   */
  @Nullable
  public static String getHashedFilename(final String unhashedFilename, final ClassSpace space) {
    for (
        Enumeration<URL> e = space.findEntries("static",
            unhashedFilename.substring(0, unhashedFilename.lastIndexOf('.')) + ".*" +
                unhashedFilename.substring(unhashedFilename.lastIndexOf('.')), true);
        e.hasMoreElements();
    ) {
      URL url = e.nextElement();
      return url.getPath();
    }
    return null;
  }
}
