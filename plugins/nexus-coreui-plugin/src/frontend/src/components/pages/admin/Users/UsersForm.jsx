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
  ExtJS
} from '@sonatype/nexus-ui-plugin';
import {
  NxForm,
  NxFormGroup,
  NxButton,
  NxFontAwesomeIcon,
  NxH2,
  NxTextInput,
  NxFormSelect,
  NxTile,
  NxStatefulTransferList,
  NxReadOnly,
  NxModal,
  NxTooltip
} from '@sonatype/react-shared-components';

import {faTrash} from '@fortawesome/free-solid-svg-icons';

import {STATUSES, isAnonymousUser, isExternalUser, isCurrentUser} from "./UsersHelper";

import UIStrings from '../../../../constants/UIStrings';

import ConfirmAdminPasswordForm from './ConfirmAdminPasswordForm';
import ConfirmNewPasswordForm from './ConfirmNewPasswordForm';

import './Users.scss';

const {USERS: {FORM: LABELS, MODAL}} = UIStrings;

export default function UsersForm({service, onDone}) {
  const [current, send] = useService(service);

  const {
    data: {externalRoles = [], readOnly, source, roles = []},
    pristineData: {userId},
    loadError,
    saveError,
    allRoles,
    isPristine,
    validationErrors,
    hasDeletePermission,
  } = current.context;

  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = ValidationUtils.isInvalid(validationErrors);
  const isCreate = ValidationUtils.isBlank(userId);
  const isExternal = isExternalUser(source);
  const hasExternalRoles = Boolean(externalRoles.length);
  const isEdit = !isCreate;
  const isChangingPassword = current.matches('changingPassword');
  const confirmingAdminPassword = current.matches('changingPassword.confirmAdminPassword');
  const confirmingNewPassword = current.matches('changingPassword.confirmNewPassword');
  const hasCreatePermission = ExtJS.checkPermission('nexus:*');

  const allRolesList = allRoles?.map((it) => ({id: it.id, displayName: it.name})) || [];

  const save = () => send('SAVE');

  const cancel = () => onDone();

  const confirmDelete = () => send('CONFIRM_DELETE');

  const retry = () => send('RETRY');

  const changePassword = () => send('CHANGE_PASSWORD');
  const cancelChangePassword = () => send('CANCEL_CHANGE_PASSWORD');

  const showDeleteButton = isEdit && hasDeletePermission && !readOnly && !isExternal &&
      !isAnonymousUser(userId) && !isCurrentUser(userId);

  const showChangePassword = isEdit && !readOnly && !isExternal && !isAnonymousUser(userId);

  return <>
    <NxForm
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
        additionalFooterBtns={showDeleteButton &&
        <NxButton variant="tertiary" onClick={confirmDelete}>
          <NxFontAwesomeIcon icon={faTrash}/>
          <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
        </NxButton>
        }
    >
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{LABELS.SECTIONS.SETUP}</NxH2>
        </NxTile.HeaderTitle>
        { showChangePassword &&
          <NxTile.HeaderActions className="nxrm-users__actions">
            <NxTooltip
                title={!hasCreatePermission && UIStrings.PERMISSION_ERROR}
                placement="bottom"
            >
              <span>
                <NxButton
                    onClick={changePassword}
                    variant="tertiary"
                    type="button"
                    disabled={!hasCreatePermission}
                >
                  {MODAL.CHANGE_PASSWORD}
                </NxButton>
              </span>
            </NxTooltip>
          </NxTile.HeaderActions>
        }
      </NxTile.Header>
      <NxTile.Content>
        <NxFormGroup
            label={LABELS.ID.LABEL}
            sublabel={LABELS.ID.SUB_LABEL}
            isRequired
        >
          <NxTextInput
              {...FormUtils.fieldProps('userId', current)}
              onChange={FormUtils.handleUpdate('userId', send)}
              disabled={isEdit || isExternal}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.FIRST_NAME.LABEL} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps('firstName', current)}
              onChange={FormUtils.handleUpdate('firstName', send)}
              disabled={isExternal}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.LAST_NAME.LABEL} isRequired>
          <NxTextInput
              {...FormUtils.fieldProps('lastName', current)}
              onChange={FormUtils.handleUpdate('lastName', send)}
              disabled={isExternal}
          />
        </NxFormGroup>
        <NxFormGroup
            label={LABELS.EMAIL.LABEL}
            sublabel={LABELS.EMAIL.SUB_LABEL}
            isRequired
        >
          <NxTextInput
              {...FormUtils.fieldProps('emailAddress', current)}
              onChange={FormUtils.handleUpdate('emailAddress', send)}
              disabled={isExternal}
          />
        </NxFormGroup>
        {!isExternal && isCreate && <>
          <NxFormGroup label={LABELS.PASSWORD.LABEL} isRequired>
            <NxTextInput
                type="password"
                autoComplete="new-password"
                {...FormUtils.fieldProps('password', current)}
                onChange={FormUtils.handleUpdate('password', send)}
            />
          </NxFormGroup>
          <NxFormGroup label={LABELS.CONFIRM_PASSWORD.LABEL} isRequired>
            <NxTextInput
                type="password"
                autoComplete="new-password"
                {...FormUtils.fieldProps('passwordConfirm', current)}
                onChange={FormUtils.handleUpdate('passwordConfirm', send)}
            />
          </NxFormGroup>
        </>}
        <NxFormGroup label={LABELS.STATUS.LABEL} isRequired>
          <NxFormSelect
              {...FormUtils.selectProps('status', current)}
              onChange={FormUtils.handleUpdate('status', send)}
              disabled={isExternal}
          >
            <option value=""/>
            {Object.values(STATUSES).map(({id, label}) =>
                <option key={id} value={id}>{label}</option>
            )}
          </NxFormSelect>
        </NxFormGroup>

        <div className="nx-form-group">
          <NxH2>{LABELS.SECTIONS.ROLES}</NxH2>
          <NxStatefulTransferList
              id="user-roles-select"
              availableItemsLabel={LABELS.ROLES.AVAILABLE}
              selectedItemsLabel={LABELS.ROLES.GRANTED}
              allItems={allRolesList}
              selectedItems={roles}
              onChange={FormUtils.handleUpdate('roles', send)}
              showMoveAll
          />
        </div>
        {isExternal &&
        <NxReadOnly>
          <NxReadOnly.Label>{LABELS.EXTERNAL_ROLES.LABEL}</NxReadOnly.Label>
          {hasExternalRoles && externalRoles.map(name => (
              <NxReadOnly.Data key={name}>{name}</NxReadOnly.Data>
          ))}
          {!hasExternalRoles &&
          <NxReadOnly.Data>{LABELS.EXTERNAL_ROLES.EMPTY_LIST}</NxReadOnly.Data>
          }
        </NxReadOnly>
        }
      </NxTile.Content>
    </NxForm>
    {isChangingPassword &&
      <NxModal
        onCancel={cancelChangePassword}
        aria-labelledby="modal-form-header"
        variant="narrow">
          {confirmingAdminPassword &&
            <ConfirmAdminPasswordForm actor={current.children.confirmAdminPasswordMachine}/>
          }
          {confirmingNewPassword &&
            <ConfirmNewPasswordForm actor={current.children.confirmNewPasswordMachine} />
          }
      </NxModal>}
  </>;
}
