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
  NxCheckbox,
  NxFieldset,
  NxTextLink,
  NxWarningAlert
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {
  NPM: {REMOVE_QUARANTINED},
  LEARN_MORE
} = UIStrings.REPOSITORIES.EDITOR;

const getSubLabel = (txt) => (
  <>
    {txt}{' '}
    <NxTextLink href="http://links.sonatype.com/products/nxrm3/docs/npm-with-firewall" external>
      {LEARN_MORE}
    </NxTextLink>
  </>
);

export default function NpmConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  return (
    <>
      <NxFieldset
        label={REMOVE_QUARANTINED.LABEL}
        sublabel={getSubLabel(REMOVE_QUARANTINED.SUBLABEL)}
        className="nxrm-form-group-remove-quarantined"
      >
        <NxCheckbox
          {...FormUtils.checkboxProps('npm.removeQuarantined', currentParent)}
          onChange={FormUtils.handleUpdate('npm.removeQuarantined', sendParent)}
        >
          {REMOVE_QUARANTINED.DESCR}
        </NxCheckbox>
        <NxWarningAlert>{REMOVE_QUARANTINED.WARNING}</NxWarningAlert>
      </NxFieldset>
    </>
  );
}
