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

import React, { ReactNode } from 'react';
import type { FirewallSecurityPagesTier } from '../../shared/useFirewallSecurityPagesTier';
import { IqConnectionError } from './banners/IqConnectionError';
import { NoIqBanner } from './banners/NoIqBanner';
import { ProPreviewBanner } from './banners/ProPreviewBanner';
import { UpgradeIqNotice } from './banners/UpgradeIqNotice';

export interface FirewallSecurityPageShellProps {
  tier: FirewallSecurityPagesTier;
  /** Minimum supported IQ Server version, shown by the upgrade notice. */
  minIqVersion?: string | null;
  children?: ReactNode;
}

/**
 * Frames the Security tabs for the current tier. Children (the tabs) are mounted only where they make sense:
 * preview tiers (with a banner) and live. In upgrade-iq and error only the notice is rendered.
 */
export function FirewallSecurityPageShell({
  tier,
  minIqVersion,
  children,
}: FirewallSecurityPageShellProps): JSX.Element | null {
  switch (tier) {
    case 'preview-pro':
      return (
        <>
          <ProPreviewBanner />
          {children}
        </>
      );
    case 'preview-no-iq':
      return (
        <>
          <NoIqBanner />
          {children}
        </>
      );
    case 'upgrade-iq':
      return <UpgradeIqNotice minIqVersion={minIqVersion} />;
    case 'error':
      return <IqConnectionError />;
    case 'live':
      return <>{children}</>;
    default:
      return null;
  }
}

export default FirewallSecurityPageShell;
