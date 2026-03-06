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

import {ExtJS, DateUtils} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxButtonBar,
  NxFooter,
  NxH2,
  NxH3,
  NxModal,
  NxP,
  NxTable,
  NxTableHead,
  NxTableRow,
  NxTableCell,
  NxTableBody,
  NxWarningAlert,
} from '@sonatype/react-shared-components';

import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  PageActions,
  Section
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';
import RecoveryModeMachine from './RecoveryModeMachine';

const {LABELS, MENU, TABLE, CONFIRMATION_MODAL} = UIStrings.RECOVERY_MODE;

export default function RecoveryMode() {
  const [current, send] = useMachine(RecoveryModeMachine, {devTools: true});

  if (!ExtJS.useUser()) {
    return null;
  }

  const disable = () => {
    send({ type: 'DISABLE' });
  };

  const confirmDisable = () => {
    send({ type: 'CONFIRM' });
  };

  const cancelDisable = () => {
    send({ type: 'CANCEL' });
  };

  const enable = () => {
    send({ type: 'ENABLE' });
  };
  const onTaskClick = (taskId) => {
    window.location.hash = `admin/system/tasks:${encodeURIComponent(taskId)}`;
  };

  const {data, error} = current.context;
  const isLoading = current.matches('loading');
  const showConfirmModal = current.matches('confirmingDisable');
  const hasRunningTasks = data?.reconcileTasks?.some(
    ({currentState}) => currentState?.startsWith('RUNNING')
  );

  return (
    <Page>
      <PageHeader>
        <PageTitle
          icon={MENU.icon}
          text={MENU.text}
          description={MENU.description}
        />
        <PageActions>
        {data?.enabled ?
          (<NxButton variant="primary" onClick={disable} disabled={hasRunningTasks}>
            <span>{LABELS.DISABLE_BUTTON}</span>
          </NxButton>)
          :
          (<NxButton variant="primary" onClick={enable}>
            <span>{LABELS.ENABLE_BUTTON}</span>
          </NxButton>)
          }
        </PageActions>
      </PageHeader>
      <ContentBody className='nxrm-recovery-mode'>
        {hasRunningTasks && (
          <NxWarningAlert>{LABELS.TASKS_RUNNING_WARNING}</NxWarningAlert>
        )}
        <Section>
          <NxH2>{LABELS.HOW_IT_WORKS}</NxH2>
          <NxP>{LABELS.HOW_IT_WORKS_DESCRIPTION}</NxP>
          <ul>
            {data?.blockedTaskNames?.map((item, index) => (
              <li key={index}>{item}</li>
            ))}
          </ul>
          <NxH3>{LABELS.STATUS_LABEL}</NxH3>
          <NxP>{data?.enabled ? LABELS.STATE_ENABLED : LABELS.STATE_DISABLED}</NxP>
        </Section>

        <Section>
          <NxH2>{LABELS.DATA_REPAIR_TASKS}</NxH2>
          <NxTable>
            <NxTableHead>
              <NxTableRow>
                <NxTableCell>
                  {TABLE.NAME_LABEL}
                </NxTableCell>
                <NxTableCell>
                  {TABLE.STATUS_LABEL}
                </NxTableCell>
                <NxTableCell>
                  {TABLE.LAST_RUN_LABEL}
                </NxTableCell>
                <NxTableCell>
                  {TABLE.LAST_RESULT_LABEL}
                </NxTableCell>
                <NxTableCell chevron/>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody isLoading={isLoading} error={error} emptyMessage={TABLE.EMPTY_MESSAGE}>
              {data?.reconcileTasks?.map(({id, name, currentState, lastRun, lastRunResult}) => (
                  <NxTableRow key={name}  onClick={() => onTaskClick(id)} isClickable>
                    <NxTableCell>{name}</NxTableCell>
                    <NxTableCell>{currentState}</NxTableCell>
                    <NxTableCell>{lastRun ? DateUtils.prettyDateTime(new Date(lastRun)) : ''}</NxTableCell>
                    <NxTableCell>{lastRunResult}</NxTableCell>
                    <NxTableCell chevron />
                  </NxTableRow>
              ))}
            </NxTableBody>
          </NxTable>
        </Section>
      </ContentBody>
      {showConfirmModal && (
        <NxModal aria-label="disable-recovery-mode-confirmation" variant="narrow" onCancel={cancelDisable}>
          <NxModal.Header>
            <NxH2>{CONFIRMATION_MODAL.TITLE}</NxH2>
          </NxModal.Header>
          <NxModal.Content>
            {CONFIRMATION_MODAL.MESSAGE}
          </NxModal.Content>
          <NxFooter>
            <NxButtonBar>
              <NxButton onClick={cancelDisable}>{CONFIRMATION_MODAL.CANCEL_BUTTON}</NxButton>
              <NxButton onClick={confirmDisable} variant="primary">{CONFIRMATION_MODAL.CONFIRM_BUTTON}</NxButton>
            </NxButtonBar>
          </NxFooter>
        </NxModal>
      )}
    </Page>
  );
}
