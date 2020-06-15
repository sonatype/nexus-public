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
import {Button, FieldWrapper, NxFontAwesomeIcon, SectionFooter, Textfield} from "nexus-ui-plugin";
import {faDownload} from '@fortawesome/free-solid-svg-icons';

export default function NodeSupportZipResponse({response, download}) {
  return <div className='nxrm-support-zip-response-ha'>
    <h2>{UIStrings.SUPPORT_ZIP.CREATED_TITLE}</h2>
    <p dangerouslySetInnerHTML={{__html: UIStrings.SUPPORT_ZIP.CREATED_DESCRIPTION}}></p>
    <FieldWrapper labelText={UIStrings.SUPPORT_ZIP.CREATED_NODEID_LABEL}>
      <Textfield
        name='nodeId'
        disabled
        value={response.nodeId}
      />
    </FieldWrapper>
    <FieldWrapper labelText={UIStrings.SUPPORT_ZIP.CREATED_NAME_LABEL}>
      <Textfield
        name='name'
        disabled
        value={response.name}
      />
    </FieldWrapper>
    <FieldWrapper labelText={UIStrings.SUPPORT_ZIP.CREATED_SIZE_LABEL}>
      <Textfield
        name='size'
        disabled
        value={response.size}
      />
    </FieldWrapper>
    <FieldWrapper labelText={UIStrings.SUPPORT_ZIP.CREATED_PATH_LABEL}>
      <Textfield
        name='file'
        disabled
        value={response.file}
      />
    </FieldWrapper>
    <SectionFooter>
      <Button variant="primary" onClick={(event) => download(event, response.name)} type="submit">
        <NxFontAwesomeIcon icon={faDownload}/>
        <span>{UIStrings.SUPPORT_ZIP.CREATED_DOWNLOAD_BUTTON}</span>
      </Button>
    </SectionFooter>
  </div>;
}
