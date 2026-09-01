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

import React, { useState } from 'react';
import { Button } from '@radix-ui/themes';
import { ExtJS } from '../../../interface/ExtJS';
import PropTypes from 'prop-types';
import UIStrings from '../../../constants/UIStrings';
import { useRouter } from '@uirouter/react';
import { RouteNames } from "../../../constants/RouteNames";
import { resolveDefaultLandingRoute, returnToTargetsClassicLanding, PREVIEW_WELCOME_ROUTE, PREVIEW_WELCOME_HASH, USER_REQUESTED_LEGACY_KEY } from '../../../utils/loginUtils';

const { SSO_BUTTON, SSO_BUTTON_CLOUD, SSO_BUTTON_LOADING } = UIStrings;

/**
 * SSO Login Button component that redirects to the appropriate authentication endpoint
 * based on the configured SSO method (SAML or OAuth2/OIDC)
 *
 * @param {Object} props - Component props
 * @param {boolean} props.autoFocus - If true, button receives focus on mount (default: true)
 */
export default function SsoLogin({ autoFocus = true }) {
  const samlEnabled = ExtJS.useState(() => ExtJS.state().getValue('samlEnabled', false));
  const oauth2Enabled = ExtJS.useState(() => ExtJS.state().getValue('oauth2Enabled', false));
  const contextPath = ExtJS.useState(() => ExtJS.state().getValue('nexus-context-path', ''));
  const isCloud = ExtJS.useState(() => ExtJS.state().getValue('isCloud', false));
  const contextPrefix = contextPath === '/' ? '' : contextPath;
  const [isRedirecting, setIsRedirecting] = useState(false);
  const router = useRouter();

  const buildUrlReturnTo = (returnToParam) => {
    const returnTo = returnToParam ? `hash=${encodeURIComponent(returnToParam)}` : '';

    let basePath = '';
    if (samlEnabled) {
      basePath = `${contextPrefix}/saml`;
    } else if (oauth2Enabled) {
      basePath = `${contextPrefix}/oidc/login`;
    } else {
      throw new Error('No SSO method is enabled');
    }

    return `${basePath}?${returnTo}`;
  }

  const handleSsoLogin = () => {
    setIsRedirecting(true);
    try {
      // A fresh login starts a new session: reset any in-session "use Classic UI"
      // preference so the defaultToPreviewUi setting is honored again (NEXUS-53957).
      sessionStorage.removeItem(USER_REQUESTED_LEGACY_KEY);
      const state = ExtJS.state();
      const landingRoute = resolveDefaultLandingRoute({
        defaultToPreviewUi: state?.getValue('defaultToPreviewUi', false) ?? false,
        canAccessPreviewUi: state?.getValue('loggedInEnabled', false) ?? false,
        userRequestedLegacy: false
      });
      let returnToParam = router.globals.params.returnTo;
      if (landingRoute === PREVIEW_WELCOME_ROUTE && returnToTargetsClassicLanding(returnToParam)) {
        // "Default to Nexus One UI": override a missing or Classic landing returnTo with the
        // Preview dashboard, but preserve Classic deep links (matches the GlobalHeaderRadix
        // soft default). SSO backends (SAML/OIDC filters) Base64-decode the hash param.
        returnToParam = btoa(PREVIEW_WELCOME_HASH);
      }
      window.location.assign(buildUrlReturnTo(returnToParam));
    } catch (ex) {
      setIsRedirecting(false);
      console.warn('redirection unsuccessful: ', ex);
      router.stateService.go(RouteNames.MISSING_ROUTE);
    }
  };

  const buttonText = isCloud ? SSO_BUTTON_CLOUD : SSO_BUTTON;

  return (
    <Button
      type="button"
      size="3"
      variant="solid"
      onClick={handleSsoLogin}
      disabled={isRedirecting}
      className="login-submit"
      data-analytics-id="nxrm-login-sso"
      autoFocus={autoFocus}
    >
      {isRedirecting ? SSO_BUTTON_LOADING : buttonText}
    </Button>
  );
}

SsoLogin.propTypes = {
  autoFocus: PropTypes.bool
};
