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
import classNames from 'classnames';

import {
  ContentBody,
  ExtJS,
  FormUtils,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  ValidationUtils
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxH2,
  NxLoadWrapper,
  NxLoadError,
  NxSubmitMask,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';

import ReplicationFormMachine from './ReplicationFormMachine';

import UIStrings from '../../../../constants/UIStrings';

import './ReplicationForm.scss';
import ReplicationInformationFields from "./ReplicationInformationFields";
import ReplicationTargetFields from "./ReplicationTargetFields";

const FORM = UIStrings.REPLICATION.FORM;

export default function ReplicationForm({itemId, onDone}) {
  const isEdit = ValidationUtils.notBlank(itemId);
  const canDelete = isEdit && ExtJS.checkPermission('nexus:replication:delete');
  const [current, send, service] = useMachine(ReplicationFormMachine, {
    context: {
      pristineData: {
        name: itemId,
        useTruststore: false
      }
    },

    actions: {
      onCancel: onDone,
      onReset: onDone,
      onSaveSuccess: (ctx, evt) => onDone({ctx, evt}),
      onDeleteSuccess: onDone
    },

    guards: {
      canDelete: () => canDelete
    },

    devTools: true
  });

  const {
    isPristine,
    loadError,
    resetError,
    saveError,
    validationErrors
  } = current.context;
  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = ValidationUtils.isInvalid(validationErrors);
  const saveButtonClasses = classNames("nx-form__submit-btn", {
    disabled: FormUtils.saveTooltip({isPristine, isInvalid})
  });

  function save(event) {
    event.preventDefault();
    send('SAVE');
  }

  function retry() {
    send('RETRY');
  }

  function deleteReplication() {
    send('CONFIRM_DELETE');
  }

  return <Page className="nxrm-replication">
    <PageHeader>
      <PageTitle text={isEdit ? FORM.EDIT_TITLE : FORM.CREATE_TITLE}/>
      {canDelete &&
          <PageActions>
            <NxButton variant="tertiary" onClick={deleteReplication}>{FORM.DELETE_BUTTON_LABEL}</NxButton>
          </PageActions>
      }
    </PageHeader>
    <ContentBody className="nxrm-replication-form">
      <NxLoadWrapper loading={isLoading} error={loadError || resetError} retryHandler={retry}>
        {() => <form className="nx-form" onSubmit={save}>
          <NxTile>
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <NxH2>{FORM.TITLE}</NxH2>
              </NxTile.HeaderTitle>
            </NxTile.Header>
            <NxTile.Content>
              <ReplicationInformationFields isEdit={isEdit} service={service}/>
            </NxTile.Content>
          </NxTile>

          <NxTile>
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <NxH2>{FORM.TARGET_INFORMATION}</NxH2>
              </NxTile.HeaderTitle>
            </NxTile.Header>
            <NxTile.Content>
              <ReplicationTargetFields service={service}/>
            </NxTile.Content>
            <footer className="nx-footer">
              {saveError && <NxLoadError
                  titleMessage="An error occurred saving data."
                  error={saveError}
                  retryHandler={save}
              />}

              <div className="nx-btn-bar">
                <NxButton type="button" onClick={onDone} className="nx-form__cancel-btn">
                  {UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}
                </NxButton>
                <NxTooltip title={FormUtils.saveTooltip({isPristine, isInvalid}) || ''}>
                  <NxButton variant="primary" className={saveButtonClasses}>
                    {isEdit ? UIStrings.SETTINGS.SAVE_BUTTON_LABEL : FORM.CREATE_BUTTON}
                  </NxButton>
                </NxTooltip>
              </div>
            </footer>
          </NxTile>
          {isSaving && <NxSubmitMask success={isSaving}/>}
        </form>}
      </NxLoadWrapper>
    </ContentBody>
  </Page>;
}
