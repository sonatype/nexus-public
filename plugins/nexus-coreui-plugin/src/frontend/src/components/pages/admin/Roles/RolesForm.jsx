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
import {useService} from '@xstate/react';

import {
  FormUtils,
  ValidationUtils,
  ExtJS,
} from '@sonatype/nexus-ui-plugin';
import {
  NxForm,
  NxFormGroup,
  NxButton,
  NxFontAwesomeIcon,
  NxH2,
  NxTextInput,
  NxStatefulTransferList,
  NxFormSelect,
  NxTile,
} from '@sonatype/react-shared-components';

import {faTrash} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';
import {TYPES} from './RolesHelper';

const {ROLES: {FORM: LABELS}} = UIStrings;

export default function RolesForm({roleId, service, onDone}) {
  const [current, send] = useService(service);

  const {
    isPristine,
    data,
    loadError,
    saveError,
    validationErrors,
    roles,
    privileges,
    sources,
    roleType,
    externalRoleType,
  } = current.context;

  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = ValidationUtils.isInvalid(validationErrors);
  const isCreate = ValidationUtils.isBlank(roleId);
  const isEdit = !isCreate;
  const hasDeletePermissions = ExtJS.checkPermission('nexus:roles:delete');
  const canDelete = hasDeletePermissions && !data.readOnly;

  const isRoleTypeSelected = Boolean(roleType);
  const isExternalTypeSelected = Boolean(externalRoleType);
  const internalRole = isCreate && roleType === TYPES.INTERNAL;
  const externalRole = isCreate && roleType === TYPES.EXTERNAL;
  const isTypeSelected = internalRole || (externalRole && isExternalTypeSelected);

  const hasExternalSources = isCreate && sources && sources.length;
  const privilegesList = privileges?.map((it) => ({id: it.name, displayName: it.name})) || [];

  const rolesList = roles?.filter(it => it.id !== roleId)?.map((it) => ({id: it.id, displayName: it.name})) || [];

  const save = () => send('SAVE');

  const cancel = () => onDone();

  const confirmDelete = () => send('CONFIRM_DELETE');

  const retry = () => send('RETRY');

  const setRoleType = (event) => send({type: 'SET_ROLE_TYPE', roleType: event.target.value});

  const setExternalRoleType = (event) => send({type: 'SET_EXTERNAL_ROLE_TYPE', externalRoleType: event.target.value});

  return <NxForm
      loading={isLoading}
      loadError={loadError}
      onCancel={cancel}
      doLoad={retry}
      onSubmit={save}
      submitError={saveError}
      submitMaskState={isSaving ? false : null}
      submitBtnText={UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
      submitMaskMessage={UIStrings.SAVING}
      validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
      additionalFooterBtns={isEdit && canDelete &&
          <NxButton variant="tertiary" onClick={confirmDelete}>
            <NxFontAwesomeIcon icon={faTrash}/>
            <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
          </NxButton>
      }
  >
    <NxTile.Content>
      {isCreate && <>
        <NxH2>{LABELS.SECTIONS.TYPE}</NxH2>
        <NxFormGroup label={LABELS.TYPE.LABEL} isRequired>
          <NxFormSelect
              {...FormUtils.selectProps('roleType', current)}
              value={roleType}
              onChange={setRoleType}
          >
            <option disabled={isRoleTypeSelected} value=""/>
            <option value={TYPES.INTERNAL}>{LABELS.TYPE.OPTIONS.NEXUS}</option>
            {hasExternalSources &&
                <option value={TYPES.EXTERNAL}>{LABELS.TYPE.OPTIONS.EXTERNAL}</option>
            }
          </NxFormSelect>
        </NxFormGroup>
        {externalRole &&
            <NxFormGroup label={LABELS.EXTERNAL_TYPE.LABEL} isRequired>
              <NxFormSelect
                  {...FormUtils.selectProps('externalRoleType', current)}
                  value={externalRoleType}
                  onChange={setExternalRoleType}
              >
                <option disabled={isExternalTypeSelected} value=""/>
                {sources?.map(({id, name}) =>
                    <option key={id} value={id}>{name}</option>
                )}
              </NxFormSelect>
            </NxFormGroup>
        }
      </>}
      {(isTypeSelected || isEdit) && <>
        <NxH2>{LABELS.SECTIONS.SETUP}</NxH2>
        <NxFormGroup label={externalRole ? LABELS.MAPPED_ROLE.LABEL : LABELS.ID.LABEL} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps('id', current)}
              disabled={isEdit}
              onChange={FormUtils.handleUpdate('id', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.NAME.LABEL} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps('name', current)}
              onChange={FormUtils.handleUpdate('name', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.DESCRIPTION.LABEL}>
          <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('description', current)}
              onChange={FormUtils.handleUpdate('description', send)}
          />
        </NxFormGroup>

        <NxH2>{LABELS.SECTIONS.PRIVILEGES}</NxH2>
        <NxStatefulTransferList
            id="privileges-select"
            availableItemsLabel={LABELS.PRIVILEGES.AVAILABLE}
            selectedItemsLabel={LABELS.PRIVILEGES.SELECTED}
            allItems={privilegesList}
            selectedItems={data.privileges || []}
            onChange={FormUtils.handleUpdate('privileges', send)}
            showMoveAll
        />

        <NxH2>{LABELS.SECTIONS.ROLES}</NxH2>
        <NxStatefulTransferList
            id="roles-select"
            availableItemsLabel={LABELS.ROLES.AVAILABLE}
            selectedItemsLabel={LABELS.ROLES.SELECTED}
            allItems={rolesList}
            selectedItems={data.roles || []}
            onChange={FormUtils.handleUpdate('roles', send)}
            showMoveAll
        />
      </>}
    </NxTile.Content>
  </NxForm>;
}
