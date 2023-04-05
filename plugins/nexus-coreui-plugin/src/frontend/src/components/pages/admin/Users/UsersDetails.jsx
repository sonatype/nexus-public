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

import Machine from './UsersFormMachine';
import UsersForm from './UsersForm';
import UsersReadOnly from './UsersReadOnly';
import UsersToken from './UsersToken';

import UIStrings from '../../../../constants/UIStrings';
import {parseIdParameter, fullName, sourceLabel} from './UsersHelper';

const {USERS: {FORM: LABELS}} = UIStrings;

export default function UsersDetails({itemId, onDone}) {
  const hasDeletePermission = ExtJS.checkPermission('nexus:users:delete');
  const canEdit = ExtJS.checkPermission('nexus:users:update');

  const {source, userId} = parseIdParameter(itemId);

  const [current, , service] = useMachine(Machine, {
    context: {
      pristineData: {source, userId},
      hasDeletePermission,
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },
    guards: {
      canDelete: () => hasDeletePermission,
    },
    devTools: true,
  });

  const {data: {firstName, lastName}, pristineData: {userId: id}} = current.context;

  const isEdit = ValidationUtils.notBlank(id);
  const showReadOnly = isEdit && !canEdit;
  const isPro = ExtJS.isProEdition();

  return <Page className="nxrm-user">
    <PageHeader>
      <PageTitle
          text={isEdit ? LABELS.EDIT_TILE(fullName({firstName, lastName})) : LABELS.CREATE_TITLE}
          description={LABELS.EDIT_DESCRIPTION(sourceLabel(source))}
      />
    </PageHeader>
    <ContentBody className="nxrm-user-form">
      <NxTile>
        {showReadOnly
            ? <UsersReadOnly service={service} onDone={onDone}/>
            : <UsersForm service={service} onDone={onDone}/>
        }
      </NxTile>
      {isPro && isEdit &&
        <NxTile>
          <UsersToken service={service} />
        </NxTile>
      }
    </ContentBody>
  </Page>;
}
