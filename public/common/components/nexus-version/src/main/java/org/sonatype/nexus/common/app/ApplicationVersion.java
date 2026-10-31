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
package org.sonatype.nexus.common.app;

/**
 * Provide information about the application version.
 *
 * Accessors will return {@link ApplicationVersionSupport#UNKNOWN} if unable to determine value.
 *
 * @since 3.0
 */
public interface ApplicationVersion
{
  /**
   * Returns the version.
   */
  String getVersion();

  /**
   * Returns the edition short name (e.g. {@code "PRO"}, {@code "COMMUNITY"}, {@code "CORE"}).
   *
   * This is the value reported in the REST API, an HTTP response header and the outbound user agent, so it remains the
   * source of truth for the edition string. It is a poor basis for behaviour decisions, though: the short name is
   * ambiguous (self-hosted Pro and Cloud both report {@code "PRO"}) and comparing it against string literals silently
   * excludes editions added later. Decide behaviour with a capability question such as {@link #isAnyProEdition()} or
   * {@link #isCommunityEdition()} instead.
   */
  String getEdition();

  /**
   * Whether the running edition is a Pro-family tier (Pro, Pro+ or Enterprise). Cloud reports the Pro short name and is
   * therefore included. Prefer this over comparing {@link #getEdition()} against {@code "PRO"}.
   */
  default boolean isAnyProEdition() {
    return NexusEditionType.fromShortName(getEdition())
        .filter(NexusEditionType::isProFamily)
        .isPresent();
  }

  /**
   * Whether the running edition is Community. Prefer this over comparing {@link #getEdition()} against
   * {@code "COMMUNITY"}.
   */
  default boolean isCommunityEdition() {
    return NexusEditionType.fromShortName(getEdition())
        .filter(type -> type == NexusEditionType.COMMUNITY)
        .isPresent();
  }

  /**
   * Returns the edition and version suitable for branded display.
   */
  String getBrandedEditionAndVersion();

  /**
   * Returns the build revision.
   */
  String getBuildRevision();

  /**
   * Returns the build timestamp.
   */
  String getBuildTimestamp();

  /**
   * Returns the minimum Nexus 2 version allowed for migration compatibility with this build.
   *
   * @since 3.4
   */
  String getNexus2CompatibleVersion();
}
