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
package org.sonatype.nexus.coreui;

import javax.validation.constraints.NotEmpty;

/**
 * Repository status exchange object.
 *
 * @since 3.0
 */
public class RepositoryStatusXO
{
  /**
   * Name of associated Repository.
   */
  @NotEmpty
  private String repositoryName;

  /**
   * Whether or not the repository is online.
   */
  private boolean online;

  /**
   * A description of the status.
   */
  private String description;

  /**
   * A reason for the status.
   */
  private String reason;

  public String getRepositoryName() {
    return repositoryName;
  }

  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    this.online = online;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }

  @Override
  public String toString() {
    return "RepositoryStatusXO{" +
        "repositoryName='" + repositoryName + '\'' +
        ", online=" + online +
        ", description='" + description + '\'' +
        ", reason='" + reason + '\'' +
        '}';
  }
}
