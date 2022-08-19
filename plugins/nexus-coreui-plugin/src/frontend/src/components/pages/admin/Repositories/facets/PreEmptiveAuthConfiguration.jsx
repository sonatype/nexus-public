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

import {FormUtils, ValidationUtils} from '@sonatype/nexus-ui-plugin';

import {NxCheckbox, NxFieldset, NxTooltip} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

import './PreEmptiveAuthConfiguration.scss';

const {LABEL, DESCR, TOOLTIP} = UIStrings.REPOSITORIES.EDITOR.PRE_EMPTIVE_AUTH;

const PROP_PATH = 'httpClient.authentication.preemptive';

export default function PreEmptiveAuthConfiguration({parentMachine}) {
  const [parentState, sendParent] = parentMachine;

  const {
    proxy: {remoteUrl},
    httpClient: {authentication}
  } = parentState.context.data;

  const hasSecureRemoteUrl = ValidationUtils.isSecureUrl(remoteUrl);
  const isDisabled = !hasSecureRemoteUrl;
  const isVisible = !!authentication;

  useEffect(() => {
    if (isVisible && !isDisabled) {
      sendParent({type: 'ADD_DATA_PROPERTY', path: PROP_PATH, value: false});
    } else {
      sendParent({type: 'DELETE_DATA_PROPERTY', path: PROP_PATH});
    }
  }, [isVisible, isDisabled]);

  return (
    <>
      {isVisible && (
        <NxFieldset label={LABEL}>
          <NxTooltip title={isDisabled ? TOOLTIP : null}>
            <NxCheckbox
              {...FormUtils.checkboxProps(PROP_PATH, parentState)}
              onChange={FormUtils.handleUpdate(PROP_PATH, sendParent)}
              disabled={isDisabled}
              overflowTooltip={!isDisabled}
            >
              {DESCR}
            </NxCheckbox>
          </NxTooltip>
        </NxFieldset>
      )}
    </>
  );
}
