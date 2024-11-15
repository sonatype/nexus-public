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
import {useActor} from '@xstate/react';

import {
  FormUtils,
  ValidationUtils,
} from '@sonatype/nexus-ui-plugin';
import {
  NxFormGroup,
  NxH2,
  NxTextInput,
  NxStatefulForm,
  NxFormSelect,
  NxTile,
  NxCheckbox,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';
import {NOTIFICATION_CONDITIONS} from './TasksHelper';
import TasksSummary from './TasksSummary';

const {TASKS: {FORM: LABELS}} = UIStrings;

export default function TasksForm({service, onDone}) {
  const [state, send] = useActor(service);

  const {
    data,
    pristineData,
    runnable,
    stoppable,
    types,
  } = state.context;

  const isCreate = ValidationUtils.isBlank(pristineData.id);
  const isEdit = !isCreate;
  const isTypeSelected = Boolean(data.typeId);

  const confirmDelete = () => send({type: 'CONFIRM_DELETE'});
  const confirmRun = () => send({type: 'CONFIRM_RUN'});
  const confirmStop = () => send({type: 'CONFIRM_STOP'});
  const cancel = () => onDone();

  return (
      <>
        {isEdit && data?.id &&
            <TasksSummary
                task={pristineData}
                runnable={runnable}
                stoppable={stoppable}
                onDelete={confirmDelete}
                onRun={confirmRun}
                onStop={confirmStop}
            />
        }
        <NxTile>
          <NxTile.Content>
            <NxStatefulForm
                {...FormUtils.formProps(state, send)}
                onCancel={cancel}
            >
              <NxH2>{LABELS.SECTIONS.SETTINGS}</NxH2>
              {isCreate && (
                  <NxFormGroup label={LABELS.TYPE.LABEL} isRequired>
                    <NxFormSelect
                        {...FormUtils.fieldProps('typeId', state)}
                        onChange={FormUtils.handleUpdate('typeId', send)}
                        disabled={isEdit}
                    >
                      <option disabled={isTypeSelected} value=""/>
                      {types?.map(({id, name}) =>
                          <option key={id} value={id}>{name}</option>
                      )}
                    </NxFormSelect>
                  </NxFormGroup>
              )}
              {(isEdit || isTypeSelected) && <>
                <NxFormGroup label={LABELS.ENABLED.LABEL}>
                  <NxCheckbox
                      {...FormUtils.checkboxProps('enabled', state)}
                      onChange={FormUtils.handleUpdate('enabled', send)}>
                    {LABELS.ENABLED.SUB_LABEL}
                  </NxCheckbox>
                </NxFormGroup>
                <NxFormGroup
                    label={LABELS.NAME.LABEL}
                    sublabel={LABELS.NAME.SUB_LABEL}
                    isRequired
                >
                  <NxTextInput
                      {...FormUtils.fieldProps('name', state)}
                      onChange={FormUtils.handleUpdate('name', send)}
                  />
                </NxFormGroup>
                <NxFormGroup
                    label={LABELS.EMAIL.LABEL}
                    sublabel={LABELS.EMAIL.SUB_LABEL}
                >
                  <NxTextInput
                      {...FormUtils.fieldProps('alertEmail', state)}
                      onChange={FormUtils.handleUpdate('alertEmail', send)}
                  />
                </NxFormGroup>
                <NxFormGroup
                    label={LABELS.SEND_NOTIFICATION_ON.LABEL}
                    sublabel={LABELS.SEND_NOTIFICATION_ON.SUB_LABEL}
                >
                  <NxFormSelect
                      {...FormUtils.fieldProps('notificationCondition', state)}
                      onChange={FormUtils.handleUpdate('notificationCondition', send)}
                  >
                    {Object.values(NOTIFICATION_CONDITIONS).map(({ID, LABEL}) =>
                        <option key={ID} value={ID}>{LABEL}</option>
                    )}
                  </NxFormSelect>
                </NxFormGroup>
              </>}
            </NxStatefulForm>
          </NxTile.Content>
        </NxTile>
      </>
  );
}
