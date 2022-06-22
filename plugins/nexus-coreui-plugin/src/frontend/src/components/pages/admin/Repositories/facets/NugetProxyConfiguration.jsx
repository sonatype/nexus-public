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

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {NxFormGroup, NxTextInput, NxFormSelect} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {NUGET} = UIStrings.REPOSITORIES.EDITOR;

const NUGET_VERSIONS = {
  V2: 'NuGet V2',
  V3: 'NuGet V3'
};

export default function NugetProxyConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  return (
    <>
      <NxFormGroup
        label={NUGET.PROTOCOL_VERSION.LABEL}
        className="nxrm-form-group-nuget-protocol-version"
        isRequired
      >
        <NxFormSelect
          {...FormUtils.selectProps('nugetProxy.nugetVersion', currentParent)}
          onChange={FormUtils.handleUpdate('nugetProxy.nugetVersion', sendParent)}
        >
          {Object.entries(NUGET_VERSIONS)?.map(([k, v]) => (
            <option key={v} value={k}>
              {v}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <NxFormGroup
        label={NUGET.METADATA_QUERY_CACHE_AGE.LABEL}
        sublabel={NUGET.METADATA_QUERY_CACHE_AGE.SUBLABEL}
        isRequired
        className="nxrm-form-group-nuget-cache"
      >
        <NxTextInput
          {...FormUtils.fieldProps('nugetProxy.queryCacheItemMaxAge', currentParent)}
          onChange={FormUtils.handleUpdate('nugetProxy.queryCacheItemMaxAge', sendParent)}
        />
      </NxFormGroup>
    </>
  );
}
