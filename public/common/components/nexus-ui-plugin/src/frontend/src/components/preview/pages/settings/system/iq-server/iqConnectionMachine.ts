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
import { ExtJS } from '../../../../../../interface/ExtJS';
import { IqServerConfiguration, IqVerificationResult } from './types';

const IQ_VERIFY_SELFHOSTED = 'service/rest/internal/ui/iq/verify-connection';
const IQ_VERIFY_CLOUD = 'service/rest/v1/iq/test-new-connection';

function getVerifyApi(): string {
  const isCloud = ExtJS.state?.()?.getValue?.('isCloud', false) ?? false;
  return isCloud ? IQ_VERIFY_CLOUD : IQ_VERIFY_SELFHOSTED;
}

export async function verifyConnection(settings: IqServerConfiguration): Promise<IqVerificationResult> {
  try {
    const response = await Axios.post(getVerifyApi(), settings);
    return { success: true, reason: response.data?.reason };
  } catch (err: any) {
    const raw = err?.response?.data || err?.message || 'Connection verification failed';
    const reason = typeof raw === 'string' ? raw : raw?.message || JSON.stringify(raw);
    return { success: false, reason };
  }
}
