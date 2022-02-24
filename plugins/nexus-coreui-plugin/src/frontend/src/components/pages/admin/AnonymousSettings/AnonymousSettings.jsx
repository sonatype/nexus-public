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
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section,
  ExtJS,
} from '@sonatype/nexus-ui-plugin';

import {faUser} from '@fortawesome/free-solid-svg-icons';

import AnonymousSettingsForm from './AnonymousSettingsForm';
import AnonymousSettingsReadOnly from './AnonymousSettingsReadOnly';

import UIStrings from '../../../../constants/UIStrings';

export default function AnonymousSettings() {
  const canEdit = ExtJS.checkPermission('nexus:settings:update');

  return <Page>
    <PageHeader>
      <PageTitle icon={faUser} {...UIStrings.ANONYMOUS_SETTINGS.MENU}/>
    </PageHeader>
    <ContentBody className='nxrm-anonymous-settings'>
      <Section>
        {canEdit ? <AnonymousSettingsForm/> : <AnonymousSettingsReadOnly/>}
      </Section>
    </ContentBody>
  </Page>;
}
