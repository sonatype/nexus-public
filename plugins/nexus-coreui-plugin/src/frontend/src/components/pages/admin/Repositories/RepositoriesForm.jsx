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

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section,
  Select,
  Utils,
  FormUtils
} from '@sonatype/nexus-ui-plugin';

import {
  NxForm,
  NxCheckbox,
  NxFormGroup,
  NxTextInput
} from '@sonatype/react-shared-components';

import {faDatabase} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';
import './Repositories.scss';
import RepositoriesFormMachine from './RepositoriesFormMachine';
import GroupMembersSelector from './GroupMembersSelector';

export default function RepositoriesForm({itemId, onDone = () => {}}) {
  const [current, send] = useMachine(RepositoriesFormMachine, {
    context: {
      pristineData: {
        name: itemId
      }
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone
    },
    devTools: true
  });

  const {
    isPristine,
    isEdit,
    loadError,
    saveError,
    validationErrors,
    data: {
      format, 
      type,
      memberNames
    },
    formats,
    types,
    repositories,
    blobStores
  } = current.context;

  const {EDITOR} = UIStrings.REPOSITORIES;
  const {SAVING} = UIStrings;

  const isLoading = current.matches('loading') 
    || current.matches('loadingOptions')
    || current.matches('loadingRepositories');
  const isSaving = current.matches('saving');

  const isInvalid = Utils.isInvalid(validationErrors);

  const retry = () => send({type: 'RETRY'});

  const update = (event) => {
    send({type: 'UPDATE', data: {[event.target.name]: event.target.value}});
  }

  const handleFormatUpdate = (event) => {
    const format = event.target.value;
    const data = {format, memberNames: []}
    if (type) {
      data.type = types.get(format)?.includes(type) ? type : '';;
    }
    send({type: 'UPDATE', data});
  }

  const save = () => send({type: 'SAVE'});

  const allRepositories = repositories?.map(r => 
    ({id: r.id, displayName: r.name})) || [];

  return <Page className="nxrm-repository-editor">
      <PageHeader>
        <PageTitle icon={faDatabase} {...isEdit ? EDITOR.EDIT_TITLE : EDITOR.CREATE_TITLE}/>
      </PageHeader>
      <ContentBody>
        <Section className="nxrm-repository-editor-form">
          <NxForm
              loading={isLoading}
              loadError={loadError}
              onCancel={onDone}
              doLoad={retry}
              onSubmit={save}
              submitError={saveError}
              submitMaskState={isSaving ? false : null}
              submitBtnText={EDITOR.SAVE_BUTTON_LABEL}
              submitMaskMessage={SAVING}
              validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
          >
            <h2 className="nx-h2">{EDITOR.FORMAT_AND_TYPE_CAPTION}</h2>
            <div className="nx-form-row">
              <NxFormGroup 
                label={EDITOR.FORMAT_LABEL} 
                isRequired
                className="nxrm-form-group-format"
              >
                <Select 
                  {...FormUtils.fieldProps('format', current)}
                  name="format"
                  onChange={handleFormatUpdate}
                >
                  <option value="">{EDITOR.SELECT_FORMAT_OPTION}</option>
                  {formats?.map(format =>
                    <option key={format} value={format}>{format}</option>
                  )} 
                </Select>
              </NxFormGroup>
              <NxFormGroup 
                label={EDITOR.TYPE_LABEL} 
                isRequired
                className="nxrm-form-group-type"
              >
                <Select 
                  {...FormUtils.fieldProps('type', current)}
                  name="type"
                  onChange={update}
                  disabled={!format}
                >
                  <option value="">{EDITOR.SELECT_TYPE_OPTION}</option>
                  {types?.get(format)?.map(type =>
                    <option key={type} value={type}>{type}</option>
                  )} 
                </Select>
              </NxFormGroup>
            </div>

            {format && type && <>
              <h2 className="nx-h2">{EDITOR.CONFIGURATION_CAPTION}</h2>
              <NxFormGroup 
                label={EDITOR.NAME_LABEL} 
                isRequired
                className="nxrm-form-group-name"
              >
                <NxTextInput 
                  {...FormUtils.fieldProps('name', current)}
                  onChange={FormUtils.handleUpdate('name', send)}
                />
              </NxFormGroup>
              <NxFormGroup 
                label={EDITOR.STATUS_LABEL} 
                isRequired
                className="nxrm-form-group-status"
              >
                <NxCheckbox
                  {...FormUtils.checkboxProps('online', current)}
                  onChange={FormUtils.handleUpdate('online', send)}
                >
                  {EDITOR.STATUS_DESCR} 
                </NxCheckbox>
              </NxFormGroup>

              <h2 className="nx-h2">{EDITOR.STORAGE_CAPTION}</h2>
              <NxFormGroup 
                label={EDITOR.BLOB_STORE_LABEL} 
                isRequired
                className="nxrm-form-group-store"
              >
                <Select 
                  {...FormUtils.fieldProps('blobStoreName', current)}
                  name="blobStoreName"
                  onChange={update}
                >
                  <option value="">{EDITOR.SELECT_STORE_OPTION}</option>
                  {blobStores?.map(bs =>
                    <option key={bs.name} value={bs.name}>{bs.name}</option>
                  )} 
                </Select>
              </NxFormGroup>
              {type !== 'group' && <NxFormGroup 
                label={EDITOR.CONTENT_VALIDATION_LABEL} 
                isRequired
                className="nxrm-form-group-content-validation"
              >
                <NxCheckbox
                  {...FormUtils.checkboxProps('strictContentTypeValidation', current)}
                  onChange={FormUtils.handleUpdate('strictContentTypeValidation', send)}
                >
                  {EDITOR.CONTENT_VALIDATION_DESCR}
                </NxCheckbox>
              </NxFormGroup>}

              {type === 'group' && <GroupMembersSelector 
                format={format}
                selectedMembers={memberNames}
                onChange={FormUtils.handleUpdate('memberNames', send)}
              />}
              
            </>}
          </NxForm>
        </Section>
      </ContentBody>
    </Page>;
}
