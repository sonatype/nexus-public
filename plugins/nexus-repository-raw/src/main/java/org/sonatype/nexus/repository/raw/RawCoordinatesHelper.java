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
package org.sonatype.nexus.repository.raw;

/**
 * Helper methods for extracting component/asset coordinates for raw artifacts.
 *
 * @since 3.0
 */
public class RawCoordinatesHelper
{
  public static String getGroup(String path) {
    StringBuilder group = new StringBuilder();
    int i = path.lastIndexOf('/');
    if (!path.startsWith("/") || i == 0) {
      group.append("/");
    }
    if (i != -1) {
      group.append(path, 0, i);
    }
    return group.toString();
  }

  private RawCoordinatesHelper() {
    // Don't instantiate
  }
}
