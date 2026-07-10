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

import {NxFormGroup, NxTextInput, NxCheckbox} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {NUGET} = UIStrings.REPOSITORIES.EDITOR;

export default function NugetSymbolServerConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  return (
    <>
      <NxFormGroup
        label={NUGET.SYMBOL_SERVER_URL.LABEL}
        sublabel={NUGET.SYMBOL_SERVER_URL.SUBLABEL}
        className="nxrm-form-group-nuget-symbol-server-url"
      >
        <NxTextInput
          {...FormUtils.fieldProps('nugetProxy.symbolServerUrl', currentParent)}
          onChange={FormUtils.handleUpdate('nugetProxy.symbolServerUrl', sendParent)}
        />
      </NxFormGroup>

      <NxFormGroup
        label={NUGET.ALLOW_ANONYMOUS_SYMBOL_ACCESS.LABEL}
        sublabel={NUGET.ALLOW_ANONYMOUS_SYMBOL_ACCESS.SUBLABEL}
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('nugetProxy.allowAnonymousSymbolAccess', currentParent)}
          onChange={FormUtils.handleUpdate('nugetProxy.allowAnonymousSymbolAccess', sendParent)}
        >
          {NUGET.ALLOW_ANONYMOUS_SYMBOL_ACCESS.LABEL}
        </NxCheckbox>
      </NxFormGroup>
    </>
  );
}
