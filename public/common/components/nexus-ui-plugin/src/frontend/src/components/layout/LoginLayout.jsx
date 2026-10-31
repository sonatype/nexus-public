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

import React from "react";
import { Badge, Flex, Theme } from '@radix-ui/themes';
import { ExtJS } from '../../interface/ExtJS';
import { EDITION_ID_STATE_KEY, getEditionDisplayName, getHeaderWordmarkLogo } from '../../interface/editionUtils';

import './LoginLayout.scss';

/**
 * Minimal layout for login page with only branding/logo in header.
 * No navigation menu, theme switcher, or other header components.
 * Always uses light theme for login page.
 * The logo is the neutral wordmark; the licensed edition is shown as a Radix
 * Badge next to it, matching the global header (getHeaderWordmarkLogo +
 * getEditionDisplayName). The header is wrapped in a scoped light Radix Theme
 * so it can use Radix components directly rather than hand-rolled CSS.
 */
export default function LoginLayout({ children }) {

  const editionId = ExtJS.useState?.(() => ExtJS.state()?.getValue?.(EDITION_ID_STATE_KEY));
  const contextPath = ExtJS.useState?.(() => ExtJS.state()?.getValue?.('nexus-context-path', '')) ?? '';

  function getLogo() {
    // Login page is always light theme.
    return getHeaderWordmarkLogo(false);
  }

  function getEditionText() {
    return getEditionDisplayName(editionId);
  }

  const homeHash = '#browse/welcome';
  const homeHref = contextPath ? `${contextPath}/${homeHash}` : `/${homeHash}`;

  const handleLogoClick = (e) => {
    e.preventDefault();
    window.location.hash = homeHash;
  };

  return (
    <div className="nxrm-login-layout">
      <header className="nxrm-login-header" role="banner">
        <Theme appearance="light" hasBackground={false} className="nxrm-login-header__theme">
          <Flex align="center" gap="2" className="nxrm-login-header__brand">
            <a
              href={homeHref}
              className="nxrm-login-header__link"
              title="Home"
              aria-label="Sonatype Nexus Repository home"
              onClick={handleLogoClick}
            >
              <img
                src={getLogo()}
                alt="Sonatype Nexus Repository"
                className="nxrm-login-header__logo"
                width={220}
                height={32}
                decoding="async"
              />
            </a>
            {Boolean(editionId) && (
              <Badge
                color="green"
                variant="solid"
                size="1"
                className="nxrm-login-header__edition-badge"
              >
                {getEditionText()}
              </Badge>
            )}
          </Flex>
        </Theme>
      </header>
      <main className="nxrm-login-main">
        {children}
      </main>
    </div>
  );
}
