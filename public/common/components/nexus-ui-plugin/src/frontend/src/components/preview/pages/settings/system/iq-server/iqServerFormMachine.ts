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
import Axios from 'axios';
import { createFormMachine, type FormContext, type ValidationErrors } from '../../../../../../interface/form';
import { validateIqConfig } from './iqServerUtils';
import { parsePropertiesString, serializeProperties } from './propertyList';
import { IqServerConfiguration, IqServerFormData, DEFAULT_IQ_CONFIGURATION } from './types';

export const IQ_API = 'service/rest/v1/iq';

export const DEFAULT_IQ_FORM_DATA: IqServerFormData = {
  ...DEFAULT_IQ_CONFIGURATION,
  properties: [],
  propertiesDroppedLineCount: 0,
};

/** Wire config -> form data. Parses `properties` exactly once so row ids stay stable
 *  through the factory's deep-equality dirty check. */
export function toFormData(config: IqServerConfiguration): IqServerFormData {
  const { properties, droppedLineCount } = parsePropertiesString(config?.properties || '');
  return {
    ...DEFAULT_IQ_FORM_DATA,
    ...config,
    properties,
    propertiesDroppedLineCount: droppedLineCount,
  };
}

/** Form data -> wire config, ready to PUT. propertiesDroppedLineCount is informational
 *  only and never sent to the server. */
export function toUpdatePayload(data: IqServerFormData): IqServerConfiguration {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { propertiesDroppedLineCount, ...rest } = data;
  return {
    ...rest,
    properties: serializeProperties(data.properties),
  };
}

export async function fetchIqSettings(): Promise<IqServerFormData> {
  try {
    const response = await Axios.get(IQ_API);
    const data = { ...DEFAULT_IQ_CONFIGURATION, ...response.data };
    if (!data.authenticationType && data.username) data.authenticationType = 'USER';
    return toFormData(data);
  } catch (err: any) {
    throw new Error(err?.response?.data?.message || err?.message || 'Failed to load IQ Server settings');
  }
}

export async function saveIqSettings(formData: IqServerFormData): Promise<IqServerFormData> {
  const payload = toUpdatePayload(formData);
  await Axios.put(IQ_API, payload);
  const response = await Axios.get(IQ_API);
  const serverData = response.data || {};
  // These four fields are local-only because the IQ Server GET response never echoes them
  // back today; they're explicitly preserved here so a save doesn't silently discard them.
  // If the API starts returning any of them, `...serverData` below wins and this merge
  // silently switches to server-wins for that field.
  const data: IqServerConfiguration = {
    ...DEFAULT_IQ_CONFIGURATION,
    authenticationType: payload.authenticationType,
    useTrustStoreForUrl: payload.useTrustStoreForUrl,
    timeoutSeconds: payload.timeoutSeconds,
    properties: payload.properties,
    ...serverData,
  };
  if (!data.authenticationType && data.username) data.authenticationType = 'USER';
  // Keep the caller's own properties array (stable row ids) rather than re-parsing the
  // round-tripped string, so rows don't remount / lose focus after a save. The dropped-line
  // count is reset to 0: the string on the wire is exactly serializeProperties(formData.properties),
  // which by construction has no unparseable lines.
  return { ...toFormData(data), properties: formData.properties, propertiesDroppedLineCount: 0 };
}

export function createIqServerFormMachine() {
  return createFormMachine<IqServerFormData>({
    id: 'iq-server-form',
    stayEditableAfterSave: true,
    context: { data: { ...DEFAULT_IQ_FORM_DATA } },
    on: {
      CLEAR_SAVE_ERROR: { actions: 'clearSaveError' },
    },
    actions: {
      validate: assign((ctx: FormContext<IqServerFormData>) => ({
        validationErrors: validateIqConfig(ctx.data, ctx.pristineData) as ValidationErrors,
      })),
      // Apply the merged re-GET result as new data + pristine (factory default discards it).
      onSaveSuccess: assign((_ctx, event: unknown) => {
        const saved = (event as { data?: IqServerFormData })?.data as IqServerFormData;
        return { data: saved, pristineData: saved, isPristine: true, touched: {} };
      }),
    },
    services: {
      load: async () => ({ data: await fetchIqSettings() }),
      // Base save (used by machine tests). Hook overrides to add toast + capabilities refresh.
      save: async (ctx: FormContext<IqServerFormData>) => saveIqSettings(ctx.data),
    },
  });
}
