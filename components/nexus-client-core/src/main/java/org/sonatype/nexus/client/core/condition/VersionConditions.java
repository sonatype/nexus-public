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
import org.sonatype.nexus.client.core.condition.internal.GenericVersionScheme;
import org.sonatype.nexus.client.core.condition.internal.InvalidVersionSpecificationException;
import org.sonatype.nexus.client.core.condition.internal.Version;
import org.sonatype.nexus.client.core.condition.internal.VersionConstraint;
import org.sonatype.nexus.client.internal.util.Check;

/**
 * {@link Condition}s that matches remote Nexus version.
 *
 * @since 2.1
 */
public abstract class VersionConditions
    implements Condition
{

  /**
   * Version constraint that matches all released Nexus versions beginning with version 1.9.
   */
  private static final VersionConstraint POST_1_8_VERSIONS = parseVersionConstraint("(1.8.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.0.
   */
  private static final VersionConstraint POST_1_9_VERSIONS = parseVersionConstraint("(1.9.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.1.
   */
  private static final VersionConstraint POST_2_0_VERSIONS = parseVersionConstraint("(2.0.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.2.
   */
  private static final VersionConstraint POST_2_1_VERSIONS = parseVersionConstraint("(2.1.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.3.
   */
  private static final VersionConstraint POST_2_2_VERSIONS = parseVersionConstraint("(2.2.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.4.
   */
  private static final VersionConstraint POST_2_3_VERSIONS = parseVersionConstraint("(2.3.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.5.
   */
  private static final VersionConstraint POST_2_4_VERSIONS = parseVersionConstraint("(2.4.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.6.
   */
  private static final VersionConstraint POST_2_5_VERSIONS = parseVersionConstraint("(2.5.99,)");

  /**
   * Version constraint that matches all released Nexus versions beginning with version 2.7.
   */
  private static final VersionConstraint POST_2_6_VERSIONS = parseVersionConstraint("(2.6.99,)");

  // ==

  public static Condition anyModernVersion() {
    return new VersionCondition(POST_1_8_VERSIONS);
  }

  public static Condition any20AndLaterVersion() {
    return new VersionCondition(POST_1_9_VERSIONS);
  }

  public static Condition any21AndLaterVersion() {
    return new VersionCondition(POST_2_0_VERSIONS);
  }

  public static Condition any22AndLaterVersion() {
    return new VersionCondition(POST_2_1_VERSIONS);
  }

  public static Condition any23AndLaterVersion() {
    return new VersionCondition(POST_2_2_VERSIONS);
  }

  public static Condition any24AndLaterVersion() {
    return new VersionCondition(POST_2_3_VERSIONS);
  }

  public static Condition any25AndLaterVersion() {
    return new VersionCondition(POST_2_4_VERSIONS);
  }

  public static Condition any26AndLaterVersion() {
    return new VersionCondition(POST_2_5_VERSIONS);
  }

  public static Condition any27AndLaterVersion() {
    return new VersionCondition(POST_2_6_VERSIONS);
  }

  public static Condition withVersion(final String versionRange) {
    return new VersionCondition(parseVersionConstraint(versionRange));
  }

  private static class VersionCondition
      implements Condition
  {

    private final VersionConstraint suitableVersions;

    private VersionCondition(final VersionConstraint suitableVersions) {
      this.suitableVersions = Check.notNull(suitableVersions, VersionConstraint.class);
    }

    /**
     * Returns true if both, editionShort and version matches the given constraints.
     */
    public boolean isSatisfiedBy(final NexusStatus status) {
      final Version version = parseVersion(status.getVersion());
      return suitableVersions.containsVersion(version);
    }

    @Override
    public String explainNotSatisfied(final NexusStatus status) {
      return String.format("(version \"%s\" contained in \"%s\")", status.getVersion(), suitableVersions);
    }
  }

  private static VersionConstraint parseVersionConstraint(final String versionConstraint) {
    try {
      return new GenericVersionScheme().parseVersionConstraint(versionConstraint);
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException("Unable to parse version constraint: " + versionConstraint, e);
    }
  }

  private static Version parseVersion(final String version) {
    try {
      return new GenericVersionScheme().parseVersion(version);
    }
    catch (InvalidVersionSpecificationException e) {
      throw new IllegalArgumentException("Unable to parse version: " + version, e);
    }
  }

}