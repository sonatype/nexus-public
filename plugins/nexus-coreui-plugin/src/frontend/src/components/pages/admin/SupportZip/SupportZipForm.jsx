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
import UIStrings from '../../../../constants/UIStrings';
import {NxButton, NxCheckbox, NxFieldset, NxFontAwesomeIcon, NxModal} from '@sonatype/react-shared-components';
import {faFileArchive} from '@fortawesome/free-solid-svg-icons';

export default function SupportZipForm({params, setParams, submit, clustered, hazips, cancel}) {
  return <>
    <NxModal.Content>
      <div className="nx-form">
        {cancel
        && <div className='nx-support-zip-modal-form-vertical-spacer'/>
        }

        <div dangerouslySetInnerHTML={{__html: UIStrings.SUPPORT_ZIP.DESCRIPTION}}/>
        <NxFieldset label={UIStrings.SUPPORT_ZIP.CONTENTS}>
          <NxCheckbox
              checkboxId='systemInformation'
              isChecked={params.systemInformation}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.REPORT_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='threadDump'
              isChecked={params.threadDump}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.DUMP_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='configuration'
              isChecked={params.configuration}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.CONFIGURATION_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='security'
              isChecked={params.security}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.SECURITY_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='log'
              isChecked={params.log}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.LOGFILES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='taskLog'
              isChecked={params.taskLog}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.TASKLOGFILES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='replication'
              isChecked={params.replication}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.REPLICATION_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='auditLog'
              isChecked={params.auditLog}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.AUDITLOGFILES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='metrics'
              isChecked={params.metrics}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.METRICS_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='jmx'
              isChecked={params.jmx}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.JMX_LABEL}
          </NxCheckbox>
        </NxFieldset>
        <NxFieldset label={UIStrings.SUPPORT_ZIP.OPTIONS}>
          <NxCheckbox
              checkboxId='limitFileSizes'
              isChecked={params.limitFileSizes}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.LIMITFILESIZES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='limitZipSize'
              isChecked={params.limitZipSize}
              onChange={setParams}
          >
            {UIStrings.SUPPORT_ZIP.LIMITZIPSIZE_LABEL}
          </NxCheckbox>
        </NxFieldset>
      </div>
    </NxModal.Content>

    <footer className="nx-footer">
      <div className="nx-btn-bar">
        {cancel &&
        <NxButton variant='secondary' onClick={cancel}>
          <span>{UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}</span>
        </NxButton>
        }

        <NxButton variant='primary' onClick={submit} type='submit'>
          <NxFontAwesomeIcon icon={faFileArchive}/>
          <span>Create support ZIP</span>
        </NxButton>
        {clustered &&
        <NxButton variant='primary' onClick={hazips} type='submit'>
          <NxFontAwesomeIcon icon={faFileArchive}/>
          <span>Create support ZIP (all nodes)</span>
        </NxButton>
        }
      </div>
    </footer>
  </>;
}
