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
import {
  NxTile,
  NxFormGroup,
  NxFormSelect,
  NxH2,
  NxTextInput,
  NxFieldset,
  NxCheckbox,
  NxStatefulForm,
  NxButton,
} from '@sonatype/react-shared-components';
import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../constants/UIStrings';
import LdapServersModalPassword from './LdapServersModalPassword';
import {isDynamicGroup, isStaticGroup} from './LdapServersHelper';
import LdapVerifyUserMappingModal from './LdapVerifyUserMapping/LdapVerifyUserMappingModal';
import LdapVerifyLoginModal from './LdapVerifyLogin/LdapVerifyLoginModal';

const {
  LDAP_SERVERS: {FORM: LABELS},
} = UIStrings;

export default function LdapServerUserAndGroupForm({actor, onDone}) {
  const [state, send] = useActor(actor);
  const {
    templates,
    template,
    data: {ldapGroupsAsRoles, groupType},
    data,
    validationErrors,
  } = state.context;

  const isTemplateSelected = ValidationUtils.notBlank(template);

  const askingPassword = state.matches('askingPassword');
  const showingVerifyLoginModal = state.matches('showingVerifyLoginModal');
  const showingVerifyUserMappingModal = state.matches('showingVerifyUserMappingModal');

  const isInvalid = FormUtils.isInvalid(validationErrors);

  const updateTemplate = (value) =>
    send({type: 'UPDATE_TEMPLATE', value});
  const setSystemPassword = (value) => send({type: 'SET_PASSWORD', name: 'authPassword', value})
  const verifyLogin = () => send({type: 'VERIFY_LOGIN'});
  const verifyUserMapping = () => send({type: 'VERIFY_USER_MAPPING'});
  const cancel = () => send({type: 'CANCEL'});

  return (
    <NxTile.Content>
      <NxH2>{LABELS.CONFIGURATION}</NxH2>
      <NxStatefulForm 
        {...FormUtils.formProps(state, send)} 
        onCancel={onDone}
        additionalFooterBtns={
          <>
            <NxButton
              className={isInvalid ? 'disabled' : ''}
              onClick={verifyUserMapping}
              type="button"
            >
              {LABELS.VERIFY_USER_MAPPING_BUTTON}
            </NxButton>
            <NxButton
              className={isInvalid ? 'disabled' : ''}
              onClick={verifyLogin}
              type="button"
            >
              {LABELS.VERIFY_LOGIN_BUTTON}
            </NxButton>
          </>
        }
      >
        <NxFormGroup label={LABELS.TEMPLATE.LABEL}>
          <NxFormSelect onChange={updateTemplate} value={template}>
            <option value="" disabled={isTemplateSelected} />
            {templates.map(({name}) => (
              <option value={name} key={name}>
                {name}
              </option>
            ))}
          </NxFormSelect>
        </NxFormGroup>
        <NxFormGroup
          label={LABELS.USER_RELATIVE_DN.LABEL}
          sublabel={LABELS.USER_RELATIVE_DN.SUB_LABEL}
        >
          <NxTextInput
            {...FormUtils.fieldProps('userBaseDn', state)}
            onChange={FormUtils.handleUpdate('userBaseDn', send)}
            className="nx-text-input--long"
          />
        </NxFormGroup>
        <NxFieldset label={LABELS.USER_SUBTREE.LABEL}>
          <NxCheckbox
            {...FormUtils.checkboxProps('userSubtree', state)}
            onChange={FormUtils.handleUpdate('userSubtree', send)}
          >
            {LABELS.USER_SUBTREE.SUB_LABEL}
          </NxCheckbox>
        </NxFieldset>
        <NxFormGroup
          label={LABELS.OBJECT_CLASS.LABEL}
          sublabel={LABELS.OBJECT_CLASS.SUB_LABEL}
          isRequired
        >
          <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('userObjectClass', state)}
            onChange={FormUtils.handleUpdate('userObjectClass', send)}
          />
        </NxFormGroup>
        <NxFormGroup
          label={LABELS.USER_FILTER.LABEL}
          sublabel={LABELS.USER_FILTER.SUB_LABEL}
        >
          <NxTextInput
            className="nx-text-input--long"
            {...FormUtils.fieldProps('userLdapFilter', state)}
            onChange={FormUtils.handleUpdate('userLdapFilter', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.USER_ID_ATTRIBUTE} isRequired>
          <NxTextInput
            {...FormUtils.fieldProps('userIdAttribute', state)}
            onChange={FormUtils.handleUpdate('userIdAttribute', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.REAL_NAME_ATTRIBUTE} isRequired>
          <NxTextInput
            {...FormUtils.fieldProps('userRealNameAttribute', state)}
            onChange={FormUtils.handleUpdate('userRealNameAttribute', send)}
          />
        </NxFormGroup>
        <NxFormGroup label={LABELS.EMAIL_ATTRIBUTE} isRequired>
          <NxTextInput
            {...FormUtils.fieldProps('userEmailAddressAttribute', state)}
            onChange={FormUtils.handleUpdate('userEmailAddressAttribute', send)}
          />
        </NxFormGroup>
        <NxFormGroup
          label={LABELS.PASSWORD_ATTRIBUTE.LABEL}
          sublabel={LABELS.PASSWORD_ATTRIBUTE.SUB_LABEL}
        >
          <NxTextInput
            {...FormUtils.fieldProps('userPasswordAttribute', state)}
            onChange={FormUtils.handleUpdate('userPasswordAttribute', send)}
          />
        </NxFormGroup>
        <NxFieldset label={LABELS.MAP_LDAP.LABEL}>
          <NxCheckbox
            {...FormUtils.checkboxProps('ldapGroupsAsRoles', state)}
            onChange={FormUtils.handleUpdate('ldapGroupsAsRoles', send)}
          >
            {LABELS.MAP_LDAP.SUB_LABEL}
          </NxCheckbox>
        </NxFieldset>
        {ldapGroupsAsRoles && (
          <>
            <NxFormGroup label={LABELS.GROUP_TYPE.LABEL} isRequired>
              <NxFormSelect
                {...FormUtils.fieldProps('groupType', state)}
                onChange={FormUtils.handleUpdate('groupType', send)}
                validatable
              >
                {Object.values(LABELS.GROUP_TYPE.OPTIONS).map(({id, label}) => (
                  <option key={id} value={id}>
                    {label}
                  </option>
                ))}
              </NxFormSelect>
            </NxFormGroup>
            {isDynamicGroup(groupType) && (
              <NxFormGroup
                label={LABELS.GROUP_MEMBER_OF_ATTRIBUTE.LABEL}
                sublabel={LABELS.GROUP_MEMBER_OF_ATTRIBUTE.SUB_LABEL}
                isRequired
              >
                <NxTextInput
                  {...FormUtils.fieldProps('userMemberOfAttribute', state)}
                  onChange={FormUtils.handleUpdate(
                    'userMemberOfAttribute',
                    send
                  )}
                />
              </NxFormGroup>
            )}
            {isStaticGroup(groupType) && (
              <>
                <NxFormGroup
                  label={LABELS.GROUP_DN.LABEL}
                  sublabel={LABELS.GROUP_DN.SUB_LABEL}
                >
                  <NxTextInput
                    {...FormUtils.fieldProps('groupBaseDn', state)}
                    onChange={FormUtils.handleUpdate('groupBaseDn', send)}
                  />
                </NxFormGroup>
                <NxFieldset label={LABELS.GROUP_SUBTREE.LABEL}>
                  <NxCheckbox
                    {...FormUtils.checkboxProps('groupSubtree', state)}
                    onChange={FormUtils.handleUpdate('groupSubtree', send)}
                  >
                    {LABELS.GROUP_SUBTREE.SUB_LABEL}
                  </NxCheckbox>
                </NxFieldset>
                <NxFormGroup
                  label={LABELS.GROUP_OBJECT_CLASS.LABEL}
                  sublabel={LABELS.GROUP_OBJECT_CLASS.SUB_LABEL}
                  isRequired
                >
                  <NxTextInput
                    {...FormUtils.fieldProps('groupObjectClass', state)}
                    onChange={FormUtils.handleUpdate('groupObjectClass', send)}
                  />
                </NxFormGroup>
                <NxFormGroup label={LABELS.GROUP_ID_ATTRIBUTE.LABEL} isRequired>
                  <NxTextInput
                    {...FormUtils.fieldProps('groupIdAttribute', state)}
                    onChange={FormUtils.handleUpdate('groupIdAttribute', send)}
                  />
                </NxFormGroup>
                <NxFormGroup
                  label={LABELS.GROUP_MEMBER_ATTRIBUTE.LABEL}
                  sublabel={LABELS.GROUP_MEMBER_ATTRIBUTE.SUB_LABEL}
                  isRequired
                >
                  <NxTextInput
                    {...FormUtils.fieldProps('groupMemberAttribute', state)}
                    onChange={FormUtils.handleUpdate(
                      'groupMemberAttribute',
                      send
                    )}
                  />
                </NxFormGroup>
                <NxFormGroup
                  label={LABELS.GROUP_MEMBER_FORMAT.LABEL}
                  sublabel={LABELS.GROUP_MEMBER_FORMAT.SUB_LABEL}
                  isRequired
                >
                  <NxTextInput
                    {...FormUtils.fieldProps('groupMemberFormat', state)}
                    onChange={FormUtils.handleUpdate('groupMemberFormat', send)}
                    className="nx-text-input--long"
                  />
                </NxFormGroup>
              </>
            )}
          </>
        )}
      </NxStatefulForm>
      {askingPassword && (
        <LdapServersModalPassword
          onCancel={cancel}
          onSubmit={({value}) => setSystemPassword(value)}
        />
      )}
      {showingVerifyLoginModal && (
        <LdapVerifyLoginModal ldapConfig={data} onCancel={cancel} />
      )}
      {showingVerifyUserMappingModal && (
        <LdapVerifyUserMappingModal ldapConfig={data} onCancel={cancel} />
      )}
    </NxTile.Content>
  );
}
