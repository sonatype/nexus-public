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

import React from 'react';
import type { FirewallSecurityPagesTier } from '../../shared/useFirewallSecurityPagesTier';
import type { FirewallSecurityTab } from './firewallSecurityTabs';
import { ProUpsellContext } from './tabs/ProUpsellContext';

export interface FirewallSecurityTabPanelProps {
  tab: FirewallSecurityTab;
  tier: FirewallSecurityPagesTier;
}

/**
 * Picks the tab content for the tier. Live is mounted only on the live tier, so no live data is requested
 * from a preview tier. Preview views show upgrade prompts only on 'preview-pro'.
 */
export function FirewallSecurityTabPanel({ tab, tier }: FirewallSecurityTabPanelProps): JSX.Element {
  const Content = tier === 'live' ? tab.Live : tab.Preview;
  return (
    <ProUpsellContext.Provider value={tier === 'preview-pro'}>
      <Content />
    </ProUpsellContext.Provider>
  );
}

export default FirewallSecurityTabPanel;
