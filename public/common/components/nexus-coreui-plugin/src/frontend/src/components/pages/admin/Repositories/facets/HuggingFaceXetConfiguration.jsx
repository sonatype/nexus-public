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

import {ExtJS, FormUtils} from '@sonatype/nexus-ui-plugin';
import {NxFieldset, NxCheckbox, NxWarningAlert} from '@sonatype/react-shared-components';

import FeatureFlags from '../../../../../constants/FeatureFlags';
import UIStrings from '../../../../../constants/UIStrings';

const {HUGGING_FACE_XET_ENABLED} = FeatureFlags;
const {HUGGING_FACE_XET} = UIStrings.REPOSITORIES.EDITOR;

/**
 * HuggingFace proxy repository per-repo XET/LFS transfer-protocol toggle.
 *
 * Hidden entirely when the global {@code nexus.format.huggingface.xet.enabled} flag is
 * false — under that condition the backend does not wire XET routes for any repository, so
 * the toggle would be non-functional.
 *
 * Defaults (NEXUS-48685, parity with super UI):
 *   - Existing pre-feature repositories carry no persisted {@code xet} block; their GET
 *     responses omit the field entirely. formData.xet is undefined and the checkbox
 *     renders UNCHECKED, preserving legacy LFS behavior for repos upgraded across the
 *     feature boundary.
 *   - New repositories are seeded with {@code xet.enabled=false} in the huggingface_proxy
 *     entry of RepositoryFormConfig so the checkbox renders UNCHECKED at create time.
 *
 * When XET is enabled, Firewall is automatically set to DISABLED because XET and
 * Firewall cannot operate concurrently on the same repository.
 */
export default function HuggingFaceXetConfiguration({parentMachine}) {
  const xetGloballyEnabled = ExtJS.useState(() => ExtJS.state().getValue(HUGGING_FACE_XET_ENABLED));

  if (!xetGloballyEnabled) {
    return null;
  }

  const [currentParent, sendParent] = parentMachine;

  // Default UNCHECKED when the attribute is absent (pre-feature repository on
  // upgrade). Create mode gets xet.enabled=false seeded via defaultValues.
  const isXetEnabled = currentParent.context?.data?.xet?.enabled ?? false;

  function handleXetChange(value) {
    sendParent({type: 'UPDATE', name: 'xet.enabled', value});
    if (value) {
      sendParent({type: 'UPDATE', name: 'firewall.mode', value: 'DISABLED'});
    }
  }

  return (
    <NxFieldset label="Transfer Protocol">
      <NxCheckbox
        {...FormUtils.checkboxProps('xet.enabled', currentParent)}
        isChecked={isXetEnabled}
        onChange={handleXetChange}
      >
        Enable XET
      </NxCheckbox>
      <NxWarningAlert>{HUGGING_FACE_XET.XET_DISABLES_FIREWALL_WARNING}</NxWarningAlert>
    </NxFieldset>
  );
}
