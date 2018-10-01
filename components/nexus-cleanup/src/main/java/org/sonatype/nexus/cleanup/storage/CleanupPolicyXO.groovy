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

import javax.validation.constraints.Pattern

import org.sonatype.nexus.cleanup.storage.config.UniqueCleanupPolicyName
import org.sonatype.nexus.validation.group.Create

import groovy.transform.ToString
import groovy.transform.builder.Builder
import org.hibernate.validator.constraints.NotBlank
import org.hibernate.validator.constraints.NotEmpty

import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT
import static org.sonatype.nexus.validation.constraint.NamePatternConstants.MESSAGE
import static org.sonatype.nexus.validation.constraint.NamePatternConstants.REGEX

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

  @Pattern(regexp = REGEX, message = MESSAGE)
  @UniqueCleanupPolicyName(groups = Create)
  @NotEmpty
  String name

  @NotBlank
  String format

  @NotBlank
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

  static CleanupPolicy mergeIntoCleanupPolicy(final CleanupPolicyXO cleanupPolicyXO,
                                              final CleanupPolicy cleanupPolicy) {
    cleanupPolicy.notes = cleanupPolicyXO.notes
    cleanupPolicy.format = toCleanupPolicyFormat(cleanupPolicyXO)
    cleanupPolicy.mode = cleanupPolicyXO.mode
    cleanupPolicy.criteria = CleanupPolicyCriteria.toMap(cleanupPolicyXO.criteria)
    return cleanupPolicy
  }

  static CleanupPolicy toCleanupPolicy(final CleanupPolicyXO cleanupPolicyXO) {
    return new CleanupPolicy(
        cleanupPolicyXO.name,
        cleanupPolicyXO.notes,
        toCleanupPolicyFormat(cleanupPolicyXO),
        cleanupPolicyXO.mode,
        CleanupPolicyCriteria.toMap(cleanupPolicyXO.criteria)
    )
  }

  static String fromCleanupPolicyFormat(final CleanupPolicy cleanupPolicy) {
    return cleanupPolicy.format?.equalsIgnoreCase(ALL_CLEANUP_POLICY_FORMAT) ?
        ALL_CLEANUP_POLICY_XO_FORMAT : cleanupPolicy.format
  }

  static String toCleanupPolicyFormat(final CleanupPolicyXO cleanupPolicyXO) {
    return cleanupPolicyXO.format?.equalsIgnoreCase(ALL_CLEANUP_POLICY_XO_FORMAT) ?
        ALL_CLEANUP_POLICY_FORMAT : cleanupPolicyXO.format
  }
}
