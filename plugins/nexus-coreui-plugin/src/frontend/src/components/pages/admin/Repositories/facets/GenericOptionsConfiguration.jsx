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

import {FormUtils, useSimpleMachine} from '@sonatype/nexus-ui-plugin';

import {
  NxFormGroup,
  NxLoadWrapper,
  NxTextInput,
  NxCheckbox,
  NxFieldset,
  NxFormSelect
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export const ROUTING_RULES_URL = 'service/rest/internal/ui/routing-rules';

export default function GenericOptionsConfiguration({parentMachine}) {
  const {current, retry, isLoading} = useSimpleMachine({
    id: 'GenericOptionsConfigurationMachine',
    url: ROUTING_RULES_URL
  });

  const [currentParent, sendParent] = parentMachine;

  const {data: routingRules, error} = current.context;

  const isNegativeCacheEnabled = currentParent.context.data.negativeCache?.enabled;

  return (
    <>
      <h2 className="nx-h2">{EDITOR.OPTIONS_CAPTION}</h2>
      <NxLoadWrapper loading={isLoading} error={error} retryHandler={retry}>
        <NxFormGroup label={EDITOR.ROUTING_RULE_LABEL} className="nxrm-form-group-routing-rule">
          <NxFormSelect
            {...FormUtils.selectProps('routingRule', currentParent)}
            onChange={FormUtils.handleUpdate('routingRule', sendParent)}
          >
            <option value="">{EDITOR.NONE_OPTION}</option>
            {routingRules?.map(({name}) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </NxFormSelect>
        </NxFormGroup>

        <NxFieldset label={EDITOR.NEGATIVE_CACHE_LABEL} className="nxrm-form-group-proxy-cache">
          <NxCheckbox
            {...FormUtils.checkboxProps('negativeCache.enabled', currentParent)}
            onChange={FormUtils.handleUpdate('negativeCache.enabled', sendParent)}
          >
            {EDITOR.ENABLED_CHECKBOX_DESCR}
          </NxCheckbox>
        </NxFieldset>

        <NxFormGroup
          label={EDITOR.NEGATIVE_CACHE_TTL_LABEL}
          sublabel={EDITOR.NEGATIVE_CACHE_TTL_SUBLABEL}
          isRequired
          className="nxrm-form-group-negative-cache-ttl"
        >
          <NxTextInput
            {...FormUtils.fieldProps('negativeCache.timeToLive', currentParent)}
            onChange={FormUtils.handleUpdate('negativeCache.timeToLive', sendParent)}
            disabled={!isNegativeCacheEnabled}
            className="nx-text-input--short"
          />
        </NxFormGroup>
      </NxLoadWrapper>
    </>
  );
}
