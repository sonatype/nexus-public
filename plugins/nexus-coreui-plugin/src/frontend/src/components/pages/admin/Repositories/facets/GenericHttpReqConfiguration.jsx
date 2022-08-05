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

import {
  NxFormGroup,
  NxCheckbox,
  NxTextInput,
  NxAccordion,
  NxStatefulAccordion,
  NxFieldset
} from '@sonatype/react-shared-components';

import UIStrings from '../../../../../constants/UIStrings';

const {EDITOR} = UIStrings.REPOSITORIES;

export default function GenericHttpReqConfiguration({parentMachine}) {
  const [currentParent, sendParent] = parentMachine;

  const {userAgentSuffix, retries, timeout, enableCircularRedirects, enableCookies} =
    currentParent.context.data.httpClient.connection || {};

  const isOpen = Boolean(userAgentSuffix || retries || timeout || enableCircularRedirects || enableCookies);
  
  return (
    <NxStatefulAccordion
      className="nxrm-form-accordion-http-request"
      defaultOpen={isOpen}
    >
      <NxAccordion.Header>
        <NxAccordion.Title>{EDITOR.REQUEST_SETTINGS_CAPTION}</NxAccordion.Title>
      </NxAccordion.Header>

      <NxFormGroup
        label={EDITOR.USER_AGENT_LABEL}
        sublabel={EDITOR.USER_AGEN_SUBLABEL}
        className="nxrm-form-group-user-agent"
      >
        <NxTextInput
          {...FormUtils.fieldProps('httpClient.connection.userAgentSuffix', currentParent)}
          onChange={FormUtils.handleUpdate('httpClient.connection.userAgentSuffix', sendParent)}
        />
      </NxFormGroup>

      <NxFormGroup
        label={EDITOR.RETRIES_LABEL}
        sublabel={EDITOR.RETRIES_SUBLABEL}
        className="nxrm-form-group-retries"
      >
        <NxTextInput
          {...FormUtils.fieldProps('httpClient.connection.retries', currentParent)}
          onChange={FormUtils.handleUpdate('httpClient.connection.retries', sendParent)}
        />
      </NxFormGroup>

      <NxFormGroup
        label={EDITOR.TIMEOUT_LABEL}
        sublabel={EDITOR.TIMEOUT_SUBLABEL}
        className="nxrm-form-group-timeout"
      >
        <NxTextInput
          {...FormUtils.fieldProps('httpClient.connection.timeout', currentParent)}
          onChange={FormUtils.handleUpdate('httpClient.connection.timeout', sendParent)}
        />
      </NxFormGroup>

      <NxFieldset label={EDITOR.REDIRECTS_LABEL} className="nxrm-form-group-redirects">
        <NxCheckbox
          {...FormUtils.checkboxProps(
            'httpClient.connection.enableCircularRedirects',
            currentParent
          )}
          onChange={FormUtils.handleUpdate(
            'httpClient.connection.enableCircularRedirects',
            sendParent
          )}
        >
          {EDITOR.ENABLED_CHECKBOX_DESCR}
        </NxCheckbox>
      </NxFieldset>

      <NxFieldset label={EDITOR.COOKIES_LABEL} className="nxrm-form-group-cookies">
        <NxCheckbox
          {...FormUtils.checkboxProps('httpClient.connection.enableCookies', currentParent)}
          onChange={FormUtils.handleUpdate('httpClient.connection.enableCookies', sendParent)}
        >
          {EDITOR.ENABLED_CHECKBOX_DESCR}
        </NxCheckbox>
      </NxFieldset>
    </NxStatefulAccordion>
  );
}
