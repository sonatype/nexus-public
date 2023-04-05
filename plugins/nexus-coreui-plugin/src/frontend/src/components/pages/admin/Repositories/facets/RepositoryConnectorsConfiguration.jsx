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
import {FormUtils, ExtJS} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../../constants/UIStrings';

import ToggleableTextInput from './ToggleableTextInput/ToggleableTextInput';

const {CONNECTORS} = UIStrings.REPOSITORIES.EDITOR.DOCKER;

export default function RepositoryConnectorsConfiguration({parentMachine}) {
  const [parentState, sendParent] = parentMachine;

  const repositoryName = parentState.context.data.name;

  const isProEdition = ExtJS.isProEdition();

  return (
    <>
      <NxH2 className="nxrm-docker-connectors-caption">{CONNECTORS.CAPTION}</NxH2>

      <p className="nxrm-docker-connectors-help">{CONNECTORS.HELP}</p>

      {isProEdition && (
        <ToggleableTextInput
          parentMachine={parentMachine}
          contextPropName="docker.subdomain"
          label={CONNECTORS.SUBDOMAIN.LABEL}
          sublabel={CONNECTORS.SUBDOMAIN.SUBLABEL}
          defaultValue={repositoryName}
          placeholder={CONNECTORS.SUBDOMAIN.PLACEHOLDER}
          clearIfDisabled
          className="nxrm-form-group-docker-connector-subdomain"
        />
      )}

      <ToggleableTextInput
        parentMachine={parentMachine}
        contextPropName="docker.httpPort"
        label={CONNECTORS.HTTP.LABEL}
        sublabel={CONNECTORS.HTTP.SUBLABEL}
        placeholder={CONNECTORS.HTTP.PLACEHOLDER}
        clearIfDisabled
        className="nxrm-form-group-docker-connector-http-port"
      />
      <ToggleableTextInput
        parentMachine={parentMachine}
        contextPropName="docker.httpsPort"
        label={CONNECTORS.HTTPS.LABEL}
        sublabel={CONNECTORS.HTTPS.SUBLABEL}
        placeholder={CONNECTORS.HTTPS.PLACEHOLDER}
        clearIfDisabled
        className="nxrm-form-group-docker-connector-https-port"
      />

      <NxFieldset
        label={CONNECTORS.ALLOW_ANON_DOCKER_PULL.LABEL}
        className="nxrm-form-group-force-basic-auth"
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('docker.forceBasicAuth', parentState)}
          onChange={FormUtils.handleUpdate('docker.forceBasicAuth', sendParent)}
        >
          {CONNECTORS.ALLOW_ANON_DOCKER_PULL.DESCR}
        </NxCheckbox>
      </NxFieldset>
    </>
  );
}
