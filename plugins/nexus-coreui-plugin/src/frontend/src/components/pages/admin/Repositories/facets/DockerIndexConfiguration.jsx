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

import {NxFieldset, NxRadio, NxFormGroup, NxTextInput} from '@sonatype/react-shared-components';
import {FormUtils} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../../constants/UIStrings';

import {DOCKER_INDEX_TYPES} from '../RepositoryFormConfig';

const {EDITOR} = UIStrings.REPOSITORIES;

const DOCKER_HUB_URL = 'https://index.docker.io/';

export default function DockerIndexConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {indexType} = currentParent.context.data.dockerProxy;

  const updateIndexType = (value) => {
    sendParent({type: 'UPDATE', name: 'dockerProxy.indexType', value});
    const indexUrl = value === DOCKER_INDEX_TYPES.registry ? null : DOCKER_HUB_URL;
    sendParent({type: 'UPDATE', name: 'dockerProxy.indexUrl', value: indexUrl});
  };

  return (
    <>
      <NxFieldset label={EDITOR.DOCKER_INDEX_LABEL}>
        <NxRadio
          name="indexType"
          value={DOCKER_INDEX_TYPES.registry}
          onChange={updateIndexType}
          isChecked={indexType === DOCKER_INDEX_TYPES.registry}
        >
          {EDITOR.USE_PROXY_REGISTRY_DESCR}
        </NxRadio>
        <NxRadio
          name="indexType"
          value={DOCKER_INDEX_TYPES.hub}
          onChange={updateIndexType}
          isChecked={indexType === DOCKER_INDEX_TYPES.hub}
        >
          {EDITOR.USE_DOCKER_HUB_DESCR}
        </NxRadio>
        <NxRadio
          name="indexType"
          value={DOCKER_INDEX_TYPES.custom}
          onChange={updateIndexType}
          isChecked={indexType === DOCKER_INDEX_TYPES.custom}
        >
          {EDITOR.USE_CUSTOM_INDEX_DESCR}
        </NxRadio>
      </NxFieldset>

      {indexType !== DOCKER_INDEX_TYPES.registry && (
        <NxFormGroup
          label={EDITOR.DOCKER_INDEX_URL_LABEL}
          className="nxrm-form-group-docker-index-url"
          isRequired
        >
          <NxTextInput
            {...FormUtils.fieldProps('dockerProxy.indexUrl', currentParent)}
            onChange={FormUtils.handleUpdate('dockerProxy.indexUrl', sendParent)}
            disabled={indexType === DOCKER_INDEX_TYPES.hub}
            placeholder={EDITOR.DOCKER_INDEX_URL_PLACEHOLDER}
          />
        </NxFormGroup>
      )}
    </>
  );
}
