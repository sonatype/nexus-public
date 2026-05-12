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

import { assign } from 'xstate';
import { createFormMachine, restClient, APIConstants } from '@sonatype/nexus-ui-plugin';
import type { FormContext, ValidationErrors } from '@sonatype/nexus-ui-plugin';

export interface PreviewUiSettings {
  anonymousEnabled: boolean;
  loggedInEnabled: boolean;
  defaultToPreviewUi: boolean;
  disableLegacyUi: boolean;
  disableSwitchFeedback: boolean;
}

export const PREVIEW_UI_SETTINGS_FORM_ID = 'preview-ui-settings-form';

export const DEFAULT_PREVIEW_UI_SETTINGS: PreviewUiSettings = {
  anonymousEnabled: false,
  loggedInEnabled: false,
  defaultToPreviewUi: false,
  disableLegacyUi: false,
  disableSwitchFeedback: false,
};

export const LOCKOUT_ERROR_MESSAGE =
  'Cannot disable Classic UI when both Anonymous and Logged-in access to Nexus One UI are disabled. ' +
  'At least one UI access method must remain enabled to prevent lockout.';

function validatePreviewUiSettings(data: PreviewUiSettings): ValidationErrors {
  const errors: ValidationErrors = {};
  if (data.disableLegacyUi && !data.anonymousEnabled && !data.loggedInEnabled) {
    errors.disableLegacyUi = LOCKOUT_ERROR_MESSAGE;
  }
  return errors;
}

export function createPreviewUiSettingsFormMachine() {
  return createFormMachine<PreviewUiSettings>({
    id: PREVIEW_UI_SETTINGS_FORM_ID,
    context: {
      data: { ...DEFAULT_PREVIEW_UI_SETTINGS },
    },
    actions: {
      // createFormMachine invokes the 'validate' named action on UPDATE events and after load.
      // This contract is verified by previewUiSettingsFormMachine.test.ts (shouldReportErrorWhenDisableLegacyAndBothAccessDisabled).
      validate: assign((ctx: FormContext<PreviewUiSettings>) => ({
        validationErrors: validatePreviewUiSettings(ctx.data),
      })),
    },
    services: {
      load: async () => {
        const data = await restClient.get<PreviewUiSettings>(APIConstants.REST.INTERNAL.PREVIEW_UI_SETTINGS);
        return { data };
      },
      save: async (ctx) => {
        await restClient.put(APIConstants.REST.INTERNAL.PREVIEW_UI_SETTINGS, ctx.data);
      },
    },
  });
}
