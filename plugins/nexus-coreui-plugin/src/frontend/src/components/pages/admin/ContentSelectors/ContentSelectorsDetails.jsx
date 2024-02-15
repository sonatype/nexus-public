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
import {useMachine} from '@xstate/react';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  ExtJS,
} from '@sonatype/nexus-ui-plugin';

import ContentSelectorsFormMachine from './ContentSelectorsFormMachine';
import ContentSelectorsForm from './ContentSelectorsForm';
import ContentSelectorsReadOnly from './ContentSelectorsReadOnly';

import UIStrings from '../../../../constants/UIStrings';

export default function ContentSelectorDetails({itemId, onDone}) {
  const [state, , service] = useMachine(ContentSelectorsFormMachine, {
    context: {
      pristineData: {
        name: itemId
      }
    },

    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone
    },

    devTools: true
  });

  const {pristineData} = state.context;

  const canUpdate = ExtJS.checkPermission('nexus:selectors:update');

  return <Page className="nxrm-content-selectors">
    <PageHeader>
      <PageTitle text={Boolean(pristineData.name) ?
          UIStrings.CONTENT_SELECTORS.EDIT_TITLE(pristineData.name) :
          UIStrings.CONTENT_SELECTORS.MENU.text}/>
    </PageHeader>
    <ContentBody>
      {(canUpdate || !itemId)
          ? <ContentSelectorsForm service={service} onDone={onDone}/>
          : <ContentSelectorsReadOnly service={service} onDone={onDone}/>
      }
    </ContentBody>
  </Page>;
}
