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

import {
  NxFormGroup,
  NxCheckbox,
  NxTextInput,
  NxFieldset,
  NxReadOnly
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GenericNameConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {
    pristineData: {name},
    data: {url}
  } = currentParent.context;
  const isEdit = !!name;

  return (
    <>
      <h2 className="nx-h2">{EDITOR.CONFIGURATION_CAPTION}</h2>
      <NxFormGroup label={EDITOR.NAME_LABEL} isRequired className="nxrm-form-group-name">
        <NxTextInput
          {...FormUtils.fieldProps('name', currentParent)}
          onChange={FormUtils.handleUpdate('name', sendParent)}
          disabled={isEdit}
        />
      </NxFormGroup>

      {isEdit && (
        <NxReadOnly>
          <NxReadOnly.Label>{EDITOR.URL_LABEL}</NxReadOnly.Label>
          <NxReadOnly.Data>{url}</NxReadOnly.Data>
        </NxReadOnly>
      )}

      <NxFieldset label={EDITOR.STATUS_LABEL} className="nxrm-form-group-status">
        <NxCheckbox
          {...FormUtils.checkboxProps('online', currentParent)}
          onChange={FormUtils.handleUpdate('online', sendParent)}
        >
          {EDITOR.STATUS_DESCR}
        </NxCheckbox>
      </NxFieldset>
    </>
  );
}
