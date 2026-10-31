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

import java.util.Optional;

import static java.util.Arrays.stream;

/**
 * The editions/license tiers a running Nexus instance can report through
 * {@link ApplicationVersion#getEdition()}.
 * <p>
 * This enum exists so behaviour can be decided by asking a capability question
 * ({@link ApplicationVersion#isAnyProEdition()}, {@link ApplicationVersion#isCommunityEdition()}) instead of comparing
 * the
 * edition short name against string literals scattered through business logic. String comparisons silently exclude any
 * edition added later; the capability questions keep working when editions are added, renamed or restructured.
 * <p>
 * <b>Short names are not unique.</b> Self-hosted Pro and Cloud both report {@code "PRO"} (see the edition
 * implementations), so the short name cannot distinguish them. {@link #PRO_PLUS} and {@link #ENTERPRISE} are live
 * editions that already publish their own distinct short names ({@code "PROPLUS"} / {@code "ENTERPRISE"}), and
 * {@code nexus.edition} can resolve to them (see {@code ConditionalOnEdition}). They are not returned by
 * {@link ApplicationVersion#getEdition()} at runtime today only because no {@code ApplicationVersion} bean is wired for
 * those tiers yet — the existing implementations are gated for Core, Community, Pro and Cloud only — which is a
 * missing-bean gap tracked by the {@code ApplicationVersionImpl} follow-up (NEXUS-54770 FU-4), not a missing short
 * name. They are declared here so the pro-family grouping is authored against the full tier axis and wiring the beans
 * later is a no-op at the call sites.
 */
public enum NexusEditionType
{
  CORE("CORE"),
  COMMUNITY("COMMUNITY"),
  PRO("PRO"),
  PRO_PLUS("PROPLUS"),
  ENTERPRISE("ENTERPRISE");

  private final String shortName;

  NexusEditionType(final String shortName) {
    this.shortName = shortName;
  }

  /**
   * The short name as returned by {@link ApplicationVersion#getEdition()}.
   */
  public String getShortName() {
    return shortName;
  }

  /**
   * Whether this is a Pro-family tier (Pro, Pro+ or Enterprise). Cloud reports {@link #PRO} and is therefore
   * included.
   */
  public boolean isProFamily() {
    return this == PRO || this == PRO_PLUS || this == ENTERPRISE;
  }

  /**
   * Resolves an edition short name (case-insensitive) to its {@link NexusEditionType}, if recognised.
   */
  public static Optional<NexusEditionType> fromShortName(final String shortName) {
    if (shortName == null) {
      return Optional.empty();
    }
    return stream(values())
        .filter(type -> type.shortName.equalsIgnoreCase(shortName.trim()))
        .findFirst();
  }

  /**
   * Collapses the Pro family (Pro, Pro+, Enterprise) to the {@link #PRO} short name for consumers that intentionally
   * present as a single {@code "PRO"} tier. Any other edition (or an unrecognised short name) is returned unchanged.
   * <p>
   * Self-hosted status producers no longer collapse; they report the real short name so the frontend
   * can distinguish the tiers (NEXUS-54766). This helper is retained for consumers that deliberately
   * keep the single-tier presentation.
   */
  public static String toFrontendShortName(final String shortName) {
    return fromShortName(shortName)
        .filter(NexusEditionType::isProFamily)
        .map(type -> PRO.shortName)
        .orElse(shortName);
  }
}
