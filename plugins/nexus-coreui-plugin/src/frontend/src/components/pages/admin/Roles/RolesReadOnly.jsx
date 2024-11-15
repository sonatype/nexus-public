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
import {useActor} from '@xstate/react';
import {indexBy, prop} from 'ramda';

import {
  NxButton,
  NxH2,
  NxReadOnly,
  NxLoadWrapper,
  NxInfoAlert,
  NxList,
  NxFooter,
  NxButtonBar,
  NxTile,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

const {ROLES: {FORM: LABELS}} = UIStrings;

export default function RolesReadOnly({service, onDone}) {
  const [current, send] = useActor(service);

  const {
    data: {id, name, description, readOnly:isDefaultRole = true, roles:selectedRoles, privileges},
    roles,
    loadError
  } = current.context;

  const rolesMap = indexBy(prop('id'), roles || []);

  const isLoading = current.matches('loading');

  const cancel = () => onDone();

  const retry = () => send({type: 'RETRY'});

  return <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
    <NxTile.Content>
      <NxInfoAlert>
        {isDefaultRole ? LABELS.DEFAULT_ROLE_WARNING : UIStrings.SETTINGS.READ_ONLY.WARNING}
      </NxInfoAlert>
      <NxH2>{LABELS.SECTIONS.SETUP}</NxH2>
      <NxReadOnly>
        <NxReadOnly.Label>{LABELS.ID.LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{id}</NxReadOnly.Data>
        <NxReadOnly.Label>{LABELS.NAME.LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{name}</NxReadOnly.Data>
        {description && <>
          <NxReadOnly.Label>{LABELS.DESCRIPTION.LABEL}</NxReadOnly.Label>
          <NxReadOnly.Data>{description}</NxReadOnly.Data>
        </>}
      </NxReadOnly>
      <NxH2>{LABELS.SECTIONS.PRIVILEGES}</NxH2>
      <NxList emptyMessage={LABELS.PRIVILEGES.EMPTY_LIST}>
        {(privileges || [])?.map(name => (
            <NxList.Item key={name}>
              <NxList.Text>{name}</NxList.Text>
            </NxList.Item>
        ))}
      </NxList>
      <NxH2>{LABELS.SECTIONS.ROLES}</NxH2>
      <NxList emptyMessage={LABELS.ROLES.EMPTY_LIST}>
        {(selectedRoles || [])?.map(roleId => (
            <NxList.Item key={roleId}>
              <NxList.Text>{rolesMap[roleId].name}</NxList.Text>
            </NxList.Item>
        ))}
      </NxList>
    </NxTile.Content>
    <NxFooter>
      <NxButtonBar>
        <NxButton type="button" onClick={cancel}>{UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}</NxButton>
      </NxButtonBar>
    </NxFooter>
  </NxLoadWrapper>;
}
