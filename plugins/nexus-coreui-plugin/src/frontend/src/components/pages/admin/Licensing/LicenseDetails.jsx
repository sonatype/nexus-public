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
import {useActor} from '@xstate/react';

import {
  NxReadOnly,
  NxLoadWrapper,
  NxGrid,
  NxH2,
  NxTile,
} from '@sonatype/react-shared-components';
import {
  DateUtils,
  ReadOnlyField,
} from '@sonatype/nexus-ui-plugin';

import UIStrings from '../../../../constants/UIStrings';

const {LICENSING: {DETAILS: LABELS}, LICENSING} = UIStrings;

export default function LicenseDetails({service}) {
  const [state, send] = useActor(service);

  const {
    data: {
      licenseType,
      contactCompany,
      contactName,
      contactEmail,
      effectiveDate,
      expirationDate,
      licensedUsers,
      fingerprint,
    },
    loadError,
  } = state.context;
  const isLoading = state.matches('loading');

  const licenseTypes = licenseType?.split(',');
  const effectiveDateLabel = DateUtils.prettyDate(effectiveDate);
  const expirationDateLabel = DateUtils.prettyDate(expirationDate);

  const retry = () => send({type: 'RETRY'});

  return (
      <NxTile>
        <NxTile.Content>
          <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={retry}>
            <NxH2>{LICENSING.SECTIONS.DETAILS}</NxH2>
            <NxGrid.Row>
              <NxGrid.Column className="nx-grid-col--25">
                <NxReadOnly>
                  <ReadOnlyField label={LABELS.COMPANY.LABEL} value={contactCompany}/>
                  <ReadOnlyField label={LABELS.NAME.LABEL} value={contactName}/>
                  <ReadOnlyField label={LABELS.EMAIL.LABEL} value={contactEmail}/>
                </NxReadOnly>
              </NxGrid.Column>
              <NxGrid.Column>
                <NxReadOnly className="nx-read-only--grid licence-details">
                  <NxReadOnly.Item>
                    <ReadOnlyField label={LABELS.EFFECTIVE_DATE.LABEL} value={effectiveDateLabel}/>
                  </NxReadOnly.Item>
                  <NxReadOnly.Item>
                    <ReadOnlyField label={LABELS.EXPIRATION_DATE.LABEL} value={expirationDateLabel}/>
                  </NxReadOnly.Item>
                  <NxReadOnly.Item/>
                  <NxReadOnly.Item>
                    <NxReadOnly.Label>{LABELS.LICENSE_TYPES.LABEL}</NxReadOnly.Label>
                    {licenseTypes?.map(type =>
                        <NxReadOnly.Data key={type}>{type}</NxReadOnly.Data>
                    )}
                  </NxReadOnly.Item>
                  <NxReadOnly.Item>
                    <ReadOnlyField label={LABELS.NUMBER_OF_USERS.LABEL} value={licensedUsers}/>
                  </NxReadOnly.Item>
                  <NxReadOnly.Item>
                    <ReadOnlyField label={LABELS.FINGERPRINT.LABEL} value={fingerprint}/>
                  </NxReadOnly.Item>
                </NxReadOnly>
              </NxGrid.Column>
            </NxGrid.Row>
          </NxLoadWrapper>
        </NxTile.Content>
      </NxTile>
  );
}
