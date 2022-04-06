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
import React, {useEffect} from 'react';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {NxFormGroup, NxFormSelect} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function YumHostedConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  useEffect(() => {
    sendParent({
      type: 'UPDATE',
      data: {
        'yum.repodataDepth': 0,
        'yum.deployPolicy': 'STRICT'
      }
    });
  }, []);

  const repodataDepthOptions = [0, 1, 2, 3, 4, 5];
  const deployPolicyOptions = [
    {key: 'Strict', value: 'STRICT'},
    {key: 'Permissive', value: 'PERMISSIVE'}
  ];

  return (
    <>
      <NxFormGroup
        label={EDITOR.YUM.REPODATA_DEPTH_LABEL}
        className="nxrm-form-group-yum-repodata-depth"
        sublabel={EDITOR.YUM.REPODATA_DEPTH_SUBLABEL}
        isRequired
      >
        <NxFormSelect
          {...FormUtils.fieldProps('yum.repodataDepth', currentParent)}
          onChange={FormUtils.handleUpdate('yum.repodataDepth', sendParent)}
        >
          {repodataDepthOptions?.map((val) => (
            <option key={val} value={val}>
              {val}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <NxFormGroup
        label={EDITOR.YUM.LAYOUT_POLICY_LABEL}
        className="nxrm-form-group-yum-deploy-policy"
        sublabel={EDITOR.YUM.LAYOUT_POLICY_SUBLABEL}
        isRequired
      >
        <NxFormSelect
          {...FormUtils.fieldProps('yum.deployPolicy', currentParent)}
          onChange={FormUtils.handleUpdate('yum.deployPolicy', sendParent)}
        >
          {deployPolicyOptions?.map(({key, value}) => (
            <option key={key} value={value}>
              {key}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>
    </>
  );
}
