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

const {EDITOR} = UIStrings.REPOSITORIES;

export default function RegistryApiSupportConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  return (
    <>
      <NxH2>{EDITOR.REGISTRY_API_SUPPORT_CAPTION}</NxH2>
      <NxFieldset
        label={EDITOR.REGISTRY_API_SUPPORT_LABEL}
        className="nxrm-form-group-registry-api-support"
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('docker.v1Enabled', currentParent)}
          onChange={FormUtils.handleUpdate('docker.v1Enabled', sendParent)}
        >
          {EDITOR.REGISTRY_API_SUPPORT_DESCR}
        </NxCheckbox>
      </NxFieldset>
    </>
  );
}
