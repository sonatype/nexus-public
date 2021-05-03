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
  FieldWrapper,
  NxButton,
  NxCheckbox,
  NxErrorAlert,
  NxFontAwesomeIcon,
  NxTooltip,
  NxInfoAlert,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  SectionFooter,
  Select,
  Textfield,
  Utils,
  NxForm,
  FormUtils
} from '@sonatype/nexus-ui-plugin';
import BlobStoresFormMachine from './BlobStoresFormMachine';
import UIStrings from '../../../../constants/UIStrings';
import CustomBlobStoreSettings from './CustomBlobStoreSettings';
import BlobStoreWarning from './BlobStoreWarning';

const FORM = UIStrings.BLOB_STORES.FORM;

export default function BlobStoresForm({itemId, onDone}) {
  const idParts = itemId.split('/');
  const pristineData = idParts?.length ? {
    type: idParts[0],
    name: idParts[1]
  } : {};

  const [current, send] = useMachine(BlobStoresFormMachine, {
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

  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isCreate = itemId === '';
  const isEdit = !isCreate;
  const {
    blobStoreUsage,
    data,
    isPristine,
    saveError,
    loadError,
    quotaTypes,
    repositoryUsage,
    type,
    types,
    validationErrors
  } = current.context;
  const isInvalid = Utils.isInvalid(validationErrors);
  const hasSoftQuota = path(['softQuota', 'enabled'], data);
  const cannotSave = isPristine || isInvalid;
  const cannotDelete = blobStoreUsage > 0 || repositoryUsage > 0;
  const deleteTooltip = cannotDelete ?
      UIStrings.BLOB_STORES.MESSAGES.CANNOT_DELETE(repositoryUsage, blobStoreUsage) :
      null;

  function setType(event) {
    send({type: 'SET_TYPE', value: event.currentTarget.value});
  }

  function updateField(event) {
    send({type: 'UPDATE', data: {[event.target.name]: event.target.value}});
  }

  function updateDynamicField(name, value) {
    send({
      type: 'UPDATE',
      data: {
        [name]: value}
    });
  }

  function toggleSoftQuota() {
    send({type: 'UPDATE_SOFT_QUOTA', name: 'enabled', value: !path(['softQuota', 'enabled'], data)});
  }

  function toggleCustomSetting(event) {
    send({type: 'UPDATE_CUSTOM_SETTINGS', name: event.target.id, value: null});
  }

  function updateCustomSettings(event) {
    const name = event.target.name;
    const value = event.target.value;
    send({type: 'UPDATE_CUSTOM_SETTINGS', name, value});
  }

  function updateQuotaField(event) {
    const name = event.target.name.replace('softQuota.', '');
    const value = event.target.value;
    send({type: 'UPDATE_SOFT_QUOTA', name, value});
  }

  function retry() {
    send({type: 'RETRY'});
  }

  function handleEnter(event) {
    if (event.key === 'Enter') {
      save(event);
    }
  }

  function save() {
    send({type: 'SAVE'});
  }

  function cancel() {
    onDone();
  }

  function confirmDelete() {
    send({type: 'CONFIRM_DELETE'});
  }

  function promoteToGroup() {
    send({type: 'PROMOTE_TO_GROUP'});
  }

  return <Page className="nxrm-blob-stores">
    {saveError && <NxErrorAlert>{saveError}</NxErrorAlert>}
    <PageHeader>
      <PageTitle text={isEdit ? FORM.EDIT_TILE(data.name) : FORM.CREATE_TITLE}
                 description={isEdit ? FORM.EDIT_DESCRIPTION(type?.id) : null}/>
      {isEdit && type?.id !== "Group" &&
      <PageActions>
        <NxButton variant="primary" onClick={promoteToGroup}>{FORM.PROMOTE_BUTTON}</NxButton>
      </PageActions>
      }
    </PageHeader>
    <NxForm className="nxrm-blob-stores-form"
            loading={isLoading}
            loadError={loadError ? `${loadError}` : null}
            doLoad={retry}
            onCancel={cancel}
            onSubmit={save}
            submitError={saveError}
            submitMaskState={isSaving ? false : null}
            submitMaskMessage={UIStrings.SAVING}
            submitBtnText={UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
            validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}>
      <Section onKeyPress={handleEnter}>
        {isEdit && <NxInfoAlert>{FORM.EDIT_WARNING}</NxInfoAlert>}
        {isCreate &&
        <FieldWrapper labelText={FORM.TYPE.label} description={FORM.TYPE.sublabel}>
          <Select name="type" value={type?.id} onChange={setType}>
            <option value=""></option>
            {types.map(({id, name}) => <option key={id} value={id}>{name}</option>)}
          </Select>
        </FieldWrapper>
        }
        {type &&
        <>
          <BlobStoreWarning type={type}/>
          {isCreate &&
          <FieldWrapper labelText={FORM.NAME.label}>
            <Textfield {...Utils.fieldProps('name', current)} onChange={updateField}/>
          </FieldWrapper>
          }
          <CustomBlobStoreSettings type={type} current={current} updateCustomSettings={updateCustomSettings} toggleCustomSetting={toggleCustomSetting}/>
          {type?.fields?.map(field =>
              <FieldWrapper key={field.id}
                            labelText={field.label}
                            descriptionText={field.helpText}
                            isOptional={!field.required}>
                <DynamicFormField
                    id={field.id}
                    current={current}
                    initialValue={field.initialValue}
                    onChange={updateDynamicField}
                    dynamicProps={field}/>
              </FieldWrapper>
          )}
          <FieldWrapper labelText={FORM.SOFT_QUOTA.ENABLED.label} descriptionText={FORM.SOFT_QUOTA.ENABLED.sublabel}>
            <NxCheckbox {...Utils.checkboxProps(['softQuota', 'enabled'], current)} onChange={toggleSoftQuota}>
              {FORM.SOFT_QUOTA.ENABLED.text}
            </NxCheckbox>
          </FieldWrapper>
          {hasSoftQuota &&
          <>
            <FieldWrapper labelText={FORM.SOFT_QUOTA.TYPE.label} descriptionText={FORM.SOFT_QUOTA.TYPE.sublabel}>
              <Select {...Utils.fieldProps(['softQuota', 'type'], current)} onChange={updateQuotaField}>
                <option value="" disabled></option>
                {quotaTypes.map(({id, name}) => <option key={id} value={id}>{name}</option>)}
              </Select>
            </FieldWrapper>
            <FieldWrapper labelText={FORM.SOFT_QUOTA.LIMIT.label}>
              <Textfield {...Utils.fieldProps(['softQuota', 'limit'], current)} onChange={updateQuotaField}/>
            </FieldWrapper>
          </>
          }
          <SectionFooter>
            {itemId &&
            <NxTooltip title={deleteTooltip}>
              <NxButton variant="tertiary" className={cannotDelete && 'disabled'} onClick={confirmDelete}>
                <NxFontAwesomeIcon icon={faTrash}/>
                <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
              </NxButton>
            </NxTooltip>
            }
          </SectionFooter>
        </>
        }
      </Section>
    </NxForm>
  </Page>;
}
