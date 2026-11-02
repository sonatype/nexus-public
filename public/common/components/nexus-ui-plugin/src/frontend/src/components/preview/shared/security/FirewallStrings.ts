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

/**
 * User-facing strings for the Firewall cell and dialog (NEXUS-54499).
 * Technical values (endpoint paths, mode enum values) stay inline in code.
 */
export default {
  FIREWALL: {
    cell: {
      inactive: 'Inactive',
      inactiveTooltip: 'Firewall is enabled but not active',
      unknown: 'Unavailable',
      unknownTooltip: 'Firewall status could not be determined',
    },
    dialog: {
      inactiveHeading: 'Inactive',
      inactiveBody: 'Firewall is enabled for this repository but is not currently active.',
      countsUnavailableHeading: 'Unavailable',
      countsUnavailableBody:
        'Violation counts could not be loaded. The IQ Server may be unreachable.',
    },
    picker: {
      title: 'Enable Firewall Protection',
      loading: 'Loading current Firewall mode...',
      loadFailed: 'Could not load the current Firewall mode. Nothing was changed.',
      retry: 'Retry',
      currentMode: (mode: string): string => `Current mode: ${mode}`,
      alreadyEnabled: (mode: string): string =>
        `Firewall is already enabled in ${mode} mode. Choose a mode to switch to.`,
      chooseMode: 'Choose a Firewall mode for this repository.',
      confirmEnable: 'Enable',
      confirmSwitch: 'Switch mode',
      cancel: 'Cancel',
      modes: {
        AUDIT: {
          label: 'Audit',
          description: 'Evaluate components and report violations without blocking downloads.',
        },
        QUARANTINE: {
          label: 'Quarantine',
          description: 'Evaluate components and block downloads that violate policy.',
        },
        PCCS: {
          label: 'PCCS',
          description: 'Quarantine plus metadata filtering of blocked components.',
        },
      },
      enabledToast: (repo: string, mode: string): string =>
        `Firewall protection enabled for ${repo} in ${mode} mode`,
      switchedToast: (repo: string, from: string, to: string): string =>
        `Firewall mode for ${repo} changed from ${from} to ${to}`,
      failedToast: (message: string): string => `Failed to enable Firewall protection: ${message}`,
    },
  },
};
