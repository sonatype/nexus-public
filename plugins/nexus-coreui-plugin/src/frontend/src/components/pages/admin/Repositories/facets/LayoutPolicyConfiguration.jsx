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

import {NxFormGroup, NxFormSelect} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function LayoutPolicyConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  let {format} = currentParent.context.data;
  format = format === 'maven2' ? 'maven' : format;

  const layoutPolicyOptions = [
    {key: 'Strict', value: 'STRICT'},
    {key: 'Permissive', value: 'PERMISSIVE'}
  ];

  return (
    <NxFormGroup
      label={EDITOR.LAYOUT_POLICY_LABEL}
      className="nxrm-form-group-layout-policy"
      sublabel={EDITOR.LAYOUT_POLICY_SUBLABEL}
      isRequired
    >
      <NxFormSelect
        {...FormUtils.selectProps(`${format}.layoutPolicy`, currentParent)}
        onChange={FormUtils.handleUpdate(`${format}.layoutPolicy`, sendParent)}
      >
        {layoutPolicyOptions.map(({key, value}) => (
          <option key={key} value={value}>
            {key}
          </option>
        ))}
      </NxFormSelect>
    </NxFormGroup>
  );
}
