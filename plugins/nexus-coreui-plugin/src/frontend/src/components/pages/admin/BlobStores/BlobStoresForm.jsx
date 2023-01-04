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
import {path} from 'ramda';
import {faTrash} from "@fortawesome/free-solid-svg-icons";

import {
  DynamicFormField,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  Textfield,
  FormUtils,
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxCheckbox,
  NxErrorAlert,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormSelect,
  NxInfoAlert,
  NxLoadWrapper,
  NxModal,
  NxStatefulForm,
  NxTextInput,
  NxTooltip
} from '@sonatype/react-shared-components';
import BlobStoresFormMachine from './BlobStoresFormMachine';
import UIStrings from '../../../../constants/UIStrings';
import CustomBlobStoreSettings from './CustomBlobStoreSettings';
import BlobStoreWarning from './BlobStoreWarning';

const FORM = UIStrings.BLOB_STORES.FORM;

export default function BlobStoresForm({itemId, onDone}) {
  const idParts = itemId.split('/');
  const pristineData = idParts?.length ? {
    type: idParts[0] && decodeURIComponent(idParts[0]),
    name: idParts[1] && decodeURIComponent(idParts[1])
  } : {};

  const [current, send, service] = useMachine(BlobStoresFormMachine, {
    context: {
      pristineData
    },

    actions: {
      onCancel: onDone,
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone
    },
    devTools: true
  });

  const showConvertToGroupModal = current.matches('modalConvertToGroup');
  const isConvertingToGroup = current.matches('convertToGroup');
  const isCreate = itemId === '';
  const isEdit = !isCreate;
  const {
    blobStoreUsage,
    data,
    quotaTypes,
    repositoryUsage,
    type,
    types
  } = current.context;
  const hasSoftQuota = path(['softQuota', 'enabled'], data);
  const cannotDelete = blobStoreUsage > 0 || repositoryUsage > 0;
  const deleteTooltip = cannotDelete ?
      UIStrings.BLOB_STORES.MESSAGES.CANNOT_DELETE(repositoryUsage, blobStoreUsage) :
      null;
  const isTypeSelected = Boolean(type)

  function setType(event) {
    send({type: 'SET_TYPE', value: event.currentTarget.value});
  }

  function updateDynamicField(name, value) {
    send({
      type: 'UPDATE',
      data: {
        [name]: value
      }
    });
  }

  function toggleSoftQuota(event) {
    send({type: 'UPDATE_SOFT_QUOTA', name: 'enabled', value: event.currentTarget.checked, data});
  }

  function updateQuotaField(event) {
    const name = event.target.name.replace('softQuota.', '');
    const value = event.target.value;
    send({type: 'UPDATE_SOFT_QUOTA', name, value});
  }

  function cancel() {
    onDone();
  }

  function confirmDelete() {
    send({type: 'CONFIRM_DELETE'});
  }

  function modalConvertToGroupOpen() {
    send({type: 'MODAL_CONVERT_TO_GROUP_OPEN'});
  }

  function modalConvertToGroupClose() {
    send({type: 'MODAL_CONVERT_TO_GROUP_CLOSE'});
  }

  function modalConvertToGroupSave() {
    send({type: 'MODAL_CONVERT_TO_GROUP_SAVE'});
  }

  function handleModalConvertToGroupValue(value) {
    send({
      type: 'MODAL_CONVERT_TO_GROUP_SET_NEW_BLOB_NAME',
      value
    });
  }

  return <Page className="nxrm-blob-stores">
    <PageHeader>
      <PageTitle text={isEdit ? FORM.EDIT_TILE(pristineData.name) : FORM.CREATE_TITLE}
                 description={isEdit ? FORM.EDIT_DESCRIPTION(type?.name || pristineData.type) : null}/>
      {isEdit && type?.id !== 'group' && types.some(type => type.id === 'group') &&
      <PageActions>
        <NxButton variant="primary" onClick={modalConvertToGroupOpen}>{FORM.CONVERT_TO_GROUP_BUTTON}</NxButton>
      </PageActions>
      }
    </PageHeader>
    <Section>
      <NxStatefulForm className="nxrm-blob-stores-form"
              {...FormUtils.formProps(current, send)}
              onCancel={cancel}
              additionalFooterBtns={itemId &&
              <NxTooltip title={deleteTooltip}>
                <NxButton variant="tertiary" className={cannotDelete && 'disabled'} onClick={confirmDelete}>
                  <NxFontAwesomeIcon icon={faTrash}/>
                  <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
                </NxButton>
              </NxTooltip>
              }>
        {isEdit && <NxInfoAlert>{FORM.EDIT_WARNING}</NxInfoAlert>}
        {isCreate &&
        <NxFormGroup label={FORM.TYPE.label} sublabel={FORM.TYPE.sublabel} isRequired>
          <NxFormSelect id="type" name="type" value={type?.id} onChange={setType}>
            <option disabled={isTypeSelected} value=""></option>
            {types.map(({id, name}) => <option key={id} value={id}>{name}</option>)}
          </NxFormSelect>
        </NxFormGroup>
        }
        {isTypeSelected &&
        <>
          <BlobStoreWarning type={type}/>
          {isCreate &&
          <NxFormGroup label={FORM.NAME.label} isRequired>
            <Textfield {...FormUtils.fieldProps('name', current)} onChange={FormUtils.handleUpdate('name', send)}/>
          </NxFormGroup>
          }
          <CustomBlobStoreSettings type={type} service={service}/>
          {type?.fields?.map(field =>
              <NxFormGroup key={field.id}
                           label={field.label}
                           sublabel={field.helpText}
                           isRequired={field.required}>
                <DynamicFormField
                    id={field.id}
                    current={current}
                    initialValue={field.initialValue}
                    onChange={updateDynamicField}
                    dynamicProps={field}/>
              </NxFormGroup>
          )}
          <div className="nxrm-soft-quota">
            <NxFieldset label={FORM.SOFT_QUOTA.ENABLED.label} sublabel={FORM.SOFT_QUOTA.ENABLED.sublabel}>
              <NxCheckbox {...FormUtils.checkboxProps(['softQuota', 'enabled'], current)} onChange={toggleSoftQuota}>
                {FORM.SOFT_QUOTA.ENABLED.text}
              </NxCheckbox>
            </NxFieldset>

            {hasSoftQuota &&
            <>
              <NxFormGroup label={FORM.SOFT_QUOTA.TYPE.label} sublabel={FORM.SOFT_QUOTA.TYPE.sublabel} isRequired>
                <NxFormSelect {...FormUtils.fieldProps(['softQuota', 'type'], current)} validatable onChange={updateQuotaField}>
                  <option value="" disabled></option>
                  {quotaTypes.map(({id, name}) => <option key={id} value={id}>{name}</option>)}
                </NxFormSelect>
              </NxFormGroup>
              <NxFormGroup label={FORM.SOFT_QUOTA.LIMIT.label} isRequired>
                <Textfield {...FormUtils.fieldProps(['softQuota', 'limit'], current)} onChange={updateQuotaField}/>
              </NxFormGroup>
            </>
            }
          </div>
        </>
        }
      </NxStatefulForm>
    </Section>
    {(showConvertToGroupModal || isConvertingToGroup) &&
    <NxModal onCancel={modalConvertToGroupClose}>
      <header className="nx-modal-header">
        <h2 className="nx-h2">Convert to Group Blob Store</h2>
      </header>
      <div className="nx-modal-content">
        <p className="nx-p">
          <strong>
            Rename Original Blob Store
          </strong>
        </p>
        <NxFormGroup
            sublabel="Assign a new name to the original blob store"
            isRequired
        >
          <NxTextInput
              disabled={isConvertingToGroup}
              value={data.modalConvertToGroupNewBlobName}
              onChange={handleModalConvertToGroupValue}
          />
        </NxFormGroup>
      </div>
      <footer className="nx-footer">
        <NxErrorAlert>
          You are converting to a group blob store. This action cannot be undone.
        </NxErrorAlert>
        <div className="nx-btn-bar">
          <NxLoadWrapper loading={isConvertingToGroup}>
            <NxButton onClick={modalConvertToGroupClose}>Close</NxButton>
            <NxButton onClick={modalConvertToGroupSave} variant="primary">Convert</NxButton>
          </NxLoadWrapper>
        </div>
      </footer>
    </NxModal>
    }
  </Page>;
}
