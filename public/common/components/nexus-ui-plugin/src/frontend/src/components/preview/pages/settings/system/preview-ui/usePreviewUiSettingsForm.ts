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

import { useMemo } from 'react';
import { APIConstants } from '../../../../../../constants/APIConstants';
import { restClient } from '../../../../../../interface/api';
import { useForm, type FormContext } from '../../../../../../interface/form';
import { clearDirtyState, useToast } from '../../../../shared';
import { PREVIEW_UI_SETTINGS_FORM_ID, createPreviewUiSettingsFormMachine, type PreviewUiSettings } from './previewUiSettingsFormMachine';

export function usePreviewUiSettingsForm() {
  const toast = useToast();
  const machine = useMemo(() => createPreviewUiSettingsFormMachine(), []);

  const form = useForm(machine, {
    services: {
      save: async (ctx: FormContext<PreviewUiSettings>) => {
        try {
          await restClient.put(APIConstants.REST.INTERNAL.PREVIEW_UI_SETTINGS, ctx.data);
          clearDirtyState(PREVIEW_UI_SETTINGS_FORM_ID);
          window.location.reload();
        } catch (err) {
          const message = err instanceof Error ? err.message : 'Failed to save Nexus One UI settings';
          toast.error(message);
          throw new Error(message);
        }
      },
    },
  });

  return form;
}
