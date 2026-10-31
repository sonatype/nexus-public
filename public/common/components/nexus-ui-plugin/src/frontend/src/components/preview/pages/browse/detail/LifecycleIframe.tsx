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

import React, { useCallback } from 'react';
import { Box, Flex, Text } from '@radix-ui/themes';
import { Shield } from 'lucide-react';

import ExtJS from '../../../../../interface/ExtJS';
import ExtAPIUtils from '../../../../../interface/ExtAPIUtils';
import Permissions from '../../../../../constants/Permissions';

/**
 * Embedded Sonatype Lifecycle (IQ Server) Health Check report for a single asset — the React port
 * of Classic's `NX.proui.view.healthcheck.HealthCheckAssetDetail` (NEXUS-54501).
 *
 * The report itself is served same-origin by Nexus at
 * `service/rest/healthcheck/healthCheckFileDetail/{base64(path)}/{base64(repo)}/index.html`, so the
 * iframe can reach into its `contentWindow.Insight` bridge once it loads. On load we identify the
 * component (Ext.Direct `healthcheck_AssetDetail.identify`) and hand the coordinates to Insight so it
 * renders the application-selection + policy report, exactly as Classic does.
 */

// =============================================================================
// URL + ENCODING
// =============================================================================

/**
 * Standard base64 of the UTF-8 bytes of `input`, matching `NX.util.Base64.encode` (standard
 * alphabet, `+/=`). Percent-escaped UTF-8 is folded back to raw bytes rather than using the
 * deprecated `unescape`, so multi-byte paths encode correctly.
 */
function base64Encode(input: string): string {
  // encodeURIComponent → percent-escaped UTF-8; the replace turns each %XX back into its raw byte,
  // giving the binary string btoa expects. This is the standard TextEncoder-free UTF-8 base64 idiom.
  const binary = encodeURIComponent(input).replace(/%([0-9A-F]{2})/g, (_match, hex) =>
    String.fromCharCode(parseInt(hex, 16)),
  );
  return btoa(binary);
}

/**
 * Builds the same-origin Health Check report URL Classic embeds: the asset path and repository name
 * are base64-encoded path segments and the repository format is a query parameter.
 *
 * Each base64 segment is additionally percent-encoded. The standard alphabet contains `/`, which a
 * path or repository name emits for real inputs (any byte 0x3F at an index 2 mod 3 — a literal `?` —
 * and much of the non-ASCII range, e.g. Cyrillic). A raw `/` would shift the server's
 * `/{base64Uri}/{base64Repository}/{path:.*}` template, so `Strings2.decodeBase64` would be handed a
 * fragment and the report would fail instead of rendering. The distributions run Jetty with
 * `UriCompliance` LEGACY plus `decodeAmbiguousURIs`, so `%2F` inside a segment is matched as data and
 * `@PathParam` hands the resource the original base64 (NEXUS-53113 configured this for npm scopes).
 * Classic interpolates the raw base64 and carries the same latent defect.
 */
export function buildHealthCheckFileDetailUrl({
  path,
  repositoryName,
  format,
}: {
  path: string;
  repositoryName: string;
  format: string;
}): string {
  const relative =
    'service/rest/healthcheck/healthCheckFileDetail' +
    `/${encodeURIComponent(base64Encode(path))}` +
    `/${encodeURIComponent(base64Encode(repositoryName))}` +
    `/index.html?format=${encodeURIComponent(format)}`;
  return ExtJS.urlOf(relative);
}

// =============================================================================
// INSIGHT BRIDGE
// =============================================================================

/** The component identity returned by `healthcheck_AssetDetail.identify`. */
export interface HealthCheckComponentIdentity {
  componentIdentifier: {
    format: string;
    coordinates: Record<string, string>;
  };
  hash: string;
}

/**
 * The subset of the IQ Server `Insight` bridge the report page exposes on its window. Modern IQ
 * exposes `setCoordinates`/`registerCoordsViewDetailsListener`; a legacy server only has the
 * Maven-only `setGav`/`registerViewDetailsListener`. Everything is optional so a defensive guard
 * decides which path is available at runtime.
 */
interface InsightBridge {
  setCoordinates?: (format: string, coordinates: Record<string, string>, opts: { hash: string }) => void;
  setGav?: (gav: Record<string, string | undefined>) => void;
  registerCoordsViewDetailsListener?: (
    cb: (appId: string, format: string, coordinates: string[], hash: string) => void,
  ) => void;
  registerViewDetailsListener?: (
    cb: (
      appId: string,
      groupId: string,
      artifactId: string,
      version: string,
      classifier: string,
      extension: string,
      hash: string,
    ) => void,
  ) => void;
  setError?: (err: { errorMessage: string; errorCode: number }) => void;
}

/** Default identify transport: Ext.Direct `healthcheck_AssetDetail.identify(repositoryName, path)`. */
async function defaultIdentify(repositoryName: string, path: string): Promise<HealthCheckComponentIdentity> {
  const response = await ExtAPIUtils.extAPIRequest('healthcheck_AssetDetail', 'identify', {
    data: [repositoryName, path],
  });
  return ExtAPIUtils.checkForErrorAndExtract(response) as HealthCheckComponentIdentity;
}

/** A modern bridge is one that understands coordinates; otherwise treat it as legacy (Maven-only). */
function isModernInsight(insight: InsightBridge): boolean {
  return Boolean(insight.registerCoordsViewDetailsListener || insight.setCoordinates);
}

/**
 * Hands the identified component to the IQ Server `Insight` bridge inside the loaded report iframe
 * and wires the "view details" listener to open IQ's detail page. Ports the coordinate/legacy-GAV
 * branching from Classic `setClmComponent` / `registerClmViewDetailsListener`.
 *
 * `identify` and `openWindow` are injected so the branching is unit-testable without a live IQ
 * server or a real iframe. Any failure is reported back onto the bridge (Classic parity) and never
 * thrown, so a broken bridge can never take down the surrounding panel.
 */
export async function bridgeInsight(
  insight: InsightBridge,
  {
    repositoryName,
    path,
    identify = defaultIdentify,
    openWindow = (url: string) => window.open(url, '_blank', 'noopener,noreferrer'),
  }: {
    repositoryName: string;
    path: string;
    identify?: (repositoryName: string, path: string) => Promise<HealthCheckComponentIdentity>;
    openWindow?: (url: string) => void;
  },
): Promise<void> {
  let identity: HealthCheckComponentIdentity;
  try {
    identity = await identify(repositoryName, path);
  } catch (err) {
    insight.setError?.({
      errorMessage: `Unable to identify component: ${err instanceof Error ? err.message : String(err)}`,
      errorCode: 400,
    });
    return;
  }

  const { format, coordinates } = identity.componentIdentifier;
  const { hash } = identity;

  if (isModernInsight(insight)) {
    insight.setCoordinates?.(format, coordinates, { hash });
    insight.registerCoordsViewDetailsListener?.((appId, listenerFormat, listenerCoords, listenerHash) => {
      const component: { format: string; coordinates: Record<string, string> } = {
        format: listenerFormat,
        coordinates: {},
      };
      // Insight hands coordinates back as a flat [key, value, key, value, …] array.
      if (listenerCoords) {
        for (let i = 0; i < listenerCoords.length; i += 2) {
          component.coordinates[listenerCoords[i]] = listenerCoords[i + 1];
        }
      }
      // Classic builds the query unconditionally but only opens the window when Insight actually
      // supplied coordinates; without them `viewdetails.html` has no component to resolve.
      if (!listenerCoords) {
        return;
      }
      const query = new URLSearchParams({
        appId,
        hash: listenerHash,
        componentIdentifier: JSON.stringify(component),
      });
      openWindow(ExtJS.urlOf(`viewdetails.html?${query.toString()}`));
    });
    return;
  }

  // Legacy IQ (pre-1.13): Maven coordinates only.
  if (format !== 'maven') {
    insight.setError?.({
      errorMessage: 'Unsupported repository format, please upgrade to Nexus IQ Server 1.13 or greater.',
      errorCode: 400,
    });
    return;
  }
  // Exactly the key set Classic's setLegacyClmComponent sends. Spreading `coordinates` instead
  // would hand a legacy bridge format-specific extras it does not understand.
  insight.setGav?.({
    groupId: coordinates.groupId,
    artifactId: coordinates.artifactId,
    version: coordinates.version,
    extension: coordinates.extension,
    classifier: coordinates.classifier,
    hash,
  });
  insight.registerViewDetailsListener?.((appId, groupId, artifactId, version, classifier, extension, listenerHash) => {
    const query = new URLSearchParams({
      appId,
      hash: listenerHash,
      groupId,
      artifactId,
      version,
      classifier,
      extension,
    });
    openWindow(ExtJS.urlOf(`viewdetails.html?${query.toString()}`));
  });
}

// =============================================================================
// COMPONENT
// =============================================================================

/** The `license` state value; absent entirely until the state store has published one. */
interface LicenseState {
  installed?: boolean;
  valid?: boolean;
}

/**
 * Mirrors `NX.State.isLicenseValid()`: `installed` and `valid` must both be explicitly true, so an
 * absent or half-published license state is not entitlement.
 */
function isLicenseValid(license: LicenseState | null | undefined): boolean {
  return license?.installed === true && license?.valid === true;
}

/**
 * Whether the per-asset Sonatype Lifecycle report may be shown, porting Classic's
 * `NX.proui.controller.HealthCheckAssetDetail.isHealthCheckEnabled()` together with the
 * `componentId` check its two `updated` handlers apply before adding the panel at all.
 *
 * Deliberately *not* `clm.enabled`: that state value describes the IQ Server *connection*, which is
 * neither necessary nor sufficient here. `HealthCheckFileDetailResource` is `@ConditionalOnEdition(pro,
 * cloud)`, requires `nexus:healthcheckalerts:read`, and serves the report from Repository Health Check
 * data when IQ is not connected — so gating on `clm.enabled` both hides a working report and, worse,
 * shows the tab to users the backend will answer with a 403/licence error page inside the iframe.
 *
 * Every input is read reactively so a login, permission change, or late-arriving license flips the
 * tab on or off rather than leaving a dead one on screen. Fails closed while state is unresolved.
 */
export function useLifecycleEntitlement(componentId?: string | null): boolean {
  const user = ExtJS.useUser();
  const license = ExtJS.useLicense() as LicenseState | null | undefined;
  const canReadAlerts = ExtJS.usePermission(
    () => ExtJS.checkPermission(Permissions.HEALTHCHECK_ALERTS.READ),
    [],
  );
  const edition = ExtJS.state().getEdition();

  return (
    Boolean(user) &&
    canReadAlerts &&
    edition !== 'EVAL' &&
    isLicenseValid(license) &&
    // Classic: `Boolean(asset.get('componentId') && asset.get('componentId').trim())`. A blob with
    // no owning component has nothing for the insight service to identify.
    Boolean(componentId?.trim())
  );
}

export interface LifecycleIframeProps {
  /** Repository that actually holds the asset content (`containingRepositoryName`, Classic parity). */
  repositoryName: string;
  /** Asset path (the ExtDirect asset `name`). */
  path: string;
  /** Repository format. */
  format: string;
  /** Owning component id; the report cannot be identified without one (Classic parity). */
  componentId?: string | null;
}

/**
 * Renders the embedded Sonatype Lifecycle report for an asset, or a graceful fallback when the user
 * or instance is not entitled to it, or the asset cannot be located.
 *
 * Classic removes the panel entirely when not entitled; the React tab list does the same, so this
 * fallback is only reached by a direct `?tab=lifecycle` link or a mid-session entitlement change.
 */
export function LifecycleIframe({
  repositoryName,
  path,
  format,
  componentId,
}: LifecycleIframeProps): JSX.Element {
  const entitled = useLifecycleEntitlement(componentId);
  const configured = entitled && Boolean(path && repositoryName && format);

  const handleLoad = useCallback(
    (event: React.SyntheticEvent<HTMLIFrameElement>) => {
      // The report is same-origin, so its window (and the Insight bridge it defines) is reachable.
      // Guarded because a cross-origin or not-yet-ready frame throws on access — in which case there
      // is simply no bridge to wire and the report still renders on its own.
      let insight: InsightBridge | undefined;
      try {
        insight = (event.currentTarget.contentWindow as unknown as { Insight?: InsightBridge })?.Insight;
      } catch {
        insight = undefined;
      }
      if (insight) {
        void bridgeInsight(insight, { repositoryName, path });
      }
    },
    [repositoryName, path],
  );

  if (!configured) {
    return (
      <Flex direction="column" align="center" justify="center" p="6" gap="2">
        <Shield size={32} color="var(--gray-9)" />
        <Text color="gray" size="2">
          Sonatype Lifecycle data is not available for this asset.
        </Text>
      </Flex>
    );
  }

  return (
    <Box p="3" style={{ height: '600px' }}>
      <iframe
        title="Sonatype Lifecycle"
        src={buildHealthCheckFileDetailUrl({ path, repositoryName, format })}
        onLoad={handleLoad}
        style={{ width: '100%', height: '100%', border: 0 }}
      />
    </Box>
  );
}

export default LifecycleIframe;
