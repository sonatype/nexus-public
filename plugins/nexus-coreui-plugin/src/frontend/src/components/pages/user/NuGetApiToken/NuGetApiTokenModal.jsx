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
import {faKey} from "@fortawesome/free-solid-svg-icons";
import {Button, ExtJS, NxFontAwesomeIcon} from 'nexus-ui-plugin';
import {NxModal} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

export default function NuGetApiTokenModal({apiKey, onCloseClick}) {
  return <NxModal id="nuget-api-key-modal">
    <header className="nx-modal-header">
      <h2 className="nx-h2">
        <NxFontAwesomeIcon icon={faKey} fixedWidth/>
        <span> {UIStrings.NUGET_API_KEY.TITLE} </span>
      </h2>
    </header>
    <div className="nx-modal-content">
      <p>
        <span> {UIStrings.NUGET_API_KEY.DETAILS.MAIN} <strong> {UIStrings.NUGET_API_KEY.DETAILS.WARNING} </strong> </span> <br/>
        <span> {UIStrings.NUGET_API_KEY.DETAILS.API_KEY_TEXT} </span> <br/>
        <code id="nuget-api-key"> { apiKey } </code> <br/>
        <span> {UIStrings.NUGET_API_KEY.DETAILS.REGISTER_TEXT} </span> <br/>
        <code id="nuget-example-command"> { UIStrings.NUGET_API_KEY.DETAILS.REGISTER_COMMAND
            .replace('{0}', apiKey)
            .replace('{1}', ExtJS.urlOf('repository/{repository name}/')) }
        </code> <br/>
        <span> {UIStrings.NUGET_API_KEY.DETAILS.AUTO_CLOSE}</span> <br/>
      </p>
    </div>
    <footer className="nx-modal-footer">
      <Button onClick={onCloseClick}>
        {UIStrings.NUGET_API_KEY.CLOSE }
      </Button>
    </footer>
  </NxModal>
}
