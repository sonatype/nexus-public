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

import {FormUtils} from '@sonatype/nexus-ui-plugin';
import {
  NxWarningAlert,
  NxFormGroup,
  NxH2,
  NxModal,
  NxStatefulForm,
  NxTextInput,
} from '@sonatype/react-shared-components';

import Machine from './BlobStoresConvertModalMachine';
import UIStrings from '../../../../constants/UIStrings';

const {BLOB_STORES: {FORM: {CONVERT_TO_GROUP_MODAL}}} = UIStrings;

export default function BlobStoresConvertModal({name, onDone, onCancel}) {
  const [state, send] = useMachine(Machine, {
    context: {
      pristineData: {name},
      data: {
        newName: `${name}-original`,
      },
    },
    actions: {
      onSaveSuccess: onDone,
    },
    devTools: true
  });

  return (
      <NxModal onCancel={onCancel}>
        <NxStatefulForm
            {...FormUtils.formProps(state, send)}
            onCancel={onCancel}
            submitBtnText={CONVERT_TO_GROUP_MODAL.CONVERT_BUTTON}
        >
          <NxModal.Header>
            <NxH2>{CONVERT_TO_GROUP_MODAL.HEADER}</NxH2>
          </NxModal.Header>
          <NxModal.Content>
            <NxWarningAlert>{CONVERT_TO_GROUP_MODAL.ALERT}</NxWarningAlert>
            <NxFormGroup
                className="blob-store-new-name"
                label={CONVERT_TO_GROUP_MODAL.LABEL}
                sublabel={CONVERT_TO_GROUP_MODAL.SUBLABEL}
                isRequired
            >
              <NxTextInput
                  {...FormUtils.fieldProps('newName', state)}
                  onChange={FormUtils.handleUpdate('newName', send)}
              />
            </NxFormGroup>
          </NxModal.Content>
        </NxStatefulForm>
      </NxModal>
  );
};
