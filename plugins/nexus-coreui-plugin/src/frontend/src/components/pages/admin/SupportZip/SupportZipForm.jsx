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
import {Button, Checkbox, CheckboxGroup, NxFontAwesomeIcon} from "nexus-ui-plugin";
import {faFileArchive} from '@fortawesome/free-solid-svg-icons';

export default function SupportZipForm({params, setParams, submit, clustered, hazips}) {
  return <>
    <div dangerouslySetInnerHTML={{__html: UIStrings.SUPPORT_ZIP.DESCRIPTION}}/>
    <CheckboxGroup labelText={'Contents:'}>
      <Checkbox
        checkboxId='systemInformation'
        isChecked={params.systemInformation}
        onChange={setParams}
      >
        {UIStrings.SUPPORT_ZIP.REPORT_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='threadDump'
        isChecked={params.threadDump}
        onChange={setParams}
      >
       {UIStrings.SUPPORT_ZIP.DUMP_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='configuration'
        isChecked={params.configuration}
        onChange={setParams}
      >
        {UIStrings.SUPPORT_ZIP.CONFIGURATION_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='security'
        isChecked={params.security}
        onChange={setParams}
      >
        {UIStrings.SUPPORT_ZIP.SECURITY_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='log'
        isChecked={params.log}
        onChange={setParams}
      >
        {UIStrings.SUPPORT_ZIP.LOGFILES_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='taskLog'
        isChecked={params.taskLog}
        onChange={setParams}
      >
        {UIStrings.SUPPORT_ZIP.TASKLOGFILES_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='auditLog'
        isChecked={params.auditLog}
        onChange={setParams}
      >
        {UIStrings.SUPPORT_ZIP.AUDITLOGFILES_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='metrics'
        isChecked={params.metrics}
        onChange={setParams}
      >
        {UIStrings.SUPPORT_ZIP.METRICS_LABEL}
      </Checkbox>
      <Checkbox
        checkboxId='jmx'
        isChecked={params.jmx}
        onChange={setParams}
       >
        {UIStrings.SUPPORT_ZIP.JMX_LABEL}
       </Checkbox>
    </CheckboxGroup>
    <CheckboxGroup labelText={'Options:'}>
      <Checkbox
        checkboxId='limitFileSizes'
        isChecked={params.limitFileSizes}
        onChange={setParams}
       >
        {UIStrings.SUPPORT_ZIP.LIMITFILESIZES_LABEL}
       </Checkbox>
      <Checkbox
        checkboxId='limitZipSize'
        isChecked={params.limitZipSize}
        onChange={setParams}
       >
        {UIStrings.SUPPORT_ZIP.LIMITZIPSIZE_LABEL}
       </Checkbox>
    </CheckboxGroup>
    <Button variant='primary' onClick={submit} type='submit'>
      <NxFontAwesomeIcon icon={faFileArchive}/>
      <span>Create support ZIP</span>
    </Button>
    {clustered &&
      <Button variant='primary' onClick={hazips} type='submit'>
        <NxFontAwesomeIcon icon={faFileArchive}/>
        <span>Create support ZIP (all nodes)</span>
      </Button>
    }
  </>;
}
