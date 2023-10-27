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
import {NxCheckbox, NxFormGroup, NxInfoAlert, NxTextInput} from '@sonatype/react-shared-components';
import {isReleaseType} from './CleanupPoliciesHelper';
import {ExtJS, FormUtils} from '@sonatype/nexus-ui-plugin';
import {useActor} from '@xstate/react';
import UIStrings from '../../../../constants/UIStrings';

const {CLEANUP_POLICIES: LABELS} = UIStrings;

export default function CleanupExclusionCriteria({actor}) {
  const [state, send] = useActor(actor);

  const {
    criteriaByFormat,
    data: {format, criteriaReleaseType},
    exclusionCriteriaEnabled,
    exclusionSortBy
  } = state.context;

  const isNormalizedVersionTaskDone = ExtJS.state().getValue(
      `${format}.normalized.version.available`
  );

  const isPreRelease = () => criteriaByFormat?.some(
      ({id, availableCriteria}) => id === format && availableCriteria.includes("isPrerelease"));

  const setExclusionCriteriaEnabled = ({target}) => {
    if (isNormalizedVersionTaskDone) {
      send({type: 'SET_EXCLUSION_CRITERIA_ENABLED', checked: target.checked});
    }
  }

  const setRetainValue = (value) => {
    send({type: 'UPDATE_RETAIN', name: 'retain', value });
  }

  return (
      <>
        {!isNormalizedVersionTaskDone && (
            <NxInfoAlert className="retain-n__alert">
              {LABELS.EXCLUSION_CRITERIA.NORMALIZED_VERSION_ALERT}
            </NxInfoAlert>
        )}
        {((isPreRelease() && !isReleaseType(criteriaReleaseType)) &&
            isNormalizedVersionTaskDone) && (
            <NxInfoAlert className="retain-n__alert">
              {LABELS.EXCLUSION_CRITERIA.ALERT}
            </NxInfoAlert>
        )}
        <NxCheckbox
            isChecked={Boolean(exclusionCriteriaEnabled)}
            onChange={setExclusionCriteriaEnabled}
            disabled={
                !isNormalizedVersionTaskDone || (isPreRelease() &&
                    !isReleaseType(criteriaReleaseType))
            }
            className="retain-n__checkbox"
        >
          <b>{LABELS.EXCLUSION_CRITERIA.LABEL}</b>
        </NxCheckbox>
        <NxFormGroup
            label={LABELS.EXCLUSION_CRITERIA.VERSION_LABEL}
            isRequired={exclusionCriteriaEnabled}
            sublabel={LABELS.EXCLUSION_CRITERIA.SUB_LABEL(exclusionSortBy, LABELS.EXCLUSION_CRITERIA.SORT_BY)}
            className="retain-n__group"
        >
          <NxTextInput
              className="nx-text-input--short"
              disabled={!exclusionCriteriaEnabled}
              {...FormUtils.fieldProps('retain', state)}
              onChange={setRetainValue}
          />
        </NxFormGroup>
      </>
  );
}
