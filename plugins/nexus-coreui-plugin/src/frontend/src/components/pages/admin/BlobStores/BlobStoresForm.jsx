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
  const isInvalid = FormUtils.isInvalid(validationErrors);
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

  function retry() {
    send({type: 'RETRY'});
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
                 description={isEdit ? FORM.EDIT_DESCRIPTION(type?.name) : null}/>
      {isEdit && type?.id !== "group" &&
      <PageActions>
        <NxButton variant="primary" onClick={promoteToGroup}>{FORM.PROMOTE_BUTTON}</NxButton>
      </PageActions>
      }
    </PageHeader>
    <Section>
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
              validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
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
        <FieldWrapper labelText={FORM.TYPE.label} description={FORM.TYPE.sublabel}>
          <Select id="type" name="type" value={type?.id} onChange={setType}>
            <option disabled={isTypeSelected} value=""></option>
            {types.map(({id, name}) => <option key={id} value={id}>{name}</option>)}
          </Select>
        </FieldWrapper>
        }
        {isTypeSelected &&
        <>
          <BlobStoreWarning type={type}/>
          {isCreate &&
          <FieldWrapper labelText={FORM.NAME.label}>
            <Textfield {...FormUtils.fieldProps('name', current)} onChange={FormUtils.handleUpdate('name', send)}/>
          </FieldWrapper>
          }
          <CustomBlobStoreSettings type={type} service={service}/>
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
          <div className="nxrm-soft-quota">
            <FieldWrapper labelText={FORM.SOFT_QUOTA.ENABLED.label} descriptionText={FORM.SOFT_QUOTA.ENABLED.sublabel}>
              <NxCheckbox {...FormUtils.checkboxProps(['softQuota', 'enabled'], current)} onChange={toggleSoftQuota}>
                {FORM.SOFT_QUOTA.ENABLED.text}
              </NxCheckbox>
            </FieldWrapper>

            {hasSoftQuota &&
            <>
              <FieldWrapper labelText={FORM.SOFT_QUOTA.TYPE.label} descriptionText={FORM.SOFT_QUOTA.TYPE.sublabel}>
                <Select {...FormUtils.fieldProps(['softQuota', 'type'], current)} onChange={updateQuotaField}>
                  <option value="" disabled></option>
                  {quotaTypes.map(({id, name}) => <option key={id} value={id}>{name}</option>)}
                </Select>
              </FieldWrapper>
              <FieldWrapper labelText={FORM.SOFT_QUOTA.LIMIT.label}>
                <Textfield {...FormUtils.fieldProps(['softQuota', 'limit'], current)} onChange={updateQuotaField}/>
              </FieldWrapper>
            </>
            }
          </div>
        </>
        }
      </NxForm>
    </Section>
  </Page>;
}
