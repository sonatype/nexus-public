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
import React, { FormEvent } from 'react';
import UIStrings from '../../../../constants/UIStrings';
import {FormUtils} from '@sonatype/nexus-ui-plugin';
import {NxButton, NxCheckbox, NxFieldset, NxFontAwesomeIcon, NxModal, useToggle, NxFormSelect, nxFormSelectStateHelpers, NxFormGroup} from '@sonatype/react-shared-components';
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
              onChange={(value) => setParams('systemInformation', value)}
          >
            {UIStrings.SUPPORT_ZIP.REPORT_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='threadDump'
              isChecked={params.threadDump}
              onChange={(value) => setParams('threadDump', value)}
          >
            {UIStrings.SUPPORT_ZIP.DUMP_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='configuration'
              isChecked={params.configuration}
              onChange={(value) => setParams('configuration', value)}
          >
            {UIStrings.SUPPORT_ZIP.CONFIGURATION_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='security'
              isChecked={params.security}
              onChange={(value) => setParams('security', value)}
          >
            {UIStrings.SUPPORT_ZIP.SECURITY_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='log'
              isChecked={params.log}
              onChange={(value) => setParams('log', value)}
          >
            {UIStrings.SUPPORT_ZIP.LOGFILES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='taskLog'
              isChecked={params.taskLog}
              onChange={(value) => setParams('taskLog', value)}
          >
            {UIStrings.SUPPORT_ZIP.TASKLOGFILES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='replication'
              isChecked={params.replication}
              onChange={(value) => setParams('replication', value)}
          >
            {UIStrings.SUPPORT_ZIP.REPLICATION_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='auditLog'
              isChecked={params.auditLog}
              onChange={(value) => setParams('auditLog', value)}
          >
            {UIStrings.SUPPORT_ZIP.AUDITLOGFILES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='metrics'
              isChecked={params.metrics}
              onChange={(value) => setParams('metrics', value)}
          >
            {UIStrings.SUPPORT_ZIP.METRICS_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='jmx'
              isChecked={params.jmx}
              onChange={(value) => setParams('jmx', value)}
          >
            {UIStrings.SUPPORT_ZIP.JMX_LABEL}
          </NxCheckbox>
          <NxFormGroup label={UIStrings.SUPPORT_ZIP.ARCHIVED_LOGS_LABEL}>
                <NxFormSelect id='archivedLog' onChange={ (value) => setParams('archivedLog', value)}>
                  <option value="0">None</option>
                  <option value="1">1 Day</option>
                  <option value="2">2 Days</option>
                  <option value="3">3 Days</option>
                </NxFormSelect>
          </NxFormGroup>
        </NxFieldset>
        <NxFieldset label={UIStrings.SUPPORT_ZIP.OPTIONS}>
          <NxCheckbox
              checkboxId='limitFileSizes'
              isChecked={params.limitFileSizes}
              onChange={(value) => setParams('limitFileSizes', value)}
          >
            {UIStrings.SUPPORT_ZIP.LIMITFILESIZES_LABEL}
          </NxCheckbox>
          <NxCheckbox
              checkboxId='limitZipSize'
              isChecked={params.limitZipSize}
              onChange={(value) => setParams('limitZipSize', value)}
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
