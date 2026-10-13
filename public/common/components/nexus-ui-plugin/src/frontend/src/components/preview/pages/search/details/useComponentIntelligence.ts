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

import { useEffect, useState } from 'react';

import ExtAPIUtils from '../../../../../interface/ExtAPIUtils';
import { ExtJS } from '../../../../../interface/ExtJS';
import APIConstants from '../../../../../constants/APIConstants';
import Permissions from '../../../../../constants/Permissions';
import { normalizeFormatKey } from './dependencySnippets/registry';
import { parseGaCoordinates } from './detailHelpers';

const { ACTION, METHODS } = APIConstants.EXT.HEALTH_CHECK_INFO;

/**
 * Formats for which the backend builds a component identifier and therefore can return
 * intelligence at all. `AbstractHealthCheckInfoComponent.read` string-compares the format and
 * only calls `ComponentIdentifier.createMavenCoordinates` / `createNugetCoordinates`; every
 * other format is answered with an empty `Info`. Requesting those would be a guaranteed-empty
 * round trip, so this hook does not make it.
 */
export const INTELLIGENCE_SUPPORTED_FORMATS: readonly string[] = ['maven2', 'nuget'];

/**
 * `HealthCheckXO.Info` as serialized by the Ext.Direct component. Every field is optional
 * upstream: a component the insight service has no record of yields an `Info` with nothing
 * but the state flags set.
 */
interface HealthCheckInfoResponse {
  mostPopularVersion?: string | null;
  age?: number | null;
  popularity?: number | null;
  securityAlerts?: number | null;
  criticalSecurityAlerts?: number | null;
  severeSecurityAlerts?: number | null;
  moderateSecurityAlerts?: number | null;
  licenseThreat?: number | null;
  licenseThreatName?: string | null;
  capped?: boolean | null;
  cappedAlerts?: boolean | null;
  disabled?: boolean | null;
  error?: boolean | null;
}

/** One element of the `List<HealthCheckXO>` the Ext.Direct method returns. */
interface HealthCheckXOResponse {
  healthCheck?: HealthCheckInfoResponse | null;
}

/**
 * Resolved intelligence for one component version. Every value is `null` when the insight
 * service has no data for it, so a caller can render each signal independently — a component
 * can have an age and popularity but no security or license data (see {@link ComponentIntelligenceStatus}).
 *
 * `null` means "no data", never "zero": a real 0 age, 0 popularity, or 0 alert count is
 * preserved as 0. The Classic renderers guard with `if (value === 0 || value > 0)` for exactly
 * this reason, and a truthiness check here would report a brand-new or unused component as
 * having no data at all.
 */
export interface ComponentIntelligenceData {
  mostPopularVersion: string | null;
  /** Age of the component in milliseconds, as returned upstream. */
  ageMillis: number | null;
  /** Percentage, unclamped as returned upstream. */
  popularity: number | null;
  /** Aggregate alert count. `null` when the three severity counts are all absent upstream. */
  securityAlerts: number | null;
  criticalSecurityAlerts: number | null;
  severeSecurityAlerts: number | null;
  moderateSecurityAlerts: number | null;
  /** Threat-level enum: 4 critical, 2 moderate, 1 none, anything else severe. */
  licenseThreat: number | null;
  /** The license name to display, e.g. `Apache-2.0`. */
  licenseThreatName: string | null;
  /** Upstream reported the alert quota as reached for this response. */
  cappedAlerts: boolean;
}

/**
 * Mutually exclusive outcomes of resolving component intelligence. Each maps to exactly one
 * rendered state, and the four non-`resolved` request outcomes mirror the precedence of the
 * Classic precondition ladder in `NX.coreui.controller.HealthCheckInfo.renderPreconditions`
 * (loading, then disabled, then error, then capped).
 *
 * - `idle`: nothing requested yet — no version resolved, or the caller disabled the hook.
 * - `unsupported-format`: the format cannot carry intelligence, so no request was made.
 * - `forbidden`: the user lacks `nexus:healthcheck:read`, so no request was made. Classic
 *   removes the whole section in this case rather than showing empty values.
 * - `loading`: the Ext.Direct request is in flight.
 * - `disabled`: Repository Health Check is not enabled on any supported repository, so the
 *   backend answered with `disabled: true` for every component.
 * - `error`: the backend could not reach the insight service, or the Ext.Direct call itself
 *   failed. Recoverable, and distinct from "this component has no data".
 * - `capped`: the insight-service query quota has been reached.
 * - `resolved`: {@link UseComponentIntelligenceResult.data} is populated — possibly only
 *   partially, and possibly with every field `null` if the component is simply unknown upstream.
 */
export type ComponentIntelligenceStatus =
  | 'idle'
  | 'unsupported-format'
  | 'forbidden'
  | 'loading'
  | 'disabled'
  | 'error'
  | 'capped'
  | 'resolved';

/**
 * Whether the security-alert and license-threat fields of a response may be shown, and if not,
 * why. These carry their own authorization and entitlement conditions, independent of the response
 * as a whole: `nexus:healthcheck:read` admits a caller to age and popularity, but Classic's
 * `NX.proui.controller.HealthCheckInfo.renderPreconditions` additionally requires a non-EVAL
 * edition, a valid license, and `nexus:healthcheckalerts:update` before showing either alert field.
 *
 * Order of precedence follows that ladder: entitlement, then permission, then quota.
 *
 * - `unknown`: entitlement cannot be determined yet — the state store has not published a license.
 *   Fails closed, so the fields stay hidden until it does.
 * - `unlicensed`: EVAL edition, or the license is not valid.
 * - `forbidden`: the user lacks `nexus:healthcheckalerts:update`.
 * - `capped`: the alert-specific quota is exhausted (`cappedAlerts`), so the alert fields carry no
 *   value even when age and popularity in the same response do.
 * - `available`: the alert fields may be shown, if the response carries them at all.
 */
export type ComponentAlertsStatus = 'unknown' | 'unlicensed' | 'forbidden' | 'capped' | 'available';

interface UseComponentIntelligenceOptions {
  gaId: string;
  format: string;
  version: string | null;
  /**
   * When false the hook performs no request and stays `idle`. Lets a caller keep calling the
   * hook unconditionally so React's rules of hooks are not violated by an early return.
   */
  enabled?: boolean;
}

export interface UseComponentIntelligenceResult {
  data: ComponentIntelligenceData | null;
  status: ComponentIntelligenceStatus;
  /**
   * Whether {@link ComponentIntelligenceData}'s alert and license fields may be shown. A caller
   * must consult this as well as `status`: the backend populates those fields for anyone holding
   * `nexus:healthcheck:read`, so `data` carrying them does not mean they are permitted.
   */
  alertsStatus: ComponentAlertsStatus;
}

/** Number of milliseconds in a day, matching the Classic renderer's `1000 * 60 * 60 * 24`. */
const MILLIS_PER_DAY = 86400000;

/** `null` unless the value is a real number, so a 0 survives and a null/undefined does not. */
function numberOrNull(value: number | null | undefined): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

/**
 * Age as rendered by Classic: a day count below the 365-day boundary, otherwise years to one
 * decimal place. `null` when there is no age, which the caller renders as unavailable.
 *
 * Ported verbatim from `NX.coreui.controller.HealthCheckInfo.renderAge`, including the `> 364`
 * cutoff (so 365 days reads `1.0 yrs`, not `365 d`) and the zero-preserving guard.
 */
export function formatComponentAge(ageMillis: number | null): string | null {
  if (ageMillis === null || ageMillis < 0) {
    return null;
  }
  const dayAge = ageMillis / MILLIS_PER_DAY;
  return dayAge > 364 ? `${(dayAge / 365).toFixed(1)} yrs` : `${dayAge.toFixed(0)} d`;
}

/**
 * Popularity as rendered by Classic: a percentage clamped to 100. Ported from
 * `renderPopularity`, including both of its bounds — the `popularity === 0 || popularity > 0`
 * guard, so a negative value or sentinel is unavailable rather than rendered as `-1 %`, and the
 * upper cap, which clamps an out-of-range value rather than dropping it.
 */
export function formatComponentPopularity(popularity: number | null): string | null {
  if (popularity === null || popularity < 0) {
    return null;
  }
  return `${Math.min(popularity, 100)} %`;
}

/**
 * Severity of a license threat level. The numbers are a small upstream enum, not a scale —
 * taken from `NX.proui.controller.HealthCheckInfo.renderLicenseThreat`, where anything that is
 * neither 4, 2, nor 1 is treated as severe.
 */
export type LicenseThreatSeverity = 'critical' | 'severe' | 'moderate' | 'none';

export function licenseThreatSeverity(licenseThreat: number): LicenseThreatSeverity {
  if (licenseThreat === 4) return 'critical';
  if (licenseThreat === 2) return 'moderate';
  if (licenseThreat === 1) return 'none';
  return 'severe';
}

/** The `license` state value ExtJS publishes; absent entirely until the state store has loaded. */
interface LicenseState {
  installed?: boolean;
  valid?: boolean;
}

/**
 * Applies the alert-field conditions from Classic's `renderPreconditions`, in its order.
 *
 * Fails closed on an unreadable license state. `NX.State.isLicenseValid()` — the condition Classic
 * checks here — is false unless `installed` and `valid` are both explicitly true, so an absent
 * state value is not entitlement. It is reported as `unknown` rather than `unlicensed` because the
 * two differ in why the fields are hidden: `unknown` is expected during state initialization and
 * resolves on its own once the store publishes, whereas `unlicensed` is a settled verdict. Both
 * hide the fields, so the transient case cannot expose an unlicensed user's alert values, and the
 * distinction never claims "not entitled" about a user whose entitlement was never determined.
 */
function resolveAlertsStatus({
  edition,
  license,
  canReadAlerts,
  cappedAlerts,
}: {
  edition: string | null | undefined;
  license: LicenseState | null | undefined;
  canReadAlerts: boolean;
  cappedAlerts: boolean;
}): ComponentAlertsStatus {
  // Ahead of the license read on purpose: EVAL is a settled verdict on its own — Classic's
  // condition is `'EVAL' === edition || !NX.State.isLicenseValid()`, so no license state can make
  // an EVAL instance entitled, and reporting `unknown` there would imply it might yet become so.
  if (edition === 'EVAL') {
    return 'unlicensed';
  }
  if (license == null || typeof license.installed !== 'boolean') {
    return 'unknown';
  }
  // Mirrors NX.State.isLicenseValid(): installed and valid must both be explicitly true.
  if (!(license.installed === true && license.valid === true)) {
    return 'unlicensed';
  }
  if (!canReadAlerts) {
    return 'forbidden';
  }
  if (cappedAlerts) {
    return 'capped';
  }
  return 'available';
}

/**
 * Hook resolving Repository Health Check component intelligence for one component version:
 * most popular version, age, popularity, security alert counts by severity, and license threat.
 *
 * Source and transport are the ones the Classic component-details panel already uses — the
 * Ext.Direct method `healthcheck_Info.read`, backed by the Sonatype insight service through
 * `HdsService`. It is the only path that exposes these five signals; the health-check REST
 * resources are all repository-level status. Authorization stays where it already is, on the
 * backend: the method carries `@RequiresAuthentication` and
 * `@RequiresPermissions("nexus:healthcheck:read")`. The permission is also checked here, which
 * is not the enforcement boundary — it only avoids issuing a request that is certain to be
 * rejected, and it matches Classic, which hides the section on the same condition.
 *
 * Security and license data additionally depend on the edition and license feature: the
 * Pro/Cloud `HealthCheckInfoComponent` requests alerts, the Community one does not, and
 * `UrlServiceImpl` drops the alerts flag when `isSearchAlertDataSupported()` is false. Those
 * fields then arrive absent, so they resolve to `null` and the caller omits them rather than
 * showing zero counts — which would misreport an unevaluated component as clean.
 *
 * On an edition that does request them, they arrive for any caller holding
 * `nexus:healthcheck:read`, regardless of the alert-specific permission — so a caller must gate
 * them on {@link UseComponentIntelligenceResult.alertsStatus} and not on their presence in `data`.
 * That gate, like Classic's, is applied in the browser and is therefore a restriction rather than
 * an enforcement: the values are in the response either way. Enforcing it properly means checking
 * `nexus:healthcheckalerts:update` in `HealthCheckInfoComponent` before populating the fields,
 * which is a backend change this hook cannot make.
 */
export function useComponentIntelligence({
  gaId,
  format,
  version,
  enabled = true,
}: UseComponentIntelligenceOptions): UseComponentIntelligenceResult {
  const [data, setData] = useState<ComponentIntelligenceData | null>(null);
  const [status, setStatus] = useState<ComponentIntelligenceStatus>('idle');

  // Provider-independent permission read: coreui never mounts a <PermissionsProvider>, so the
  // context-based usePermission would return false for everyone. Reactive, so a permission or
  // user change re-runs the effect below instead of leaving a stale empty section on screen.
  const canRead = ExtJS.usePermission(() => ExtJS.checkPermission(Permissions.HEALTHCHECK.READ), []);

  // The alert and license fields need their own gate: the backend fills them in for any caller
  // holding nexus:healthcheck:read, so without this a restricted user would see them. This is not
  // the enforcement boundary — the Ext.Direct method does not check this permission, so the values
  // still reach the browser — but it applies the same restriction Classic does at the same layer.
  const canReadAlerts = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.HEALTHCHECK_ALERTS.UPDATE),
    [],
  );
  const license = ExtJS.useLicense();
  const alertsStatus = resolveAlertsStatus({
    edition: ExtJS.state().getEdition(),
    license,
    canReadAlerts,
    cappedAlerts: Boolean(data?.cappedAlerts),
  });

  const backendFormat = normalizeFormatKey(format);
  const supportedFormat = INTELLIGENCE_SUPPORTED_FORMATS.includes(backendFormat);

  useEffect(() => {
    // `version` is deliberately truthiness-checked rather than compared to null. Unlike the
    // detail page, where '' is the valid selected version of a versionless format, the insight
    // service identifies a component by coordinates that include a version, so '' has nothing
    // to match and is treated as "nothing to request".
    if (!(enabled && version)) {
      setData(null);
      setStatus('idle');
      return;
    }

    if (!supportedFormat) {
      setData(null);
      setStatus('unsupported-format');
      return;
    }

    if (!canRead) {
      setData(null);
      setStatus('forbidden');
      return;
    }

    let cancelled = false;

    const run = async () => {
      setStatus('loading');
      setData(null);

      const { group, name } = parseGaCoordinates(gaId);

      let info: HealthCheckInfoResponse | null;
      try {
        const response = await ExtAPIUtils.extAPIRequest(ACTION, METHODS.READ, {
          // `data` is the Ext.Direct argument list, so the single `HealthCheckXO[]` parameter
          // is itself one element of it — hence the nested array. Sending the components flat
          // would bind the first component to the method's only parameter and drop the rest.
          data: [[{ group, name, version, format: backendFormat }]],
        });
        const results = ExtAPIUtils.checkForErrorAndExtract(response) as HealthCheckXOResponse[] | null;
        info = results?.[0]?.healthCheck ?? null;
      } catch {
        // The thrown error can carry an insight-service URL or a backend stack trace, so it is
        // deliberately not surfaced. `error` is the same state Classic reaches by setting
        // healthCheckError from a failed Ext.Direct envelope.
        if (cancelled) return;
        setStatus('error');
        return;
      }

      if (cancelled) return;

      if (!info) {
        setStatus('error');
        return;
      }

      // Precedence matches Classic's precondition ladder: a disabled, errored, or capped
      // response is reported as such even when some values happen to have been filled in.
      if (info.disabled) {
        setStatus('disabled');
        return;
      }
      if (info.error) {
        setStatus('error');
        return;
      }
      if (info.capped) {
        setStatus('capped');
        return;
      }

      // `||`, not `??`, on the two string fields: an empty string carries no more information
      // than an absent one, and preserving it would render a blank row and an empty license
      // badge instead of the unavailable placeholder. The numbers use `??` semantics via
      // numberOrNull, which keeps a real 0.
      setData({
        mostPopularVersion: info.mostPopularVersion || null,
        ageMillis: numberOrNull(info.age),
        popularity: numberOrNull(info.popularity),
        securityAlerts: numberOrNull(info.securityAlerts),
        criticalSecurityAlerts: numberOrNull(info.criticalSecurityAlerts),
        severeSecurityAlerts: numberOrNull(info.severeSecurityAlerts),
        moderateSecurityAlerts: numberOrNull(info.moderateSecurityAlerts),
        licenseThreat: numberOrNull(info.licenseThreat),
        licenseThreatName: info.licenseThreatName || null,
        cappedAlerts: Boolean(info.cappedAlerts),
      });
      setStatus('resolved');
    };

    run();

    // Guards against a slower earlier request resolving after a newer one and putting the
    // previous component's or version's intelligence back on screen.
    return () => {
      cancelled = true;
    };
    // `supportedFormat` is deliberately absent: it is derived from `backendFormat` in the render
    // body and cannot change without it, so listing it would suggest it can.
  }, [gaId, backendFormat, version, enabled, canRead]);

  return { data, status, alertsStatus };
}
