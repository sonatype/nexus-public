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
  NxTile,
} from '@sonatype/react-shared-components';
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  ExtJS,
  ValidationUtils,
} from '@sonatype/nexus-ui-plugin';

import Machine from './PrivilegesFormMachine';
import PrivilegesForm from './PrivilegesForm';
import PrivilegesReadOnly from './PrivilegesReadOnly';

import UIStrings from '../../../../constants/UIStrings';

const {PRIVILEGES: {FORM: LABELS}} = UIStrings;

export default function PrivilegesDetails({itemId, onDone}) {
  const hasDeletePermissions = ExtJS.checkPermission('nexus:privileges:delete');
  const hasEditPermissions = ExtJS.checkPermission('nexus:privileges:update');

  const [current, , service] = useMachine(Machine, {
    context: {
      pristineData: {
        name: itemId,
      }
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },
    guards: {
      canDelete: () => hasDeletePermissions,
    },
    devTools: true,
  });

  const {data: {readOnly}, pristineData} = current.context;

  const canEdit = hasEditPermissions && !readOnly;
  const isEdit = ValidationUtils.notBlank(itemId);
  const showReadOnly = isEdit && !canEdit;

  return <Page className="nxrm-privileges">
    <PageHeader>
      <PageTitle
          text={isEdit ? LABELS.EDIT_TILE(pristineData.name || '') : LABELS.CREATE_TITLE}
          description={isEdit ? LABELS.EDIT_DESCRIPTION : null}
      />
    </PageHeader>
    <ContentBody className="nxrm-privileges-form">
      <NxTile>
        {showReadOnly
            ? <PrivilegesReadOnly service={service} onDone={onDone}/>
            : <PrivilegesForm itemId={itemId} service={service} onDone={onDone}/>
        }
      </NxTile>
    </ContentBody>
  </Page>;
}
