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
package org.sonatype.nexus.cleanup.storage

import java.util.concurrent.TimeUnit

import javax.annotation.Nullable

import groovy.transform.ToString
import groovy.transform.builder.Builder

import static com.google.common.collect.Maps.newHashMap
import static java.lang.Boolean.parseBoolean
import static java.lang.Integer.parseInt
import static java.lang.String.valueOf
import static java.util.Objects.nonNull
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.PRERELEASES
import static org.sonatype.nexus.cleanup.storage.CleanupPolicyReleaseType.RELEASES
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.IS_PRERELEASE_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_BLOB_UPDATED_KEY
import static org.sonatype.nexus.repository.search.DefaultComponentMetadataProducer.LAST_DOWNLOADED_KEY

/**
 *  Collection of the criteria associated with a Cleanup Policy
 *
 *  @since 3.14
 */
@Builder
@ToString(includePackage = false, includeNames = true)
class CleanupPolicyCriteria
{
  private static final DAY_IN_SECONDS = TimeUnit.DAYS.toSeconds(1)

  Integer lastBlobUpdated

  Integer lastDownloaded

  CleanupPolicyReleaseType releaseType

  static CleanupPolicyCriteria fromMap(final Map<String, String> criteriaMap) {
    return builder()
        .lastBlobUpdated(getLastBlobUpdated(criteriaMap))
        .lastDownloaded(getLastDownloaded(criteriaMap))
        .releaseType(getReleaseType(criteriaMap))
        .build()
  }

  @Nullable
  static Integer getLastBlobUpdated(final Map<String, String> criteriaMap) {
    return getIntegerFromCriteriaMap(criteriaMap, LAST_BLOB_UPDATED_KEY)
  }

  @Nullable
  static Integer getLastDownloaded(final Map<String, String> criteriaMap) {
    return getIntegerFromCriteriaMap(criteriaMap, LAST_DOWNLOADED_KEY)
  }

  @Nullable
  static Integer getIntegerFromCriteriaMap(final Map<String, String> criteriaMap, final String criteria) {
    def value = criteriaMap?.get(criteria)

    if (value) {
      return parseInt(value) / DAY_IN_SECONDS
    }

    return null
  }

  @Nullable
  static CleanupPolicyReleaseType getReleaseType(final Map<String, String> criteriaMap) {
    def value = criteriaMap?.get(IS_PRERELEASE_KEY)

    if (value) {
      return parseBoolean(value) ? PRERELEASES : RELEASES
    }

    return null
  }

  static Map<String, String> toMap(final CleanupPolicyCriteria criteria) {
    final HashMap<String, String> criteriaMap = newHashMap()

    if (nonNull(criteria.lastBlobUpdated)) {
      criteriaMap.put(LAST_BLOB_UPDATED_KEY, valueOf(criteria.lastBlobUpdated * DAY_IN_SECONDS))
    }

    if (nonNull(criteria.lastDownloaded)) {
      criteriaMap.put(LAST_DOWNLOADED_KEY, valueOf(criteria.lastDownloaded * DAY_IN_SECONDS))
    }

    if (nonNull(criteria.releaseType)) {
      criteriaMap.put(IS_PRERELEASE_KEY, Boolean.toString(criteria.releaseType == PRERELEASES))
    }

    return criteriaMap
  }
}
