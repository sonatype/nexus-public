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

import {FormUtils, useSimpleMachine} from '@sonatype/nexus-ui-plugin';

import {
  NxFormGroup,
  NxStatefulTransferList,
  NxLoadWrapper
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export const cleanupPoliciesUrl = (event) =>
  '/service/rest/internal/cleanup-policies?format=' +
  encodeURIComponent(event.format);

export default function GenericCleanupConfiguration({parentMachine}) {
  const {current, load, retry, isLoading} = useSimpleMachine(
    'GenericCleanupConfigurationMachine',
    cleanupPoliciesUrl
  );

  const [currentParent, sendParent] = parentMachine;

  const {format, policyNames} = currentParent.context.data;

  useEffect(() => {
    load({format});
  }, [format]);

  const {data: policies, error} = current.context;

  const availablePolicies =
    policies?.map((it) => ({id: it.name, displayName: it.name})) || [];

  return (
    <>
      <h2 className="nx-h2">{EDITOR.CLEANUP_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        <NxFormGroup
          label={EDITOR.CLEANUP_POLICIES_LABEL}
          sublabel={EDITOR.CLEANUP_POLICIES_SUBLABEL}
        >
          <NxStatefulTransferList
            allItems={availablePolicies}
            selectedItems={new Set(policyNames)}
            onChange={FormUtils.handleUpdate('policyNames', sendParent)}
          />
        </NxFormGroup>
      </NxLoadWrapper>
    </>
  );
}
