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
package org.sonatype.nexus.common.app;

/**
 * Provide information about the application version.
 *
 * Accessors will return {@link ApplicationVersionSupport#UNKNOWN} if unable to determine value.
 *
 * @since 3.0
 */
public interface ApplicationVersion
{
  /**
   * Returns the version.
   */
  String getVersion();

  /**
   * Returns the edition.
   */
  String getEdition();

  /**
   * Returns the edition and version suitable for branded display.
   */
  String getBrandedEditionAndVersion();

  /**
   * Returns the build revision.
   */
  String getBuildRevision();

  /**
   * Returns the build timestamp.
   */
  String getBuildTimestamp();

  /**
   * Returns the minimum Nexus 2 version allowed for migration compatibility with this build.
   *
   * @since 3.4
   */
  String getNexus2CompatibleVersion();
}
