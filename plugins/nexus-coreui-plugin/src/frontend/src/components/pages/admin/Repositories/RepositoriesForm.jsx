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
  Utils,
  FormUtils
} from '@sonatype/nexus-ui-plugin';

import {NxForm} from '@sonatype/react-shared-components';

import {faDatabase} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';
import './Repositories.scss';

import RepositoriesFormMachine from './RepositoriesFormMachine';

import GenericGroupConfiguration from './facets/GenericGroupConfiguration';
import GenericCleanupConfiguration from './facets/GenericCleanupConfiguration';
import GenericHostedConfiguration from './facets/GenericHostedConfiguration';
import GenericStorageConfiguration from './facets/GenericStorageConfiguration';
import GenericFormatConfiguration from './facets/GenericFormatConfiguration';
import GenericReadOnlyNameConfiguration from './facets/GenericReadOnlyNameConfiguration';
import GenericEditNameConfiguration from './facets/GenericEditNameConfiguration';
import GenericProxyConfiguration from './facets/GenericProxyConfiguration';
import GenericOptionsConfiguration from './facets/GenericOptionsConfiguration';
import GenericHttpReqConfiguration from './facets/GenericHttpReqConfiguration';
import GenericHttpAuthConfiguration from './facets/GenericHttpAuthConfiguration';

export default function RepositoriesForm({itemId, onDone = () => {}}) {
  const isEdit = Boolean(itemId);
  const stateMachine = useMachine(RepositoriesFormMachine, {
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

  const [current, send] = stateMachine;

  const {
    isPristine,
    loadError,
    saveError,
    validationErrors,
    data: {format, type}
  } = current.context;

  const {EDITOR} = UIStrings.REPOSITORIES;
  const {SAVING} = UIStrings;

  const isLoading = current.matches('loading');
  const isSaving = current.matches('saving');
  const isInvalid = Utils.isInvalid(validationErrors);

  const retry = () => send({type: 'RETRY'});

  const save = () => send({type: 'SAVE'});

  return (
    <Page className="nxrm-repository-editor">
      <PageHeader>
        <PageTitle icon={faDatabase} {...(isEdit ? EDITOR.EDIT_TITLE : EDITOR.CREATE_TITLE)} />
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
            submitBtnText={isEdit ? EDITOR.SAVE_BUTTON : EDITOR.CREATE_BUTTON}
            submitMaskMessage={SAVING}
            validationErrors={FormUtils.saveTooltip({isPristine, isInvalid})}
          >
            {isEdit && <GenericReadOnlyNameConfiguration parentMachine={stateMachine}/>}
            {!isEdit && <GenericFormatConfiguration parentMachine={stateMachine}/>}
            {format && type && (
              <>
              {!isEdit && <GenericEditNameConfiguration parentMachine={stateMachine} />}
                <GenericStorageConfiguration parentMachine={stateMachine} />
                {type === 'group' && <GenericGroupConfiguration parentMachine={stateMachine} />}
                {type === 'hosted' && <GenericHostedConfiguration parentMachine={stateMachine} />}
                {type === 'proxy' && (
                  <>
                    <GenericProxyConfiguration parentMachine={stateMachine} />
                    <GenericOptionsConfiguration parentMachine={stateMachine} />
                  </>
                )}
                {type !== 'group' && <GenericCleanupConfiguration parentMachine={stateMachine} />}
                {type === 'proxy' && (
                  <>
                    <GenericHttpAuthConfiguration parentMachine={stateMachine} />
                    <GenericHttpReqConfiguration parentMachine={stateMachine} />
                  </>
                )}
              </>
            )}
          </NxForm>
        </Section>
      </ContentBody>
    </Page>
  );
}
