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

import Axios from 'axios';
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
    // GET /v1/iq returns an all-null XO while IQ has never been configured. A plain spread
    // would let those nulls overwrite the '' string defaults (url, username, password,
    // properties), and the form then calls string methods like url.trim() on null and
    // crashes. Drop null/undefined before spreading so the defaults stand for any field the
    // server left empty. timeoutSeconds is legitimately nullable ("use the global timeout"),
    // but its default is null too, so dropping it here is a no-op.
    const raw = (response.data ?? {}) as Record<string, unknown>;
    const cleaned = Object.fromEntries(
      Object.entries(raw).filter(([, value]) => value !== null && value !== undefined),
    );
    const data = { ...DEFAULT_IQ_CONFIGURATION, ...cleaned };
    // The API rejects a null/blank authenticationType (@NotNull on IqConnectionXo), and the
    // dropdown already displays USER for an absent value, so normalize a still-blank value
    // here instead of shipping one the form never shows. Previously this only fired when a
    // username was present, which is never true on a first-time setup.
    if (!data.authenticationType) data.authenticationType = 'USER';
    return toFormData(data);
  } catch (err: any) {
    throw new Error(err?.response?.data?.message || err?.message || 'Failed to load IQ Server settings');
  }
}

export async function putIqSettings(config: IqServerConfiguration): Promise<IqServerConfiguration> {
  const response = await Axios.put(IQ_API, config);
  return response.data;
}

/**
 * Disable IQ Server via the dedicated endpoint.
 *
 * POST /v1/iq/disable flips only the enabled flag — the backend re-persists the
 * already-stored ClmConfiguration untouched, so every stored setting survives by
 * construction, including ones the GET does not expose and a reconstructed PUT payload
 * could therefore never echo back. It also performs no request to the remote IQ Server,
 * so disabling works while IQ is unreachable.
 */
export async function disableIqServer(): Promise<void> {
  await Axios.post(`${IQ_API}/disable`);
}
