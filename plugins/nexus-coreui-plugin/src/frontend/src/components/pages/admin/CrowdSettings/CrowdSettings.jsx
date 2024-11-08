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
import {useMachine, useActor} from '@xstate/react';
import {faTrash} from "@fortawesome/free-solid-svg-icons";
import {faAtlassian} from '@fortawesome/free-brands-svg-icons';
import CrowdSettingsMachine from './CrowdSettingsMachine';
import {
  ContentBody,
  FormUtils,
  Page,
  PageHeader,
  PageTitle,
  Section
} from '@sonatype/nexus-ui-plugin';
import {
  NxButton,
  NxCheckbox,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxStatefulForm,
  NxTextInput,
  NxTooltip,
} from '@sonatype/react-shared-components';
import UIStrings from '../../../../constants/UIStrings';

export default function CrowdSettings() {
  const service = useMachine(CrowdSettingsMachine, {devTools: true})[2];
  return <CrowdSettingsForm service={service}/>;
}

export function CrowdSettingsForm({service}) {
  const [current, send] = useActor(service);

  const {data, isPristine, validationErrors} = current.context;
  const isInvalid = FormUtils.isInvalid(validationErrors);

  function discard() {
    send({type: 'RESET'});
  }

  function verifyConnection() {
    send({type: 'VERIFY-CONNECTION'});
  }

  function clearCache() {
    send({type: 'CLEAR-CACHE'});
  }

  return <Page>
    <PageHeader><PageTitle icon={faAtlassian} {...UIStrings.CROWD_SETTINGS.MENU}/></PageHeader>
    <ContentBody className='nxrm-crowd-settings'>
      <Section>
        <NxStatefulForm
            {...FormUtils.formProps(current, send)}
            additionalFooterBtns={<>
              <NxTooltip title={FormUtils.discardTooltip({isPristine})}>
                <NxButton type="button" className={(isPristine) && 'disabled'} onClick={discard}>
                  {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
                </NxButton>
              </NxTooltip>
              <NxButton type="button" disabled={isInvalid} onClick={verifyConnection}>
                {UIStrings.CROWD_SETTINGS.BUTTONS.VERIFY_BUTTON_LABEL}
              </NxButton>
              <NxButton type="button" onClick={clearCache}>
                <NxFontAwesomeIcon icon={faTrash}/>
                <span>{UIStrings.CROWD_SETTINGS.BUTTONS.CLEAR_BUTTON_LABEL}</span>
              </NxButton>
            </>}>
          <NxFormGroup label={UIStrings.CROWD_SETTINGS.FIELDS.enabledLabel}>
            <NxCheckbox
                {...FormUtils.checkboxProps('enabled', current)}
                onChange={FormUtils.handleUpdate('enabled', send)}>
              {UIStrings.CROWD_SETTINGS.FIELDS.enabledDescription}
            </NxCheckbox>
          </NxFormGroup>
          <NxFormGroup label={UIStrings.CROWD_SETTINGS.FIELDS.realmActiveLabel}>
            <NxCheckbox
                {...FormUtils.checkboxProps('realmActive', current)}
                onChange={FormUtils.handleUpdate('realmActive', send)}>
              <span dangerouslySetInnerHTML={UIStrings.CROWD_SETTINGS.FIELDS.realmActiveDescription}/>
            </NxCheckbox>
          </NxFormGroup>
          <NxFormGroup
              label={UIStrings.CROWD_SETTINGS.FIELDS.urlLabel}
              sublabel={UIStrings.CROWD_SETTINGS.FIELDS.urlDescription}
              isRequired>
            <NxTextInput
                className="nx-text-input--long"
                {...FormUtils.fieldProps('url', current)}
                onChange={FormUtils.handleUpdate('url', send)}/>
          </NxFormGroup>
          {data.url?.startsWith('https') && <>
            <NxFormGroup
                label={UIStrings.CROWD_SETTINGS.FIELDS.useTrustStoreLabel}
                sublabel={<span dangerouslySetInnerHTML={UIStrings.CROWD_SETTINGS.FIELDS.useTrustStoreDescription}/>}
                isRequired>
              <NxCheckbox
                  {...FormUtils.checkboxProps('useTrustStoreForUrl', current)}
                  onChange={FormUtils.handleUpdate('useTrustStoreForUrl', send)}>
                {UIStrings.CROWD_SETTINGS.FIELDS.useTrustStoreText}
              </NxCheckbox>
            </NxFormGroup>
          </>}
          <NxFormGroup label={UIStrings.CROWD_SETTINGS.FIELDS.applicationNameLabel} isRequired>
            <NxTextInput
                autoComplete="off"
                {...FormUtils.fieldProps('applicationName', current)}
                onChange={FormUtils.handleUpdate('applicationName', send)}/>
          </NxFormGroup>
          <NxFormGroup label={UIStrings.CROWD_SETTINGS.FIELDS.applicationPasswordLabel} isRequired>
            <NxTextInput
                type="password"
                autoComplete="new-password"
                {...FormUtils.fieldProps('applicationPassword', current)}
                onChange={FormUtils.handleUpdate('applicationPassword', send)}/>
          </NxFormGroup>
          <NxFormGroup
              label={UIStrings.CROWD_SETTINGS.FIELDS.timeoutLabel}
              sublabel={UIStrings.CROWD_SETTINGS.FIELDS.timeoutDescription}>
            <NxTextInput
                {...FormUtils.fieldProps('timeout', current)}
                onChange={FormUtils.handleUpdate('timeout', send)}/>
          </NxFormGroup>
        </NxStatefulForm>
      </Section>
    </ContentBody>
  </Page>;
}
