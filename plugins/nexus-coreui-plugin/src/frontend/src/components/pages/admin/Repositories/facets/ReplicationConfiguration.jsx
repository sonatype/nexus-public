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

import {NxCheckbox, NxFieldset} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function ReplicationConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  return (
    <NxFieldset
      label={EDITOR.REPLICATION_LABEL}
      sublabel={EDITOR.REPLICATION_SUBLABEL}
      className="nxrm-form-group-replication"
    >
      <NxCheckbox
        {...FormUtils.checkboxProps('replication.enabled', currentParent)}
        onChange={FormUtils.handleUpdate('replication.enabled', sendParent)}
      >
        {EDITOR.ENABLED_CHECKBOX_DESCR}
      </NxCheckbox>
    </NxFieldset>
  );
}
