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
import {ExtJS} from '@sonatype/nexus-ui-plugin';
import {useRouter} from '@uirouter/react';
import {NxTextLink} from '@sonatype/react-shared-components';
import {faShieldAlt} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section
} from '@sonatype/nexus-ui-plugin';

import IqServerForm from './IqServerForm';
import IqServerReadOnly from './IqServerReadOnly';

import UIStrings from '../../../../constants/UIStrings';
import {ROUTE_NAMES} from '../../../../routerConfig/routeNames/routeNames';

export default function IqServer() {
  const canEdit = ExtJS.checkPermission('nexus:settings:update');
  const router = useRouter();

  // Show the breadcrumb only when the user is editing an existing IQ Server
  // connection (i.e. navigated here from the Connected/already-configured page).
  const fromConnected = router.stateService.params.fromConnected === true ||
                        router.stateService.params.fromConnected === 'true';

  function navigateToConnected(e) {
    e.preventDefault();
    router.stateService.go(ROUTE_NAMES.ADMIN.IQ.CONNECTED);
  }

  if (!ExtJS.useUser()) {
    return null;
  }

  return <Page>
    <PageHeader>
      <div className="iq-server-page-header">
        {fromConnected &&
        <nav className="iq-server-breadcrumb" aria-label="Breadcrumb">
          <NxTextLink href="#" onClick={navigateToConnected}>
            {UIStrings.IQ_SERVER.MENU.text}
          </NxTextLink>
          <span className="iq-server-breadcrumb-separator"> / </span>
          <span className="iq-server-breadcrumb-current">
            {UIStrings.IQ_SERVER.CONNECTION_SETTINGS_BREADCRUMB}
          </span>
        </nav>}
        <PageTitle
            icon={UIStrings.IQ_SERVER.MENU.icon}
            text={UIStrings.IQ_SERVER.MENU.text}
            description={UIStrings.IQ_SERVER.MENU.description}
        />
      </div>
    </PageHeader>
    <ContentBody className="nxrm-iq-server">
      <Section>
        {canEdit ? <IqServerForm/> : <IqServerReadOnly/>}
      </Section>
    </ContentBody>
  </Page>;
}
