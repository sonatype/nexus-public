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
package org.sonatype.nexus.cleanup.internal.rest;

import javax.validation.constraints.NotEmpty;

import org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType;
import org.sonatype.nexus.cleanup.storage.config.CleanupPolicyAssetNamePattern;

/**
 * @since 3.29
 */
public class PreviewRequestXO
{
  private String name; // cleanup policy name

  @NotEmpty
  private String repository;

  private Integer criteriaLastBlobUpdated;

  private Integer criteriaLastDownloaded;

  private CleanupPolicyReleaseType criteriaReleaseType;

  @CleanupPolicyAssetNamePattern
  private String criteriaAssetRegex;

  private String filter;

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getRepository() {
    return repository;
  }

  public void setRepository(final String repository) {
    this.repository = repository;
  }

  public CleanupPolicyReleaseType getCriteriaReleaseType() {
    return criteriaReleaseType;
  }

  public Integer getCriteriaLastBlobUpdated() {
    return criteriaLastBlobUpdated;
  }

  public Integer getCriteriaLastDownloaded() {
    return criteriaLastDownloaded;
  }

  public String getCriteriaAssetRegex() {
    return criteriaAssetRegex;
  }

  public void setCriteriaAssetRegex(final String criteriaAssetRegex) {
    this.criteriaAssetRegex = criteriaAssetRegex;
  }

  public void setCriteriaLastBlobUpdated(final Integer criteriaLastBlobUpdated) {
    this.criteriaLastBlobUpdated = criteriaLastBlobUpdated;
  }

  public void setCriteriaLastDownloaded(final Integer criteriaLastDownloaded) {
    this.criteriaLastDownloaded = criteriaLastDownloaded;
  }

  public void setCriteriaReleaseType(final CleanupPolicyReleaseType criteriaReleaseType) {
    this.criteriaReleaseType = criteriaReleaseType;
  }

  public String getFilter() {
    return filter;
  }

  public void setFilter(final String filter) {
    this.filter = filter;
  }
}
