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
package org.sonatype.nexus.cleanup.storage;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.sonatype.nexus.cleanup.storage.config.CleanupPolicyAssetNamePattern;
import org.sonatype.nexus.common.text.Strings2;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.Objects.nonNull;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.IS_PRERELEASE_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_BLOB_UPDATED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.LAST_DOWNLOADED_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.REGEX_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_KEY;
import static org.sonatype.nexus.cleanup.config.CleanupPolicyConstants.RETAIN_SORT_BY_KEY;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.PRERELEASES;
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.RELEASES;

/**
 * Collection of the criteria associated with a Cleanup Policy.
 */
public class CleanupPolicyCriteria
{
  private static final long DAY_IN_SECONDS = TimeUnit.DAYS.toSeconds(1L);

  private final Integer lastBlobUpdated;

  private final Integer lastDownloaded;

  private final CleanupPolicyReleaseType releaseType;

  @CleanupPolicyAssetNamePattern
  private final String regex;

  private final Integer retain;

  private final String sortBy;

  public CleanupPolicyCriteria(
      final Integer lastBlobUpdated,
      final Integer lastDownloaded,
      final CleanupPolicyReleaseType releaseType,
      final String regex,
      final Integer retain,
      final String sortBy)
  {
    this.lastBlobUpdated = lastBlobUpdated;
    this.lastDownloaded = lastDownloaded;
    this.releaseType = releaseType;
    this.regex = regex;
    this.retain = retain;
    this.sortBy = sortBy;
  }

  public static CleanupPolicyCriteria fromMap(final Map<String, String> criteriaMap) {
    return new CleanupPolicyCriteria(
        getIntegerFromCriteriaMap(criteriaMap, LAST_BLOB_UPDATED_KEY),
        getIntegerFromCriteriaMap(criteriaMap, LAST_DOWNLOADED_KEY),
        getReleaseType(criteriaMap),
        criteriaMap.get(REGEX_KEY),
        getIntegerFromCriteriaMap(criteriaMap, RETAIN_KEY),
        criteriaMap.get(RETAIN_SORT_BY_KEY));
  }

  public static Map<String, String> toMap(final CleanupPolicyCriteria criteria) {
    HashMap<String, String> criteriaMap = newHashMap();

    if (nonNull(criteria.lastBlobUpdated)) {
      String value = valueOf(criteria.lastBlobUpdated * DAY_IN_SECONDS);
      criteriaMap.put(LAST_BLOB_UPDATED_KEY, value);
    }

    if (nonNull(criteria.lastDownloaded)) {
      String value = valueOf(criteria.lastDownloaded * DAY_IN_SECONDS);
      criteriaMap.put(LAST_DOWNLOADED_KEY, value);
    }

    if (nonNull(criteria.releaseType)) {
      String value = Boolean.toString(criteria.releaseType == PRERELEASES);
      criteriaMap.put(IS_PRERELEASE_KEY, value);
    }

    if (nonNull(criteria.regex)) {
      criteriaMap.put(REGEX_KEY, criteria.regex);
    }

    if (nonNull(criteria.retain)) {
      String value = valueOf(criteria.retain);
      criteriaMap.put(RETAIN_KEY, value);
    }

    if (nonNull(criteria.sortBy)) {
      criteriaMap.put(RETAIN_SORT_BY_KEY, criteria.sortBy);
    }

    return criteriaMap;
  }

  @Nullable
  private static Integer getIntegerFromCriteriaMap(final Map<String, String> criteriaMap, final String criteria) {
    String value = criteriaMap.get(criteria);

    if (Strings2.notBlank(value)) {
      return parseInt(value) / (int) DAY_IN_SECONDS;
    }

    return null;
  }

  @Nullable
  private static CleanupPolicyReleaseType getReleaseType(final Map<String, String> criteriaMap) {
    String value = criteriaMap.get(IS_PRERELEASE_KEY);

    if (Strings2.notBlank(value)) {
      return parseBoolean(value) ? PRERELEASES : RELEASES;
    }

    return null;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", CleanupPolicyCriteria.class.getSimpleName() + "[", "]")
        .add("lastBlobUpdated=" + lastBlobUpdated)
        .add("lastDownloaded=" + lastDownloaded)
        .add("releaseType=" + releaseType)
        .add("regex='" + regex + "'")
        .add("retain='" + retain + "'")
        .add("sortBy=" + sortBy)
        .toString();
  }
}
