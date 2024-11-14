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
package org.sonatype.nexus.rapture.internal.state;

/**
 * Status exchange object.
 *
 * @since 3.0
 */
public class StatusXO
{
  private String edition;

  private String version;

  private String buildRevision;

  private String buildTimestamp;

  public StatusXO() {
  }

  public StatusXO(String edition, String version, String buildRevision, String buildTimestamp) {
    this.edition = edition;
    this.version = version;
    this.buildRevision = buildRevision;
    this.buildTimestamp = buildTimestamp;
  }

  public String getEdition() {
    return edition;
  }

  public void setEdition(String edition) {
    this.edition = edition;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getBuildRevision() {
    return buildRevision;
  }

  public void setBuildRevision(String buildRevision) {
    this.buildRevision = buildRevision;
  }

  public String getBuildTimestamp() {
    return buildTimestamp;
  }

  public void setBuildTimestamp(String buildTimestamp) {
    this.buildTimestamp = buildTimestamp;
  }

  @Override
  public String toString() {
    return "StatusXO{" +
        "edition='" + edition + '\'' +
        ", version='" + version + '\'' +
        ", buildRevision='" + buildRevision + '\'' +
        ", buildTimestamp='" + buildTimestamp + '\'' +
        '}';
  }
}
