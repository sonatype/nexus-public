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
package org.sonatype.nexus.repository.npm.internal.audit.report;

import java.util.Objects;
import java.util.Set;

import org.sonatype.nexus.repository.vulnerability.SeverityLevel;

/**
 * Model will be serialized into Json representation for npm audit report.
 *
 * @since 3.24
 */
public class Advisory
{
  private final Set<Finding> findings;

  private final int id;

  private final String title;

  private final String moduleName;

  private final String patchedVersions;

  private final String overview;

  private final transient SeverityLevel severityLevel;

  private final String severity;

  private final String url;

  public Advisory(
      final Set<Finding> findings,
      final int id,
      final String title,
      final String moduleName,
      final String patchedVersions,
      final String overview,
      final SeverityLevel severityLevel,
      final String severity,
      final String url)
  {
    this.findings = findings;
    this.id = id;
    this.title = title;
    this.moduleName = moduleName;
    this.patchedVersions = patchedVersions;
    this.overview = overview;
    this.severityLevel = severityLevel;
    this.severity = severity;
    this.url = url;
  }

  public Set<Finding> getFindings() {
    return findings;
  }

  public int getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getModuleName() {
    return moduleName;
  }

  public String getPatchedVersions() {
    return patchedVersions;
  }

  public String getOverview() {
    return overview;
  }

  public String getSeverity() {
    return severity;
  }

  public SeverityLevel getSeverityLevel() {
    return severityLevel;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Advisory advisory = (Advisory) o;
    return id == advisory.id;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
