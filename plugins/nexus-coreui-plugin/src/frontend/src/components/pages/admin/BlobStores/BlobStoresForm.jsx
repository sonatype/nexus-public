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
  FormUtils,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxCheckbox,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormSelect,
  NxInfoAlert,
  NxP,
  NxStatefulForm,
  NxTextInput,
  NxTooltip
} from '@sonatype/react-shared-components';

import BlobStoresFormMachine, {canUseSpaceUsedQuotaOnly, SPACE_USED_QUOTA_ID} from './BlobStoresFormMachine';
import UIStrings from '../../../../constants/UIStrings';
import CustomBlobStoreSettings from './CustomBlobStoreSettings';
import BlobStoreWarning from './BlobStoreWarning';
import BlobStoresConvertModal from './BlobStoresConvertModal';

const {BLOB_STORES: {FORM}} = UIStrings;

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
  const isCreate = itemId === '';
  const isEdit = !isCreate;
  const {
    blobStoreUsage,
    data,
    quotaTypes,
    repositoryUsage,
    type,
    types,
    bucketRegionMismatch
  } = current.context;
  const hasSoftQuota = path(['softQuota', 'enabled'], data);
  const cannotDelete = blobStoreUsage > 0 || repositoryUsage > 0;
  const deleteTooltip = cannotDelete ?
      UIStrings.BLOB_STORES.MESSAGES.CANNOT_DELETE(repositoryUsage, blobStoreUsage) :
      null;
  const isTypeSelected = Boolean(type)

  const setType = (value) => send({type: 'SET_TYPE', value});

  const updateDynamicField = (name, value) => {
    send({
      type: 'UPDATE',
      data: {
        [name]: value
      }
    });
  }

  const toggleSoftQuota = (value) => {
    send({type: 'UPDATE_SOFT_QUOTA', name: 'enabled', value, data});
  }

  const updateSoftQuotaType = (value) => send({ type: 'UPDATE_SOFT_QUOTA', name: 'type', value});
  const updateSoftQuotaLimit = (value) => send({ type: 'UPDATE_SOFT_QUOTA', name: 'limit', value});

  const confirmDelete = () => send({type: 'CONFIRM_DELETE'});
  const modalConvertToGroupOpen = () => send({type: 'MODAL_CONVERT_TO_GROUP_OPEN'});
  const modalConvertToGroupClose = () => send({type: 'MODAL_CONVERT_TO_GROUP_CLOSE'});

  const spaceUsedQuotaName = quotaTypes?.find((it) => it.id === SPACE_USED_QUOTA_ID).name;

  function getErrorTitleMessage() {
    return bucketRegionMismatch ? UIStrings.BLOB_STORES.GOOGLE.ERROR.bucketRegionMismatchTitle : undefined;
  }

  return <Page className="nxrm-blob-stores">
    <PageHeader>
      <PageTitle text={isEdit ? FORM.EDIT_TILE(pristineData.name) : FORM.CREATE_TITLE}
                 description={isEdit ? FORM.EDIT_DESCRIPTION(type?.name || pristineData.type) : null}/>
      {isEdit && type?.id !== 'group' && types?.some(type => type.id === 'group') &&
      <PageActions>
        <NxButton variant="primary" onClick={modalConvertToGroupOpen}>{FORM.CONVERT_TO_GROUP_BUTTON}</NxButton>
      </PageActions>
      }
    </PageHeader>
    <Section>
      <NxStatefulForm className="nxrm-blob-stores-form"
        {...FormUtils.formProps(current, send)}
        submitErrorTitleMessage={getErrorTitleMessage()}
        onCancel={onDone}
        additionalFooterBtns={itemId &&
          <NxTooltip title={deleteTooltip}>
            <NxButton variant="tertiary" className={cannotDelete && 'disabled'} onClick={confirmDelete} type="button">
              <NxFontAwesomeIcon icon={faTrash}/>
              <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
            </NxButton>
          </NxTooltip>
        }
      >
        {isEdit && <NxInfoAlert>{FORM.EDIT_WARNING}</NxInfoAlert>}
        {isCreate &&
        <NxFormGroup label={FORM.TYPE.label} sublabel={FORM.TYPE.sublabel} isRequired>
          <NxFormSelect {...FormUtils.selectProps('type', current)} value={type?.id} onChange={setType}>
            <option disabled={isTypeSelected} value=""></option>
            {types?.map(({id, name}) => <option key={id} value={id}>{name}</option>)}
          </NxFormSelect>
        </NxFormGroup>
        }
        {isTypeSelected &&
        <>
          <BlobStoreWarning type={type}/>
          {isCreate &&
              <NxFormGroup
                  className="blob-store-name"
                  label={FORM.NAME.label}
                  isRequired
              >
                <NxTextInput
                    {...FormUtils.fieldProps('name', current)}
                    onChange={FormUtils.handleUpdate('name', send)}
                />
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
              <NxFormGroup label={FORM.SOFT_QUOTA.TYPE.label} isRequired>
                {canUseSpaceUsedQuotaOnly(type)
                  ? <NxP>{spaceUsedQuotaName}</NxP>
                  : <NxFormSelect {...FormUtils.fieldProps(['softQuota', 'type'], current)} validatable onChange={updateSoftQuotaType}>
                      <option value="" disabled></option>
                      {quotaTypes.map(({ id, name }) => <option key={id} value={id}>{name}</option>)}
                    </NxFormSelect>
                }
              </NxFormGroup>
              <NxFormGroup label={FORM.SOFT_QUOTA.LIMIT.label} isRequired>
                <NxTextInput {...FormUtils.fieldProps(['softQuota', 'limit'], current)} onChange={updateSoftQuotaLimit}/>
              </NxFormGroup>
            </>
            }
          </div>
        </>
        }
      </NxStatefulForm>
    </Section>
    {showConvertToGroupModal &&
        <BlobStoresConvertModal
            name={data.name}
            onDone={onDone}
            onCancel={modalConvertToGroupClose}
        />
    }
  </Page>;
}
