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

import {NxH2, NxCheckbox, NxFieldset} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../../constants/UIStrings';

import ToggleableTextInput from './ToggleableTextInput/ToggleableTextInput';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function RepositoryConnectorsConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  return (
    <>
      <NxH2 className="nxrm-docker-connectors-caption">{EDITOR.REPOSITORY_CONNECTORS_CAPTION}</NxH2>

      <p className="nxrm-docker-connectors-help">{EDITOR.DOCKER_CONNECTORS_HELP}</p>

      <ToggleableTextInput
        parentMachine={parentMachine}
        contextPropName="docker.httpPort"
        label={EDITOR.HTTP_CONNECTOR_LABEL}
        sublabel={EDITOR.HTTP_CONNECTOR_SUBLABEL}
        placeholder={EDITOR.DOCKER_CONNECTOR_PLACEHOLDER}
        className="nxrm-form-group-docker-connector-http-port"
      />
      <ToggleableTextInput
        parentMachine={parentMachine}
        contextPropName="docker.httpsPort"
        label={EDITOR.HTTPS_CONNECTOR_LABEL}
        sublabel={EDITOR.HTTPS_CONNECTOR_SUBLABEL}
        placeholder={EDITOR.DOCKER_CONNECTOR_PLACEHOLDER}
        className="nxrm-form-group-docker-connector-https-port"
      />

      <NxFieldset
        label={EDITOR.ALLOW_ANON_DOCKER_PULL_LABEL}
        className="nxrm-form-group-force-basic-auth"
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('docker.forceBasicAuth', currentParent)}
          onChange={FormUtils.handleUpdate('docker.forceBasicAuth', sendParent)}
        >
          {EDITOR.ALLOW_ANON_DOCKER_PULL_DESCR}
        </NxCheckbox>
      </NxFieldset>
    </>
  );
}
