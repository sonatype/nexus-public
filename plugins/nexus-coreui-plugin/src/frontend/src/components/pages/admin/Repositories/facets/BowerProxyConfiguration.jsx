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

import {NxCheckbox, NxFieldset} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function BowerProxyConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  useEffect(() => {
    sendParent({type: 'UPDATE', name: 'bower.rewritePackageUrls', value: true});
  }, []);

  return (
    <NxFieldset
      label={EDITOR.BOWER.REWRITE_URLS_LABEL}
      className="nxrm-form-group-bower-rewrite-urls"
    >
      <NxCheckbox
        {...FormUtils.checkboxProps('bower.rewritePackageUrls', currentParent)}
        onChange={FormUtils.handleUpdate('bower.rewritePackageUrls', sendParent)}
      >
        {EDITOR.BOWER.REWRITE_URLS_DESCR}
      </NxCheckbox>
    </NxFieldset>
  );
}
