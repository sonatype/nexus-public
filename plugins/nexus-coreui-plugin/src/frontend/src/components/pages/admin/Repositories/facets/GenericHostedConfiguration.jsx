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

import {NxFormGroup, NxCheckbox, NxFieldset, NxFormSelect} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

const deploymentPolicies = Object.entries(EDITOR.DEPLOYMENT_POLICY_OPTIONS);

export default function GenericHostedConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  return (
    <>
      <h2 className="nx-h2">{EDITOR.HOSTED_CAPTION}</h2>
      <NxFormGroup
        label={EDITOR.DEPLOYMENT_POLICY_LABEL}
        sublabel={EDITOR.DEPLOYMENT_POLICY_SUBLABEL}
        isRequired
        className="nxrm-form-group-hosted"
      >
        <NxFormSelect
          {...FormUtils.fieldProps('storage.writePolicy', currentParent)}
          onChange={FormUtils.handleUpdate('storage.writePolicy', sendParent)}
        >
          {deploymentPolicies?.map(([value, displayName]) => (
            <option key={value} value={value}>
              {displayName}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>

      <NxFieldset
        label={EDITOR.PROPRIETARY_COMPONENTS_LABEL}
        className="nxrm-form-group-is-proprietary"
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('component.proprietaryComponents', currentParent)}
          onChange={FormUtils.handleUpdate('component.proprietaryComponents', sendParent)}
        >
          {EDITOR.PROPRIETARY_COMPONENTS_DESCR}
        </NxCheckbox>
      </NxFieldset>
    </>
  );
}
