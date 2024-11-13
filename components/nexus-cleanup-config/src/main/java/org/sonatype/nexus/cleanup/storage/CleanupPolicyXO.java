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

import static org.sonatype.nexus.cleanup.storage.CleanupPolicy.ALL_CLEANUP_POLICY_FORMAT;

public class CleanupPolicyXO
{
  public static final String ALL_CLEANUP_POLICY_XO_FORMAT = "(All Formats)";

  private String name;

  private String format;

  private String mode;

  private String notes;

  private CleanupPolicyCriteria criteria;

  /**
   * sortOrder will override the typical alphanumeric ordering in the UI, so the higher your sortOrder, the closer to
   * the top you will get
   */
  private int sortOrder = 0;

  public CleanupPolicyXO() {
    //default constructor
  }

  public CleanupPolicyXO(
      final String name,
      final String format,
      final String mode,
      final String notes,
      final CleanupPolicyCriteria criteria)
  {
    this.name = name;
    this.format = format;
    this.mode = mode;
    this.notes = notes;
    this.criteria = criteria;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(final String format) {
    this.format = format;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(final String mode) {
    this.mode = mode;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
  }

  public CleanupPolicyCriteria getCriteria() {
    return criteria;
  }

  public void setCriteria(final CleanupPolicyCriteria criteria) {
    this.criteria = criteria;
  }

  public int getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(final int sortOrder) {
    this.sortOrder = sortOrder;
  }

  public static CleanupPolicyXO fromCleanupPolicy(final CleanupPolicy cleanupPolicy, final int sortOrder) {
    CleanupPolicyXO cleanupPolicyXO = fromCleanupPolicy(cleanupPolicy);
    cleanupPolicyXO.setSortOrder(sortOrder);
    return cleanupPolicyXO;
  }

  public static CleanupPolicyXO fromCleanupPolicy(final CleanupPolicy cleanupPolicy) {
    return new CleanupPolicyXO(cleanupPolicy.getName(), fromCleanupPolicyFormat(cleanupPolicy), cleanupPolicy.getMode(),
        cleanupPolicy.getNotes(), CleanupPolicyCriteria.fromMap(cleanupPolicy.getCriteria()));
  }

  public static String fromCleanupPolicyFormat(final CleanupPolicy cleanupPolicy) {
    if (cleanupPolicy == null || cleanupPolicy.getFormat() == null) {
      return null;
    }
    return cleanupPolicy.getFormat()
        .equalsIgnoreCase(ALL_CLEANUP_POLICY_FORMAT) ? ALL_CLEANUP_POLICY_XO_FORMAT : cleanupPolicy.getFormat();
  }
}
