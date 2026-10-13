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

import {ExtJS} from '../interface/ExtJS';

/**
 * Formats supported by Repository Firewall.
 *
 * Source of truth: FirewallSupportedFormat.java
 *
 * NOTE: p2 and raw are in the backend enum but not in official docs
 * (https://help.sonatype.com/en/repository-firewall.html).
 * They are treated as supported until product team clarification.
 *
 * NOT IN THIS SET (show N/A by default): terraform, apt, helm, gitlfs, alpine, ansiblegalaxy, oci.
 * These become configurable when the FIRE-956 limited-support override is on — see
 * isFirewallSupportedFormat. They remain outside this set, and outside
 * isFirewallAnalyzableFormat, because IQ still cannot analyze them. This set itself stays a
 * faithful mirror of the backend enum and must not be widened for that override.
 *
 * 'pub' is runtime-gated on NX.State value 'firewallPubFormatEnabled'.
 * 'swift' is runtime-gated on NX.State value 'swiftFirewallEnabled'.
 */
export const FIREWALL_SUPPORTED_FORMATS = new Set([
  'r',
  'go',
  'p2',
  'rubygems',
  'npm',
  'yum',
  'pypi',
  'cargo',
  'conan',
  'conda',
  'nuget',
  'maven2',
  'composer',
  'cocoapods',
  'huggingface',
  'docker',
  'raw',
  'pub',
  'swift',
]);

const FLAG_GATED_FORMATS: Record<string, string> = {
  pub: 'firewallPubFormatEnabled',
  swift: 'swiftFirewallEnabled',
};

/**
 * NX.State key published by FirewallLimitedSupportFormatsStateContributor (FIRE-956).
 *
 * When true, the backend accepts Firewall configuration on any proxy format, so the UI must stop
 * reporting formats outside FIREWALL_SUPPORTED_FORMATS as unsupported. Off by default.
 */
const LIMITED_SUPPORT_OVERRIDE_STATE_KEY = 'firewallLimitedSupportFormatsEnabled';

function isLimitedSupportOverrideEnabled(): boolean {
  return ExtJS.state()?.getValue(LIMITED_SUPPORT_OVERRIDE_STATE_KEY) === true;
}

/**
 * Shared decision the two exported predicates below have in common: the FLAG_GATED_FORMATS
 * rollout gate, then membership in FIREWALL_SUPPORTED_FORMATS.
 *
 * Returns a boolean when those two rules settle the answer. Returns null for the single case where
 * the two predicates legitimately differ: a non-empty format that carries no rollout flag and is
 * not in the supported set. Each caller supplies its own fallback for that case.
 */
function resolveFormatGate(format: string | null | undefined): boolean | null {
  const key = format?.toLowerCase() ?? '';
  if (!key) {
    return false;
  }
  // Own-property lookup only. A bare index would resolve inherited Object.prototype members, so
  // keys like 'constructor' or 'valueOf' would take the flag-gated branch and stay unsupported even
  // with the blanket override on.
  const flag = Object.prototype.hasOwnProperty.call(FLAG_GATED_FORMATS, key)
    ? FLAG_GATED_FORMATS[key]
    : undefined;
  if (flag) {
    return ExtJS.state()?.getValue(flag) === true;
  }
  if (!FIREWALL_SUPPORTED_FORMATS.has(key)) {
    return null;
  }
  return true;
}

/**
 * UI-VISIBILITY GATE: may the admin see and configure Firewall for this repository format?
 *
 * Answers "will the backend accept a Firewall configuration for this format", which is a broader
 * question than "can Firewall analyze this format" — see isFirewallAnalyzableFormat for the latter.
 * Format-only: independent of tier and license state. Case-insensitive. Returns false for
 * null/undefined/empty.
 *
 * Formats listed in FLAG_GATED_FORMATS are additionally gated on an NX.State feature
 * flag and return false while that flag is off. This is a feature-rollout gate only —
 * it is independent of tier and license state.
 *
 * FIRE-956: when the limited-support override is enabled, set membership is no longer required and
 * any format is reported as supported. The override is deliberately blanket, mirroring the backend
 * validator, so that a format added to NXRM later is not silently excluded. Two properties are
 * preserved regardless of the override:
 *
 *  - FLAG_GATED_FORMATS still applies. 'pub' is inside the supported enum but carries its own
 *    rollout flag and a minimum IQ version, so the override must not make pub Firewall visible when
 *    its own flag is off. 'swift' (NEXUS-53057) has the same shape: in the enum, gated on
 *    'swiftFirewallEnabled'. The blanket override must not rescue either of them.
 *  - An empty or missing format is still unsupported.
 *
 * Use this function to decide whether to show/hide Firewall UI elements (wizard step,
 * edit tab, etc.) for a repository format.
 */
export function isFirewallSupportedFormat(format: string | null | undefined): boolean {
  return resolveFormatGate(format) ?? isLimitedSupportOverrideEnabled();
}

/**
 * CAPABILITY CHECK: can Repository Firewall (IQ) actually analyze this format?
 *
 * Identical to isFirewallSupportedFormat except that the FIRE-956 limited-support override does not
 * apply. The override only lifts the backend's *configuration* restriction; it grants IQ no new
 * analysis capability. A format outside FIREWALL_SUPPORTED_FORMATS is therefore never analyzable,
 * however the override is set.
 *
 * The rollout gate is shared with the visibility gate, not duplicated, so 'swift' and 'pub' report
 * false here too while their own flag is off.
 *
 * Use this function wherever the UI states what protection is actually in effect — Firewall status
 * displays, protection-level copy, and the tier-aware checks in
 * firewallTier.isFirewallEnterpriseSupportedFormat / getFirewallEnterpriseSupportedFormats. Using
 * the visibility gate there would claim a protection level IQ cannot deliver for a limited-support
 * format.
 */
export function isFirewallAnalyzableFormat(format: string | null | undefined): boolean {
  return resolveFormatGate(format) ?? false;
}
