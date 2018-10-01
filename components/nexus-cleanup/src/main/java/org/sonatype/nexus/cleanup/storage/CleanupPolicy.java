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

import java.util.Map;

import org.sonatype.nexus.common.entity.AbstractEntity;

/**
 * Represents a configured cleanup policy
 *
 * @since 3.14
 */
public class CleanupPolicy
    extends AbstractEntity
{
  public static final CleanupPolicy NONE_POLICY = new CleanupPolicy("None", null, null, null, null);

  public static final String ALL_CLEANUP_POLICY_FORMAT = "ALL_FORMATS";

  private String name;

  private String notes;

  /**
   * Format that the policy is associated with or All
   */
  private String format;

  /**
   * The mode that the policy should take, e.g. deletion
   */
  private String mode;

  private Map<String, String> criteria;

  public CleanupPolicy() {
    //NOSONAR
  }

  public CleanupPolicy(final String name,
                       final String notes,
                       final String format,
                       final String mode,
                       final Map<String, String> criteria)
  {
    this.name = name;
    this.notes = notes;
    this.format = format;
    this.mode = mode;
    this.criteria = criteria;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(final String notes) {
    this.notes = notes;
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

  public Map<String, String> getCriteria() {
    return criteria;
  }

  public void setCriteria(final Map<String, String> criteria) {
    this.criteria = criteria;
  }

  @Override
  public String toString() {
    return "CleanupPolicy{" +
        "name='" + name + '\'' +
        ", format='" + format + '\'' +
        ", mode='" + mode + '\'' +
        ", criteria=" + criteria +
        '}';
  }
}
