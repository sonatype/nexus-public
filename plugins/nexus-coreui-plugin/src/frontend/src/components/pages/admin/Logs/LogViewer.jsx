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
import React, {useEffect, useRef} from 'react';
import {useMachine} from '@xstate/react';

import {
  ContentBody,
  ExtJS,
  Page,
  PageActions,
  PageHeader,
  PageTitle,
  Section,
  SectionToolbar
} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxButtonBar,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxFormSelect,
  NxTextInput,
} from '@sonatype/react-shared-components';

import {faStamp} from '@fortawesome/free-solid-svg-icons';

import LogViewerMachine from "./LogViewerMachine";
import UIStrings from '../../../../constants/UIStrings';

const VIEW = UIStrings.LOGS.VIEW;

export default function LogViewer({itemId}) {
  const refreshPeriods = [
    [0,   UIStrings.LOGS.REFRESH.MANUAL_ITEM],
    [20,  UIStrings.LOGS.REFRESH.TWENTY_SECONDS_ITEM],
    [60,  UIStrings.LOGS.REFRESH.MINUTE_ITEM],
    [120, UIStrings.LOGS.REFRESH.TWO_MINUTES_ITEM],
    [300, UIStrings.LOGS.REFRESH.FIVE_MINUTES_ITEM]
  ];
  const refreshSizes = [
    [25,  UIStrings.LOGS.SIZE.LAST25KB_ITEM],
    [50,  UIStrings.LOGS.SIZE.LAST50KB_ITEM],
    [100, UIStrings.LOGS.SIZE.LAST100KB_ITEM]
  ];
  const [current, send] = useMachine(LogViewerMachine, {
    context: {
      itemId
    },
    devTools: true
  });
  const selectedPeriod = current.context.period;
  const selectedSize = current.context.size;
  const mark = current.context.mark;
  const logText = current.context.data;
  const textarea = useRef(null);

  // scroll to bottom of text area
  useEffect(() => {
    if (textarea.current) {
      textarea.current.scrollTop = textarea.current.scrollHeight;
    }
  });

  function onChangePeriod(period) {
    const newPeriod = Number(period);
    if (period == 0) {
      send({type: 'MANUAL_REFRESH', period});
    }
    else {
      send({type: 'UPDATE_PERIOD', period: newPeriod});
    }
  }

  function onChangeSize(size) {
    send({type: 'UPDATE_SIZE', size});
  }

  function updateMark(value) {
    send({type: 'UPDATE_MARK', mark: value});
  }

  function onMarkKeyPress(event) {
    if (event.key === 'Enter') {
      event.preventDefault();
      send({type: 'INSERT_MARK'});
    }
  }  

  function insertMark() {
    send({type: 'INSERT_MARK'});
  }

  return <Page>
    <PageHeader>
      <PageTitle text={VIEW.TITLE(itemId)}/>
      <PageActions>
        <a download className="nx-btn nx-btn--primary" href={ExtJS.urlOf(`service/rest/internal/logging/logs/${itemId}`)}>{VIEW.DOWNLOAD}</a>
      </PageActions>
    </PageHeader>
    <ContentBody>
      <Section className="nxrm-log-viewer">
        <SectionToolbar>
          {itemId === "nexus.log" && <div className="nx-form-row">
            <NxFormGroup label={VIEW.MARK.LABEL}>
              <NxTextInput
                  className="nxrm-log-viewer-mark-text"
                  id="mark"
                  name="mark"
                  value={mark}
                  isPristine={true}
                  onChange={updateMark}
                  onKeyPress={onMarkKeyPress}
              />
            </NxFormGroup>
            <NxButtonBar>
              <NxButton onClick={insertMark} id="insertMark">
                <NxFontAwesomeIcon icon={faStamp}/>
                <span>{VIEW.MARK.INSERT}</span>
              </NxButton>
            </NxButtonBar>
          </div>}

          <div className="nxrm-spacer" />

          <div className="nx-form-row">
            <NxFormGroup label={VIEW.REFRESH.RATE_LABEL}>
              <NxFormSelect name="period" id="period" value={selectedPeriod} onChange={onChangePeriod}>
                {refreshPeriods.map(([period, label]) =>
                    <option key={period} value={period}>{label}</option>
                )}
              </NxFormSelect>
            </NxFormGroup>
            <NxFormGroup className="log-viewer-size" label={VIEW.REFRESH.SIZE_LABEL}>
              <NxFormSelect name="size" value={selectedSize} onChange={onChangeSize}>
                {refreshSizes.map(([size, label]) =>
                    <option key={size} value={size}>{label}</option>
                )}
              </NxFormSelect>
            </NxFormGroup>
          </div>
        </SectionToolbar>
        <textarea
            name="logs"
            value={logText}
            className="log-viewer-textarea"
            ref={textarea}
            readOnly
        />
      </Section>
    </ContentBody>
  </Page>;
}
