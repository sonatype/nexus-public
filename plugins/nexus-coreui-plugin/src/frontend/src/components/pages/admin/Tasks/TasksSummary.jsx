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

import {
  NxTile,
  NxPageTitle,
  NxFontAwesomeIcon,
  NxButton,
  NxButtonBar,
  NxH2,
  NxTooltip,
  NxGrid,
  NxReadOnly,
} from '@sonatype/react-shared-components';
import {
  ReadOnlyField,
} from '@sonatype/nexus-ui-plugin';

import {faTrash, faStop, faPlay} from '@fortawesome/free-solid-svg-icons';
import UIStrings from '../../../../constants/UIStrings';
import {canDeleteTask, canRunTask, canStopTask} from './TasksHelper';

const {TASKS: {SUMMARY: LABELS, FORM}} = UIStrings;

export default function TasksSummary({task, runnable, stoppable, onDelete, onRun, onStop}) {
  const canDelete = canDeleteTask();
  const canRun = canRunTask();
  const canStop = canStopTask();

  const deleteTask = () => {
    if (canDelete) {
      onDelete();
    }
  };

  const runTask = () => {
    if (canRun) {
      onRun();
    }
  };

  const stopTask = () => {
    if (canStop) {
      onStop();
    }
  };

  return (
      <NxTile>
        <NxPageTitle>
          <NxH2>{FORM.SECTIONS.SUMMARY}</NxH2>
          <NxButtonBar>
            <NxTooltip title={!canDelete && UIStrings.PERMISSION_ERROR}>
              <NxButton
                  type="button"
                  className={!canDelete && 'disabled'}
                  onClick={deleteTask}
                  variant="tertiary"
              >
                <NxFontAwesomeIcon icon={faTrash}/>
                <span>{UIStrings.SETTINGS.DELETE_BUTTON_LABEL}</span>
              </NxButton>
            </NxTooltip>
            {runnable && task.enabled &&
                <NxTooltip title={!canRun && UIStrings.PERMISSION_ERROR}>
                  <NxButton
                      type="button"
                      className={!canRun && 'disabled'}
                      onClick={runTask}
                      variant="tertiary"
                  >
                    <NxFontAwesomeIcon icon={faPlay}/>
                    <span>{LABELS.BUTTONS.RUN}</span>
                  </NxButton>
                </NxTooltip>
            }
            {stoppable &&
                <NxTooltip title={!canStop && UIStrings.PERMISSION_ERROR}>
                  <NxButton
                      type="button"
                      className={!canStop && 'disabled'}
                      onClick={stopTask}
                      variant="tertiary"
                  >
                    <NxFontAwesomeIcon icon={faStop}/>
                    <span>{LABELS.BUTTONS.STOP}</span>
                  </NxButton>
                </NxTooltip>
            }
          </NxButtonBar>
        </NxPageTitle>
        <NxTile.Content>
          <NxGrid.Row>
            <NxGrid.Column className="nx-grid-col--33">
              <NxReadOnly>
                <ReadOnlyField label={LABELS.ID} value={task.id}/>
                <ReadOnlyField label={LABELS.NAME} value={task.name}/>
                <ReadOnlyField label={LABELS.TYPE} value={task.typeName}/>
              </NxReadOnly>
            </NxGrid.Column>
            <NxGrid.Column className="nx-grid-col--67">
              <NxReadOnly className="task-details-right-column">
                <ReadOnlyField label={LABELS.STATUS} value={task.statusDescription} item/>
                <ReadOnlyField label={LABELS.NEXT_RUN} value={task.nextRun} item/>
                <ReadOnlyField label={LABELS.LAST_RUN} value={task.lastRun} item/>
                <ReadOnlyField label={LABELS.LAST_RESULT} value={task.lastRunResult} item/>
              </NxReadOnly>
            </NxGrid.Column>
          </NxGrid.Row>
        </NxTile.Content>
      </NxTile>
  );
};
