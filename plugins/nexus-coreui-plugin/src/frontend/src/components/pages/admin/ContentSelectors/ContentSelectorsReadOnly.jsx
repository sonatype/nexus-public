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

import {FormUtils, Section} from '@sonatype/nexus-ui-plugin';

import {
  NxButton,
  NxReadOnly,
  NxTile,
  NxFooter,
  NxButtonBar,
  NxInfoAlert
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../constants/UIStrings';

export default function ContentSelectorsReadOnly({service, onDone}) {
  const [state] = useActor(service);

  const {data} = state.context;

  const cancel = () => onDone();

  return <Section className="nxrm-content-selectors-read-only">
    <NxTile.Content>
      <NxInfoAlert>{UIStrings.SETTINGS.READ_ONLY.WARNING}</NxInfoAlert>
      <NxReadOnly>
        <NxReadOnly.Label>{UIStrings.CONTENT_SELECTORS.NAME_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{FormUtils.fieldProps('name', state).value}</NxReadOnly.Data>
        <NxReadOnly.Label>{UIStrings.CONTENT_SELECTORS.TYPE_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{data.type?.toUpperCase()}</NxReadOnly.Data>
        <NxReadOnly.Label>{UIStrings.CONTENT_SELECTORS.DESCRIPTION_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{FormUtils.fieldProps('description', state).value}</NxReadOnly.Data>
        <NxReadOnly.Label>{UIStrings.CONTENT_SELECTORS.EXPRESSION_LABEL}</NxReadOnly.Label>
        <NxReadOnly.Data>{FormUtils.fieldProps('expression', state).value}</NxReadOnly.Data>
      </NxReadOnly>
    </NxTile.Content>
    <NxFooter>
      <NxButtonBar>
        <NxButton type="button" onClick={cancel}>{UIStrings.SETTINGS.CANCEL_BUTTON_LABEL}</NxButton>
      </NxButtonBar>
    </NxFooter>
  </Section>
}
