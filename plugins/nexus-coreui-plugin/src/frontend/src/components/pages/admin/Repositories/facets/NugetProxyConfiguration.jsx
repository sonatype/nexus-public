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

import {NxRadio, NxFieldset, NxFormGroup, NxTextInput} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {NUGET} = UIStrings.REPOSITORIES.EDITOR;

export default function NugetProxyConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {nugetVersion} = currentParent.context.data.nugetProxy;

  const updateNugetVersion = (value) => {
    sendParent({type: 'UPDATE', name: 'nugetProxy.nugetVersion', value});
  };

  return (
    <>
      <NxFieldset
        label={NUGET.PROTOCOL_VERSION.LABEL}
        className="nxrm-form-group-nuget-protocol-version"
        isRequired
      >
        <NxRadio
          name="nuget-protocol-version"
          value="V2"
          onChange={updateNugetVersion}
          isChecked={nugetVersion === 'V2'}
        >
          {NUGET.PROTOCOL_VERSION.V2_RADIO_DESCR}
        </NxRadio>
        <NxRadio
          name="nuget-protocol-version"
          value="V3"
          onChange={updateNugetVersion}
          isChecked={nugetVersion === 'V3'}
        >
          {NUGET.PROTOCOL_VERSION.V3_RADIO_DESCR}
        </NxRadio>
      </NxFieldset>

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
