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
import { Page } from '@sonatype/nexus-ui-plugin';
import {
  NxDivider,
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxP,
  NxTextLink
} from '@sonatype/react-shared-components';
import { faBox, faGhost } from '@fortawesome/free-solid-svg-icons';

import { useSref } from '@uirouter/react';
import { ROUTE_NAMES } from '../../../routerConfig/routeNames/routeNames';

export function MissingRoutePage()  {
  const { href: dashboardHref } = useSref(ROUTE_NAMES.BROWSE.WELCOME.ROOT);

  return <Page>
    <div className="nxrm-missing-route-page-content">
      <section className="nxrm-missing-route-page-content-message">
        <GhostAnimation/>

        <NxH1>404</NxH1>
        <NxH2>RESOURCE NOT FOUND</NxH2>

        <NxDivider/>

        <NxP>
          This resource is not available. It may have been moved, deleted, or you don’t have access.
        </NxP>
      </section>

        <section className="nxrm-missing-route-page-actions">

          <NxP className="nxrm-missing-route-page-actions-message">
            You might want to:
          </NxP>

          <div>
            <NxTextLink
                data-analytics-id="nxrm-404-page-dashboard-lnk"
                className="nx-btn"
                href={dashboardHref}
            >
              Return to Dashboard
            </NxTextLink>
            <NxTextLink
                data-analytics-id="nxrm-404-page-support-lnk"
                className="nx-btn"
                href="https://links.sonatype.com/products/nexus/docs?utm_medium=product&utm_source=nexus_repo&utm_campaign=menu-docs"
                external
            >
              Visit Documentation
            </NxTextLink>
          </div>
        </section>
    </div>
  </Page>
}

function GhostAnimation() {
  return <div className="nxrm-ghost-animation">
    <span className="fa-layers fa-fw">
      <NxFontAwesomeIcon className="nxrm-404-box" icon={faBox} />
      <NxFontAwesomeIcon className="nxrm-404-ghost" icon={faGhost} />
    </span>
  </div>
}
