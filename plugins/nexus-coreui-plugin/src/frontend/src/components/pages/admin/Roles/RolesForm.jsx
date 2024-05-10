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
import React, {useState} from 'react';
import {useService} from '@xstate/react';

import {
  FormUtils,
  ValidationUtils,
  ExtJS,
} from '@sonatype/nexus-ui-plugin';
import {
  NxFormGroup,
  NxButton,
  NxFontAwesomeIcon,
  NxH2,
  NxTextInput,
  NxStatefulForm,
  NxStatefulTransferList,
  NxFormSelect,
  NxTile,
  NxTransferListHalf
} from '@sonatype/react-shared-components';

import ExternalRolesCombobox from './ExternalRolesCombobox'

import {faTrash} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';
import {TYPES} from './RolesHelper';

import RolesSelectionModal from './RolesSelectionModal';

import PrivilegesSelectionModal from './PrivilegesSelectionModal';

const {ROLES: {FORM: LABELS}} = UIStrings;

export default function RolesForm({roleId, service, onDone}) {
  const stateMachine = useService(service);
  const [state, send] = stateMachine;

  const {
    data,
    roles,
    privileges,
    sources,
    roleType,
    externalRoleType,
    privilegesListFilter,
    rolesListFilter
  } = state.context;

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

  const cancel = () => onDone();

  const confirmDelete = () => send('CONFIRM_DELETE');

  const setRoleType = (roleType) => send({type: 'SET_ROLE_TYPE', roleType});

  const setExternalRoleType = (externalRoleType) => send({type: 'SET_EXTERNAL_ROLE_TYPE', externalRoleType});

  const [showModalRoles, setShowModalRoles] = useState(false);
  const [showModalPrivileges, setShowModalPrivileges] = useState(false);

  const selectedRoles = rolesList.filter(r => data.roles.includes(r.id));
  const selectedPrivileges = privilegesList.filter(r => data.privileges.includes(r.id));

  const rolesFooter = `${selectedRoles.length} Item${selectedRoles.length != 1 ? 's' : ''} Available`;
  const privilegesFooter = `${selectedPrivileges.length} Item${selectedPrivileges.length != 1 ? 's' : ''} Available`;

  const isRolesModalEnabled = ExtJS.state().getValue('nexus.react.roles.modal.enabled');
  const isPrivilegesModalEnabled = ExtJS.state().getValue('nexus.react.privileges.modal.enabled');

  const setPrivilegesListFilter = (event) => send({type: 'SET_PRIVILEGES_LIST_FILTER', privilegesListFilter: event});
  const setRolesListFilter = (event) => send({type: 'SET_ROLES_LIST_FILTER', rolesListFilter: event});

  function saveModalPrivileges (newPrivileges) {
    send({ type: 'UPDATE_PRIVILEGES', newPrivileges: newPrivileges });
  }

  function saveModalRoles (newRoles) {
    send({ type: 'UPDATE_ROLES', newRoles: newRoles });
  }

  return <NxStatefulForm
      {...FormUtils.formProps(state, send)}
      onCancel={cancel}
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
              {...FormUtils.selectProps('roleType', state)}
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
                  {...FormUtils.selectProps('externalRoleType', state)}
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
        {externalRole &&
            <ExternalRolesCombobox
                actor={state.context.externalRolesRef}
                parentMachine={stateMachine}
            />
        }
        {(isEdit || internalRole) &&
            <NxFormGroup label={LABELS.ID.LABEL} isRequired>
              <NxTextInput
                  {...FormUtils.fieldProps('id', state)}
                  disabled={isEdit}
                  onChange={FormUtils.handleUpdate('id', send)}
              />
            </NxFormGroup>
        }

        <NxFormGroup label={LABELS.NAME.LABEL} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps('name', state)}
              onChange={FormUtils.handleUpdate('name', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.DESCRIPTION.LABEL}>
          <NxTextInput
              className="nx-text-input--long"
              {...FormUtils.fieldProps('description', state)}
              onChange={FormUtils.handleUpdate('description', send)}
          />
        </NxFormGroup>

        {!isPrivilegesModalEnabled &&
          <>
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
          </>
        }

        {isPrivilegesModalEnabled &&
          <div className="modal-selection-section">
            <NxH2 className="modal-section-title">Applied Privileges</NxH2>
            <NxButton onClick={() => setShowModalPrivileges(true)} className="modal-button privileges-modal" variant="tertiary" type="button">
              Modify Applied Privileges
            </NxButton>
            {showModalPrivileges &&
              <PrivilegesSelectionModal
                title={LABELS.SECTIONS.PRIVILEGES}
                allPrivileges={privileges}
                onModalClose={() => setShowModalPrivileges(false)}
                selectedPrivileges={data.privileges}
                saveModal={saveModalPrivileges}
              />
            }
            <NxTransferListHalf
              label="Applied Privileges"
              filterValue={privilegesListFilter}
              onFilterChange={setPrivilegesListFilter}
              items={selectedPrivileges}
              footerContent={privilegesFooter} />
          </div>
        }

        {isRolesModalEnabled &&
          <div className="modal-selection-section">
            <NxH2 className="modal-section-title">Applied Roles</NxH2>
            <NxButton onClick={() => setShowModalRoles(true)} className="modal-button roles-modal" variant="tertiary" type="button">
              Modify Applied Roles
            </NxButton>
            {showModalRoles &&
              <RolesSelectionModal
                title={LABELS.SECTIONS.ROLES}
                allRoles={roles.filter(r => r.id != data.id)}
                onModalClose={() => setShowModalRoles(false)}
                selectedRoles={data.roles}
                saveModal={saveModalRoles}
              />
            }
            <NxTransferListHalf
              label="Applied Roles"
              filterValue={rolesListFilter}
              onFilterChange={setRolesListFilter}
              items={selectedRoles}
              footerContent={rolesFooter} />
          </div>
        }

        {!isRolesModalEnabled &&
          <>
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
          </>
        }
      </>}
    </NxTile.Content>
  </NxStatefulForm>;
}
