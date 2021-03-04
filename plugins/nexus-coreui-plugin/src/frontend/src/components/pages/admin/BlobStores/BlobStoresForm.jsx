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
  ContentBody,
  DynamicFormField,
  FieldWrapper,
  NxButton,
  NxCheckbox,
  NxFontAwesomeIcon,
  NxLoadWrapper,
  NxTooltip,
  NxWarningAlert,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  SectionFooter,
  Select,
  Textfield,
  Utils
} from '@sonatype/nexus-ui-plugin';
import BlobStoresFormMachine from './BlobStoresFormMachine';
import UIStrings from '../../../../constants/UIStrings';

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
  const isCreate = itemId === '';
  const isEdit = !isCreate;
  const {
    blobStoreUsage,
    data,
    isPristine,
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

  function toggleSoftQuota() {
    send({type: 'UPDATE_SOFT_QUOTA', name: 'enabled', value: !path(['softQuota', 'enabled'], data)});
  }

  function updateQuotaField(event) {
    const name = event.target.name.replace('softQuota.', '');
    const value = event.target.value;
    send({type: 'UPDATE_SOFT_QUOTA', name, value})
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

  return <Page className="nxrm-blob-stores">
    {isEdit && <NxWarningAlert>{FORM.EDIT_WARNING}</NxWarningAlert>}
    <PageHeader>
      <PageTitle text={isEdit ? FORM.EDIT_TILE(data.name) : FORM.CREATE_TITLE}
                 description={isEdit ? FORM.EDIT_DESCRIPTION(type?.id) : null}/>
      {isEdit &&
      <PageActions>
        <NxButton variant="primary">{FORM.PROMOTE_BUTTON}</NxButton>
      </PageActions>
      }
    </PageHeader>
    <ContentBody className="nxrm-blob-stores-form">
      <Section onKeyPress={handleEnter}>
        <NxLoadWrapper loading={isLoading} error={loadError ? `${loadError}` : null} retryHandler={retry}>
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
            {isCreate &&
            <FieldWrapper labelText={FORM.NAME.label}>
              <Textfield {...Utils.fieldProps('name', current)} onChange={updateField}/>
            </FieldWrapper>
            }
            {type?.fields?.map(field =>
                <FieldWrapper key={field.id}
                              labelText={field.label}
                              descriptionText={field.helpText}
                              isOptional={!field.required}>
                  <DynamicFormField
                      {...Utils.fieldProps(field.id, current, field.initialValue || '')}
                      onChange={updateField}
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
              <NxTooltip title={Utils.saveTooltip({isPristine, isInvalid})}>
                <NxButton variant="primary" className={cannotSave && 'disabled'} onClick={save} type="submit">
                  {UIStrings.SETTINGS.SAVE_BUTTON_LABEL}
                </NxButton>
              </NxTooltip>
              <NxButton onClick={cancel}>{UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}</NxButton>
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
        </NxLoadWrapper>
      </Section>
    </ContentBody>
  </Page>;
}
