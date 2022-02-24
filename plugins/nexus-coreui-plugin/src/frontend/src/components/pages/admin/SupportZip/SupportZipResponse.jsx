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
import {FieldWrapper, SectionFooter, Textfield} from '@sonatype/nexus-ui-plugin';
import {NxButton, NxFontAwesomeIcon} from '@sonatype/react-shared-components';
import {faDownload} from '@fortawesome/free-solid-svg-icons';

export default function SupportZipResponse({response, download}) {
  return <div className='nxrm-support-zip-response'>
    <h2>{UIStrings.SUPPORT_ZIP.CREATED_TITLE}</h2>
    <p>Support ZIP has been created.<br/>
    You can reference this file on the filesystem or download the file from your browser.</p>
    <FieldWrapper labelText={UIStrings.SUPPORT_ZIP.CREATED_NAME_LABEL}>
      <Textfield
        name='name'
        disabled={true}
        value={response.name}
      />
    </FieldWrapper>
    <FieldWrapper labelText={UIStrings.SUPPORT_ZIP.CREATED_SIZE_LABEL}>
      <Textfield
        name='size'
        disabled={true}
        value={response.size}
      />
    </FieldWrapper>
    <FieldWrapper labelText={UIStrings.SUPPORT_ZIP.CREATED_PATH_LABEL}>
      <Textfield
        name='file'
        disabled={true}
        value={response.file}
      />
    </FieldWrapper>
    <SectionFooter>
      <NxButton variant="primary" onClick={(event) => download(event, response.name)} type="submit">
        <NxFontAwesomeIcon icon={faDownload}/>
        <span>{UIStrings.SUPPORT_ZIP.CREATED_DOWNLOAD_BUTTON}</span>
      </NxButton>
    </SectionFooter>
  </div>;
}
