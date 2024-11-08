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
import {faServer} from '@fortawesome/free-solid-svg-icons';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  FormUtils,
  ReadOnlyField,
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxTooltip,
  NxTextInput,
  NxFormGroup,
  NxStatefulForm,
  NxReadOnly,
  NxTile,
} from '@sonatype/react-shared-components';

import DataStoreConfigurationMachine from './DataStoreConfigurationMachine';
import UIStrings from '../../../../constants/UIStrings';

const FIELDS = UIStrings.DATASTORE_CONFIGURATION.FIELDS;

export default function DataStoreConfiguration() {
  const [state, send] = useMachine(DataStoreConfigurationMachine, {
    devTools: true,
  });

  const {isPristine, data} = state.context;
  const discard = () => send({type: 'RESET'});

  return (
    <Page>
      <PageHeader>
        <PageTitle
          icon={faServer}
          {...UIStrings.DATASTORE_CONFIGURATION.MENU}
        />
      </PageHeader>
      <ContentBody className="nxrm-datastore-configuration">
        <NxTile className="user-account-settings">
          <NxStatefulForm
            {...FormUtils.formProps(state, send)}
            additionalFooterBtns={
              <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
                <NxButton
                  type="button"
                  className={isPristine && 'disabled'}
                  onClick={discard}
                >
                  {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
                </NxButton>
              </NxTooltip>
            }
          >
            <NxReadOnly>
              <ReadOnlyField label={FIELDS.jdbcUrlLabel} value={data.jdbcUrl} />
              <ReadOnlyField
                label={FIELDS.usernameLabel}
                value={data.username}
              />
              <ReadOnlyField label={FIELDS.schemaLabel} value={data.schema} />
            </NxReadOnly>
            <NxFormGroup label={FIELDS.maxConnectionPoolLabel}>
              <NxTextInput
                {...FormUtils.fieldProps('maximumConnectionPool', state)}
                onChange={FormUtils.handleUpdate('maximumConnectionPool', send)}
              />
            </NxFormGroup>
            <NxFormGroup label={FIELDS.advancedLabel}>
              <NxTextInput
                {...FormUtils.fieldProps('advanced', state)}
                onChange={FormUtils.handleUpdate('advanced', send)}
                className="nx-text-input--long"
                type="textarea"
              />
            </NxFormGroup>
          </NxStatefulForm>
        </NxTile>
      </ContentBody>
    </Page>
  );
}
