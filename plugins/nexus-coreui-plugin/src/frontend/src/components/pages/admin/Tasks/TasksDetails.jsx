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
  ValidationUtils,
} from '@sonatype/nexus-ui-plugin';

import Machine from './TasksFormMachine';
import TasksForm from './TasksForm';
import TasksReadOnly from './TasksReadOnly';
import {canUpdateTask} from './TasksHelper';

import {faClock} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';

export default function TasksDetails({itemId, onDone}) {
  const hasEditPermissions = canUpdateTask();

  const [state, , service] = useMachine(Machine, {
    context: {
      pristineData: {
        id: decodeURIComponent(itemId),
      }
    },
    actions: {
      onSaveSuccess: onDone,
      onDeleteSuccess: onDone,
    },
    devTools: true,
  });

  const {pristineData: {id}} = state.context;

  const isEdit = ValidationUtils.notBlank(id);
  const showReadOnly = isEdit && !hasEditPermissions;

  return <Page className="nxrm-task">
    <PageHeader>
      <PageTitle icon={faClock} {...UIStrings.TASKS.MENU} />
    </PageHeader>
    <ContentBody className="nxrm-task-form">
      {showReadOnly
          ? <TasksReadOnly service={service} onDone={onDone}/>
          : <TasksForm service={service} onDone={onDone}/>
      }
    </ContentBody>
  </Page>;
}
