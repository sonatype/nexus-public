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

import {NxFormGroup, NxTextInput} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {KEY, PASSPHRASE} = UIStrings.REPOSITORIES.EDITOR.APT.SIGNING;

export default function AptSigningConfiguration({parentMachine}) {
  const [parentState, sendParent] = parentMachine;

  return (
    <>
      <NxFormGroup label={KEY.LABEL} sublabel={KEY.SUBLABEL} isRequired>
        <NxTextInput
          type="textarea"
          {...FormUtils.fieldProps('aptSigning.keypair', parentState)}
          onChange={FormUtils.handleUpdate('aptSigning.keypair', sendParent)}
          className="nx-text-input--long"
          placeholder={KEY.PLACEHOLDER}
        />
      </NxFormGroup>

      <NxFormGroup label={PASSPHRASE.LABEL} sublabel={PASSPHRASE.SUBLABEL}>
        <NxTextInput
          type="password"
          {...FormUtils.fieldProps('aptSigning.passphrase', parentState)}
          onChange={FormUtils.handleUpdate('aptSigning.passphrase', sendParent)}
        />
      </NxFormGroup>
    </>
  );
}
