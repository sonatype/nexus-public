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
import React, {useEffect, useState} from 'react';

import Axios from 'axios';

import {FormUtils} from '@sonatype/nexus-ui-plugin';

import {
  NxFieldset,
  NxFormGroup,
  NxFormSelect,
  NxInfoAlert
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {FIREWALL} = UIStrings.REPOSITORIES.EDITOR;

const FIREWALL_FORMAT_CAPABILITIES_URL = 'service/rest/v1/repositories/firewall/format-capabilities';

export default function FirewallConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;
  const format = currentParent.context.data?.format;

  const [pccsFormats, setPccsFormats] = useState(['npm', 'pypi']);

  useEffect(() => {
    let cancelled = false;
    Axios.get(FIREWALL_FORMAT_CAPABILITIES_URL)
        .then(({data}) => {
          if (!cancelled) setPccsFormats(data.filter(c => c.pccsModeSupported).map(c => c.format));
        })
        .catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const isPccsValidFormat = pccsFormats.includes(format);

  return (
    <>
      <h2 className="nx-h2">{FIREWALL.CAPTION}</h2>

      <NxFieldset label={FIREWALL.LABEL} sublabel={FIREWALL.SUBLABEL}>
        <NxFormGroup
          label={FIREWALL.MODE_LABEL}
          className="nxrm-form-group-firewall-mode"
        >
          <NxFormSelect
            {...FormUtils.selectProps('firewall.mode', currentParent)}
            onChange={FormUtils.handleUpdate('firewall.mode', sendParent)}
          >
            <option value="DISABLED">{FIREWALL.MODE_DISABLED}</option>
            <option value="AUDIT">{FIREWALL.MODE_AUDIT}</option>
            <option value="QUARANTINE">{FIREWALL.MODE_QUARANTINE}</option>
            {isPccsValidFormat && (
              <option value="PCCS">{FIREWALL.MODE_PCCS}</option>
            )}
          </NxFormSelect>
        </NxFormGroup>

        <NxInfoAlert>{FIREWALL.WARNING}</NxInfoAlert>
      </NxFieldset>
    </>
  );
}
