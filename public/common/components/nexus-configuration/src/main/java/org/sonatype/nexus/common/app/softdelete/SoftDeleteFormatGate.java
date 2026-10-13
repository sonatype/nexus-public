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
package org.sonatype.nexus.common.app.softdelete;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.sonatype.nexus.common.app.FeatureFlags.SOFT_DELETE_ENABLED_NAMED_VALUE;
import static org.sonatype.nexus.common.app.FeatureFlags.SOFT_DELETE_FORMATS_NAMED_VALUE;

/**
 * Central gate for whether a delete should become a recoverable soft-delete, combining the global
 * feature flag ({@code nexus.softDelete.enabled}) with a per-format allowlist
 * ({@code nexus.softDelete.formats}).
 *
 * <p>
 * This is what enables a phased, format-by-format rollout of the Trash/soft-delete feature
 * (NEXUS-49147). The schema columns are added to every format up front (they are inert/nullable), but
 * a delete only becomes a soft-delete when the flag is on <em>and</em> the repository's format is
 * enabled. Formats that are not enabled hard-delete exactly as before, so their format-specific
 * metadata-rebuild handlers are not required until the format is added to the allowlist.
 * </p>
 *
 * <p>
 * Allowlist semantics (fail-closed): soft-delete is active only for formats explicitly listed in
 * {@code nexus.softDelete.formats}. An empty or unset list means <em>no</em> format is soft-deleted —
 * everything hard-deletes exactly as before — even when the global flag is on. This is deliberate: a
 * format must be opted in only once its soft-delete/restore handlers are validated, so nothing can be
 * enabled by accident. Matching is case-insensitive. The single-entry wildcard {@code *} enables
 * <em>all</em> formats (a maintenance-free "enable everything" switch); the empty default still means
 * none, so the fail-closed behavior is unchanged.
 * </p>
 */
@Component
public class SoftDeleteFormatGate
{
  /** Wildcard allowlist entry meaning "all formats". */
  private static final String ALL_FORMATS = "*";

  private final boolean enabled;

  private final Set<String> enabledFormats;

  @Autowired
  public SoftDeleteFormatGate(
      @Value(SOFT_DELETE_ENABLED_NAMED_VALUE) final boolean enabled,
      @Value(SOFT_DELETE_FORMATS_NAMED_VALUE) final String formats)
  {
    this.enabled = enabled;
    this.enabledFormats = parseFormats(formats);
  }

  /**
   * Whether the soft-delete feature is active at all: the global flag is on <em>and</em> at least one
   * format is enabled. An empty/unset allowlist means the feature is inert (everything hard-deletes), so
   * this returns {@code false}. Callers that gate non-format-specific work (e.g. browse-node cleanup
   * routing) can use this.
   */
  public boolean isEnabled() {
    return enabled && !enabledFormats.isEmpty();
  }

  /**
   * Whether a delete in a repository of the given format should become a soft-delete: the global flag is
   * on and the format is explicitly in the allowlist (or the allowlist is the {@code *} wildcard, meaning
   * all formats). An empty/unset allowlist enables no formats.
   *
   * @param format the repository format value (e.g. {@code maven2}, {@code npm}); {@code null} is not enabled
   */
  public boolean isEnabledForFormat(final String format) {
    if (!enabled || format == null) {
      return false;
    }
    return enabledFormats.contains(ALL_FORMATS)
        || enabledFormats.contains(format.toLowerCase(Locale.ROOT));
  }

  /**
   * The normalized (trimmed, lower-cased) set of allow-listed formats, possibly containing the {@code *}
   * wildcard meaning "all formats". Empty when no format is enabled. Exposed so the frontend can be told
   * which formats are eligible (via {@code SoftDeleteStateContributor}) and mirror the same per-format
   * gate the delete paths enforce, keeping the "moved to Trash" UI copy accurate during a phased rollout.
   */
  public Set<String> getEnabledFormats() {
    return enabledFormats;
  }

  private static Set<String> parseFormats(final String csv) {
    if (csv == null || csv.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(csv.split(","))
        .map(value -> value.trim().toLowerCase(Locale.ROOT))
        .filter(value -> !value.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }
}
