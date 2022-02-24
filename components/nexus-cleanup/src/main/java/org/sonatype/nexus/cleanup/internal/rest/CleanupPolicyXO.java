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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.sonatype.nexus.cleanup.storage.CleanupPolicy;
import org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType;
import org.sonatype.nexus.cleanup.storage.config.CleanupPolicyAssetNamePattern;
import org.sonatype.nexus.cleanup.storage.config.UniqueCleanupPolicyName;
import org.sonatype.nexus.validation.group.Create;

import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_FORMATS;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.PRERELEASES;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.RELEASES;
import static org.sonatype.nexus.validation.constraint.NamePatternConstants.MESSAGE;
import static org.sonatype.nexus.validation.constraint.NamePatternConstants.REGEX;

/**
 * @since 3.29
 */
public class CleanupPolicyXO
{
  @Pattern(regexp = REGEX, message = MESSAGE)
  @UniqueCleanupPolicyName(groups = Create.class)
  @NotEmpty
  @Size(max = 255)
  private String name;

  @NotEmpty
  private String format;

  private String notes;

  private Long criteriaLastBlobUpdated; // days

  private Long criteriaLastDownloaded; // days

  private CleanupPolicyReleaseType criteriaReleaseType;

  @CleanupPolicyAssetNamePattern
  private String criteriaAssetRegex;

  private int inUseCount;

  public String getName() {
    return name;
  }

  public String getFormat() {
    return format;
  }

  public String getNotes() {
    return notes;
  }

  public CleanupPolicyReleaseType getCriteriaReleaseType() {
    return criteriaReleaseType;
  }

  public Long getCriteriaLastBlobUpdated() {
    return criteriaLastBlobUpdated;
  }

  public Long getCriteriaLastDownloaded() {
    return criteriaLastDownloaded;
  }

  public String getCriteriaAssetRegex() {
    return criteriaAssetRegex;
  }

  public int getInUseCount() {
    return inUseCount;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
  }

  public void setCriteriaAssetRegex(final String criteriaAssetRegex) {
    this.criteriaAssetRegex = criteriaAssetRegex;
  }

  public void setCriteriaLastBlobUpdated(final Long criteriaLastBlobUpdated) {
    this.criteriaLastBlobUpdated = criteriaLastBlobUpdated;
  }

  public void setCriteriaLastDownloaded(final Long criteriaLastDownloaded) {
    this.criteriaLastDownloaded = criteriaLastDownloaded;
  }

  public void setCriteriaReleaseType(final CleanupPolicyReleaseType criteriaReleaseType) {
    this.criteriaReleaseType = criteriaReleaseType;
  }

  public void setInUseCount(int inUseCount) {
    this.inUseCount = inUseCount;
  }

  public static CleanupPolicyXO fromCleanupPolicy(final CleanupPolicy cleanupPolicy, int inUseCount) {
    CleanupPolicyXO xo = new CleanupPolicyXO();
    xo.setName(cleanupPolicy.getName());
    xo.setFormat(cleanupPolicy.getFormat().equals(ALL_CLEANUP_POLICY_FORMAT) ? ALL_FORMATS : cleanupPolicy.getFormat());
    xo.setNotes(cleanupPolicy.getNotes());
    xo.setCriteriaAssetRegex(cleanupPolicy.getCriteria().get(REGEX_KEY));
    xo.setCriteriaLastBlobUpdated(toDays(getNullableLong(cleanupPolicy.getCriteria(), LAST_BLOB_UPDATED_KEY)));
    xo.setCriteriaLastDownloaded(toDays(getNullableLong(cleanupPolicy.getCriteria(), LAST_DOWNLOADED_KEY)));
    xo.setCriteriaReleaseType(getNullableReleaseType(cleanupPolicy.getCriteria()));
    xo.setInUseCount(inUseCount);
    return xo;
  }

  private static Long getNullableLong(final Map<String, String> map, final String key) {
    String value = map.get(key);

    if (value == null) {
      return null;
    }

    return Long.valueOf(value);
  }

  private static CleanupPolicyReleaseType getNullableReleaseType(final Map<String, String> map) {
    String value = map.get(IS_PRERELEASE_KEY);

    if (value == null) {
      return null;
    }

    return Boolean.parseBoolean(value) ? PRERELEASES : RELEASES;
  }

  private static Long toDays(final Long seconds) {
    if (seconds == null) {
      return null;
    }
    else {
      return TimeUnit.SECONDS.toDays(seconds);
    }
  }
}
