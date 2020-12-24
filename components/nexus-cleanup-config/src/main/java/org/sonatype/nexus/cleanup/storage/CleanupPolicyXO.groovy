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


import groovy.transform.ToString
import groovy.transform.builder.Builder

import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT

/**
 * Cleanup Policy exchange object.
 *
 * @since 3.14
 */
@Builder
@ToString(includePackage = false, includeNames = true)
class CleanupPolicyXO
{
  public static final String ALL_CLEANUP_POLICY_XO_FORMAT = "(All Formats)"

  String name

  String format

  String mode

  String notes

  CleanupPolicyCriteria criteria

  /**
   * sortOrder will override the typical alphanumeric ordering in the UI, so the higher your sortOrder,
   * the closer to the top you will get
   */
  int sortOrder = 0

  static CleanupPolicyXO fromCleanupPolicy(final CleanupPolicy cleanupPolicy, final int sortOrder) {
    CleanupPolicyXO cleanupPolicyXO = fromCleanupPolicy(cleanupPolicy)
    cleanupPolicyXO.setSortOrder(sortOrder)
    return cleanupPolicyXO
  }

  static CleanupPolicyXO fromCleanupPolicy(final CleanupPolicy cleanupPolicy) {
    return builder()
        .name(cleanupPolicy.name)
        .format(fromCleanupPolicyFormat(cleanupPolicy))
        .mode(cleanupPolicy.mode)
        .notes(cleanupPolicy.notes)
        .criteria(CleanupPolicyCriteria.fromMap(cleanupPolicy.criteria))
        .build()
  }

  static String fromCleanupPolicyFormat(final CleanupPolicy cleanupPolicy) {
    return cleanupPolicy.format?.equalsIgnoreCase(ALL_CLEANUP_POLICY_FORMAT) ?
        ALL_CLEANUP_POLICY_XO_FORMAT : cleanupPolicy.format
  }
}
