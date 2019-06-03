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
package org.sonatype.nexus.repository.apt.internal.debian;

/**
 * @since 3.next
 */
public class PackageInfo
{
  private static final String PACKAGE_FIELD = "Package";

  private static final String VERSION_FIELD = "Version";

  private static final String ARCHITECTURE_FIELD = "Architecture";

  private final ControlFile controlFile;

  public PackageInfo(ControlFile controlFile) {
    this.controlFile = controlFile;
  }

  public String getPackageName() {
    return getField(PACKAGE_FIELD);
  }

  public String getVersion() {
    return getField(VERSION_FIELD);
  }

  public String getArchitecture() {
    return getField(ARCHITECTURE_FIELD);
  }

  private String getField(String fieldName) {
    return controlFile.getField(fieldName).map(f -> f.value).get();
  }
}
