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
package org.sonatype.nexus.testsuite.testsupport.maven;

import org.joda.time.DateTime;

/**
 * @since 3.30
 */
public class MavenTestComponent
{
  private String name;

  private String baseVersion;

  private String version;

  private DateTime lastUpdated;

  public MavenTestComponent(final String name,
                            final String baseVersion,
                            final String version,
                            final DateTime lastUpdated) {
    this.name = name;
    this.baseVersion = baseVersion;
    this.version = version;
    this.lastUpdated = lastUpdated;
  }

  public String baseVersion() {
    return baseVersion;
  }

  public String version() {
    return version;
  }

  public DateTime lastUpdated() {
    return lastUpdated;
  }

  public String name() {
    return name;
  }
}
