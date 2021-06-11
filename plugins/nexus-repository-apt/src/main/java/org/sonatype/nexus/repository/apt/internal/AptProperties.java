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
package org.sonatype.nexus.repository.apt.internal;

/**
 * Apt properties
 *
 * @since 3.31
 */
public final class AptProperties
{
  private AptProperties() {
    //Properties class
  }

  //Apt general properties
  public static final String DEB = "DEB";

  // Apt hosted properties for metadata rebuild
  public static final String P_INDEX_SECTION = "index_section";

  public static final String P_ARCHITECTURE = "architecture";

  public static final String P_PACKAGE_NAME = "package_name";

  public static final String P_PACKAGE_VERSION = "package_version";

  //Apt supported metadata archive file extensions
  public static final String GZ = ".gz";

  public static final String BZ2 = ".bz2";
}
