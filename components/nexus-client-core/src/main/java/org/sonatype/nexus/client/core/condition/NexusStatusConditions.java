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
package org.sonatype.nexus.client.core.condition;

import org.sonatype.nexus.client.core.Condition;
import org.sonatype.nexus.client.core.NexusStatus;

/**
 * {@link Condition}s matching {@link NexusStatus}.
 *
 * @since 2.1
 */
public abstract class NexusStatusConditions
{

  public static Condition anyModern() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.anyModernVersion());
  }

  public static Condition anyModernPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.anyModernVersion());
  }

  public static Condition any20AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any20AndLaterVersion());
  }

  public static Condition any20AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any20AndLaterVersion());
  }

  public static Condition any21AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any21AndLaterVersion());
  }

  public static Condition any21AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any21AndLaterVersion());
  }

  public static Condition any22AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any22AndLaterVersion());
  }

  public static Condition any22AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any22AndLaterVersion());
  }

  public static Condition any23AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any23AndLaterVersion());
  }

  public static Condition any23AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any23AndLaterVersion());
  }

  public static Condition any24AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any24AndLaterVersion());
  }

  public static Condition any24AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any24AndLaterVersion());
  }

  public static Condition any25AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any25AndLaterVersion());
  }

  public static Condition any25AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any25AndLaterVersion());
  }

  public static Condition any26AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any26AndLaterVersion());
  }

  public static Condition any26AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any26AndLaterVersion());
  }

  public static Condition any27AndLater() {
    return LogicalConditions.and(EditionConditions.anyEdition(), VersionConditions.any27AndLaterVersion());
  }

  public static Condition any27AndLaterPro() {
    return LogicalConditions.and(EditionConditions.anyProEdition(), VersionConditions.any27AndLaterVersion());
  }
}
