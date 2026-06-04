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
import {
  ContentBody,
  Page,
  PageHeader,
  PageTitle,
  Section,
} from '../../../../layout';
import { ExtJS } from '../../../../../interface/ExtJS';
import { FormUtils } from '../../../../../interface/FormUtils';
import {
  NxLoadWrapper,
  NxCheckbox,
  NxButton,
  NxFieldset,
  NxStatefulForm,
  NxTooltip,
} from '@sonatype/react-shared-components';

import UIStrings from '../../../constants/UIStrings';
import { usePreviewUiSettingsForm } from '../../settings/system/preview-ui/usePreviewUiSettingsForm';

export default function PreviewUiSettings() {
  const canEdit = ExtJS.checkPermission('nexus:settings:update');
  const form = usePreviewUiSettingsForm();

  const anonymousCb = form.checkbox('anonymousEnabled');
  const loggedInCb = form.checkbox('loggedInEnabled');
  const defaultToPreviewCb = form.checkbox('defaultToPreviewUi');
  const disableSwitchFeedbackCb = form.checkbox('disableSwitchFeedback');

  return (
    <Page>
      <PageHeader>
        <PageTitle
          icon={UIStrings.PREVIEW_UI_SETTINGS.MENU.icon}
          text={UIStrings.PREVIEW_UI_SETTINGS.MENU.text}
          description={UIStrings.PREVIEW_UI_SETTINGS.description}
        />
      </PageHeader>
      <ContentBody className="nxrm-preview-ui-settings">
        <Section>
          <NxLoadWrapper
            loading={form.isLoading}
            error={form.loadError}
            retryHandler={() => form.send({ type: 'RETRY' })}
          >
            <NxStatefulForm
              onSubmit={() => form.submit()}
              loading={form.isSaving}
              submitError={form.saveError ? String(form.saveError) : null}
              validationErrors={form.hasValidationErrors ? Object.values(form.validationErrors).find(Boolean) : null}
              additionalFooterBtns={
                <NxTooltip title={FormUtils.discardTooltip({ isPristine: form.isPristine })}>
                  <NxButton
                    type="button"
                    className={form.isPristine ? 'disabled' : undefined}
                    onClick={() => form.reset()}
                  >
                    {UIStrings.SETTINGS.DISCARD_BUTTON_LABEL}
                  </NxButton>
                </NxTooltip>
              }
            >
              {() => <>
                {/* NEXUS-52024: Disable Classic UI checkbox hidden until Nexus One UI reaches feature parity */}

                <NxFieldset label={UIStrings.PREVIEW_UI_SETTINGS.ANONYMOUS.label}>
                  <NxCheckbox
                    checkboxId="anonymousEnabled"
                    isChecked={anonymousCb.checked}
                    onChange={() => anonymousCb.onChange(!anonymousCb.checked)}
                    disabled={!canEdit}
                  >
                    {UIStrings.PREVIEW_UI_SETTINGS.ANONYMOUS.description}
                  </NxCheckbox>
                </NxFieldset>

                <NxFieldset label={UIStrings.PREVIEW_UI_SETTINGS.LOGGEDIN.label}>
                  <NxCheckbox
                    checkboxId="loggedInEnabled"
                    isChecked={loggedInCb.checked}
                    onChange={() => loggedInCb.onChange(!loggedInCb.checked)}
                    disabled={!canEdit}
                  >
                    {UIStrings.PREVIEW_UI_SETTINGS.LOGGEDIN.description}
                  </NxCheckbox>
                </NxFieldset>

                <NxFieldset label={UIStrings.PREVIEW_UI_SETTINGS.ROLLOUT.title}>
                  <NxCheckbox
                    checkboxId="defaultToPreviewUi"
                    isChecked={defaultToPreviewCb.checked}
                    onChange={() => defaultToPreviewCb.onChange(!defaultToPreviewCb.checked)}
                    disabled={!canEdit}
                  >
                    {UIStrings.PREVIEW_UI_SETTINGS.ROLLOUT.DEFAULT_TO_PREVIEW.helpText}
                  </NxCheckbox>
                  <NxCheckbox
                    checkboxId="disableSwitchFeedback"
                    isChecked={disableSwitchFeedbackCb.checked}
                    onChange={() => disableSwitchFeedbackCb.onChange(!disableSwitchFeedbackCb.checked)}
                    disabled={!canEdit}
                  >
                    {UIStrings.PREVIEW_UI_SETTINGS.ROLLOUT.DISABLE_SWITCH_FEEDBACK.helpText}
                  </NxCheckbox>
                </NxFieldset>
              </>}
            </NxStatefulForm>
          </NxLoadWrapper>
        </Section>
      </ContentBody>
    </Page>
  );
}
