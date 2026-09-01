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
package org.sonatype.nexus.repository.search.sql.store;

import java.time.OffsetDateTime;

/**
 * One distinct component version with the repositories containing it.
 */
public class ComponentVersionData
{
  private String version;

  private String normalisedVersion;

  private String repositories;

  private OffsetDateTime lastModified;

  public String getVersion() {
    return version;
  }

  public void setVersion(final String version) {
    this.version = version;
  }

  public String getNormalisedVersion() {
    return normalisedVersion;
  }

  public void setNormalisedVersion(final String normalisedVersion) {
    this.normalisedVersion = normalisedVersion;
  }

  /**
   * Comma-separated repository names, as aggregated by the database. Repository names cannot
   * contain a comma (see NamePatternConstants.REGEX), so this is unambiguous to split.
   */
  public String getRepositories() {
    return repositories;
  }

  public void setRepositories(final String repositories) {
    this.repositories = repositories;
  }

  public OffsetDateTime getLastModified() {
    return lastModified;
  }

  public void setLastModified(final OffsetDateTime lastModified) {
    this.lastModified = lastModified;
  }
}
