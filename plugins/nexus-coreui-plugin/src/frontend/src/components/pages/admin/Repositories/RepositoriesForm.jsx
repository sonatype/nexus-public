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
  FormUtils,
  ExtJS,
  UIStrings as UIStringsGlobal
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm
} from '@sonatype/react-shared-components';

import {faDatabase, faTrash} from '@fortawesome/free-solid-svg-icons';

import UIStrings from '../../../../constants/UIStrings';
import './Repositories.scss';

import RepositoriesFormMachine from './RepositoriesFormMachine';

import {getFacets} from './RepositoryFormConfig';

import GenericFormatConfiguration from './facets/GenericFormatConfiguration';
import GenericNameConfiguration from './facets/GenericNameConfiguration';

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
    data: {format, type}
  } = current.context;

  const {EDITOR} = UIStrings.REPOSITORIES;

  const canDelete =
    format && ExtJS.checkPermission(`nexus:repository-admin:${format}:${itemId}:delete`);

  const confirmDelete = () => canDelete && send({type: 'CONFIRM_DELETE'});

  const repositoryFacets = getFacets(format, type);

  return (
    <Page className="nxrm-repository-editor">
      <PageHeader>
        <PageTitle icon={faDatabase} {...(isEdit ? EDITOR.EDIT_TITLE : EDITOR.CREATE_TITLE)} />
      </PageHeader>
      <ContentBody>
        <Section className="nxrm-repository-editor-form">
          <NxStatefulForm
            {...FormUtils.formProps(current, send)}
            onCancel={onDone}
            submitBtnText={isEdit ? EDITOR.SAVE_BUTTON : EDITOR.CREATE_BUTTON}
            additionalFooterBtns={
              isEdit && (
                <NxButton
                  type="button"
                  variant="tertiary"
                  onClick={confirmDelete}
                  className={!canDelete ? 'disabled' : ''}
                  title={!canDelete ? UIStringsGlobal.PERMISSION_ERROR : ''}
                >
                  <NxFontAwesomeIcon icon={faTrash} />
                  <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
                </NxButton>
              )
            }
          >
            <GenericFormatConfiguration parentMachine={stateMachine} />
            {format && type && (
              <>
                <GenericNameConfiguration parentMachine={stateMachine} />
                {repositoryFacets.map((Facet, index) => (
                  <Facet parentMachine={stateMachine} key={index} />
                ))}
              </>
            )}
          </NxStatefulForm>
        </Section>
      </ContentBody>
    </Page>
  );
}
