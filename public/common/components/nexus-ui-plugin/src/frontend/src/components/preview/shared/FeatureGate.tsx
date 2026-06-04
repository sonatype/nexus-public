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
import { isFeatureEnabled } from '../config/featureFlags';
import SettingsNotAvailablePage from '../pages/settings/SettingsNotAvailablePage';

/**
 * FeatureGate - Conditionally renders a feature or Settings Not Available page
 *
 * SECURITY MODEL:
 * - Production: Not Available page only. WIP cannot be accessed.
 * - Development: Not Available page with "View WIP" link for testing.
 *
 * Usage in routes:
 *
 * const GatedPrivilegesPage = () => (
 *   <FeatureGate
 *     featureKey="security.privileges"
 *     featureName="Privileges"
 *   >
 *     <PrivilegesPage />
 *   </FeatureGate>
 * );
 *
 * Or use the HOC:
 *
 * const GatedPrivilegesPage = withFeatureGate(PrivilegesPage, 'security.privileges', 'Privileges');
 */

export interface FeatureGateProps {
  featureKey: string;
  featureName: string;
  children: React.ReactNode;
}

export function FeatureGate({ featureKey, featureName, children }: FeatureGateProps) {
  const isEnabled = isFeatureEnabled(featureKey);

  if (isEnabled) {
    return children;
  }

  return <SettingsNotAvailablePage featureName={featureName} />;
}

/**
 * Higher-Order Component version for easier use in routes
 *
 * SECURITY: WIP pages only accessible in development mode.
 *
 * Usage:
 * const GatedPage = withFeatureGate(ActualPage, 'security.privileges', 'Privileges');
 */
export function withFeatureGate<P extends object>(
  WrappedComponent: React.ComponentType<P>,
  featureKey: string,
  featureName: string,
): React.ComponentType<P> {
  const displayName = WrappedComponent.displayName || WrappedComponent.name || 'Component';

  function GatedComponent(props: P) {
    const isEnabled = isFeatureEnabled(featureKey);

    if (isEnabled) {
      return <WrappedComponent {...props} />;
    }

    return <SettingsNotAvailablePage featureName={featureName} />;
  }

  GatedComponent.displayName = `FeatureGate(${displayName})`;
  return GatedComponent;
}

export default FeatureGate;
