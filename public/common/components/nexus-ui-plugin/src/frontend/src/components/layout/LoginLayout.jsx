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
import { ExtJS } from '@sonatype/nexus-ui-plugin';

import './LoginLayout.scss';

// Edition constants - must match backend values
const EDITION_COMMUNITY = "COMMUNITY";
const EDITION_PRO = "PRO";

/**
 * Minimal layout for login page with only branding/logo in header.
 * No navigation menu, theme switcher, or other header components.
 * Always uses light theme for login page.
 */
export default function LoginLayout({ children, logoConfig }) {

  const edition = ExtJS.useState(() => ExtJS.state().getEdition());
  const contextPath = ExtJS.useState(() => ExtJS.state().getValue('nexus-context-path', ''));

  function getLogo() {
    // Light-only theme, so we only use light logos
    return edition === EDITION_COMMUNITY ? (logoConfig?.ceLight || logoConfig?.proLight)
        : edition === EDITION_PRO ? logoConfig?.proLight
        : (logoConfig?.coreLight || logoConfig?.proLight); // Core or fallback to pro
  }

  function getEditionText() {
    return edition === EDITION_COMMUNITY ? "Community"
        : edition === EDITION_PRO ? "Professional"
        : "Core";
  }

  const homeHref = contextPath ? `${contextPath}/#browse/welcome` : "/#browse/welcome";

  return (
    <div className="nxrm-login-layout">
      <header className="nxrm-login-header" role="banner">
        <a
          href={homeHref}
          className="nxrm-login-header__link"
          title="Home"
          aria-label={`Sonatype Nexus Repository ${getEditionText()} home`}
        >
          <img
            src={getLogo()}
            alt={`Sonatype Nexus Repository ${getEditionText()}`}
            className="nxrm-login-header__logo"
            width={220}
            height={32}
            decoding="async"
          />
        </a>
      </header>
      <main className="nxrm-login-main">
        {children}
      </main>
    </div>
  );
}
