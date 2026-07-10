/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import axios from 'axios';

import UploadDetailsMachine, {
  extractEnforcementPayload,
  parseHostedEnforcementError,
  HOSTED_ENFORCEMENT_ERROR_PREFIX
} from './UploadDetailsMachine.js';

jest.mock('axios', () => ({
  post: jest.fn()
}));

describe('UploadDetailsMachine enforcement helpers', () => {
  describe('extractEnforcementPayload', () => {
    it('returns null for empty responses', () => {
      expect(extractEnforcementPayload(null)).toBeNull();
      expect(extractEnforcementPayload(undefined)).toBeNull();
    });

    it('returns null for non-403/503 status codes', () => {
      const resp = { status: 500, data: { errorCode: 'HOSTED_DEPLOYMENT_BLOCKED' } };
      expect(extractEnforcementPayload(resp)).toBeNull();
    });

    it('returns null when body is not an enforcement error', () => {
      const resp = { status: 403, data: { message: 'insufficient permissions' } };
      expect(extractEnforcementPayload(resp)).toBeNull();
    });

    it('returns null for unrecognised errorCode', () => {
      const resp = { status: 403, data: { errorCode: 'SOMETHING_ELSE' } };
      expect(extractEnforcementPayload(resp)).toBeNull();
    });

    it('returns parsed payload for 403 block', () => {
      // CLM-40150: even if a stray evaluationUrl arrives in the BE response (older
      // server, replay log, etc.), the parser must drop it. The FE banner no longer
      // renders a link, and we don't want consumers reading a field that's no longer
      // part of the contract.
      const resp = {
        status: 403,
        data: {
          errorCode: 'HOSTED_DEPLOYMENT_BLOCKED',
          assetName: 'log4j-core-2.14.1.jar',
          repositoryName: 'maven-releases',
          evaluationUrl: 'https://iq/eval/xyz',
          correlationId: 'corr-1'
        }
      };
      const payload = extractEnforcementPayload(resp);
      expect(payload).toEqual({
        errorCode: 'HOSTED_DEPLOYMENT_BLOCKED',
        assetName: 'log4j-core-2.14.1.jar',
        repositoryName: 'maven-releases',
        reason: null,
        correlationId: 'corr-1'
      });
      expect(payload).not.toHaveProperty('evaluationUrl');
    });

    it('returns parsed payload for 503 unavailable', () => {
      const resp = {
        status: 503,
        data: {
          errorCode: 'HOSTED_ENFORCEMENT_UNAVAILABLE',
          assetName: 'log4j-core-2.14.1.jar',
          repositoryName: 'maven-releases',
          reason: 'IQ timed out',
          correlationId: 'corr-2'
        }
      };
      const payload = extractEnforcementPayload(resp);
      expect(payload.errorCode).toBe('HOSTED_ENFORCEMENT_UNAVAILABLE');
      expect(payload.assetName).toBe('log4j-core-2.14.1.jar');
      expect(payload.repositoryName).toBe('maven-releases');
      expect(payload.reason).toBe('IQ timed out');
      // CLM-40150: evaluationUrl is no longer part of the parsed shape.
      expect(payload).not.toHaveProperty('evaluationUrl');
    });
  });

  describe('saveData service — 200-with-success=false enforcement path', () => {
    let saveData;

    beforeAll(() => {
      saveData = UploadDetailsMachine.options.services.saveData;
    });

    beforeEach(() => {
      axios.post.mockReset();
    });

    const baseArgs = () => ({
      repoSettings: { name: 'maven-releases' },
      data: {},
      disabledFields: {}
    });

    it('forwards a HOSTED_ENFORCEMENT::-prefixed message from a 200/success=false body so setSaveError can route it', async () => {
      // Backend converts HostedEnforcementException into an ErrorPacket with success=false
      // and the tagged JSON in data[0].message — the machine must rethrow that string verbatim
      // so the setSaveError action can detect the prefix and populate hostedEnforcementError.
      // CLM-40150: BE no longer emits evaluationUrl in the tagged JSON. The fixture
      // mirrors the current wire shape so this test would catch a regression where
      // the field reappears.
      const enforcementPayload = {
        errorCode: 'HOSTED_DEPLOYMENT_BLOCKED',
        assetName: 'log4j-core-2.14.1.jar',
        repositoryName: 'maven-releases',
        correlationId: 'corr-200-false'
      };
      const taggedMessage = HOSTED_ENFORCEMENT_ERROR_PREFIX + JSON.stringify(enforcementPayload);
      axios.post.mockResolvedValue({
        data: {
          success: false,
          0: { message: taggedMessage }
        }
      });

      await expect(saveData(baseArgs())).rejects.toThrow(taggedMessage);

      try {
        await saveData(baseArgs());
      }
      catch (err) {
        // Round-trip: the message thrown here is what setSaveError reads from event.data.message.
        // parseHostedEnforcementError must recover the original payload.
        expect(parseHostedEnforcementError(err.message)).toEqual(enforcementPayload);
      }
    });

    it('falls back to "Unknown Error" when 200/success=false body has no message', async () => {
      axios.post.mockResolvedValue({ data: { success: false } });

      await expect(saveData(baseArgs())).rejects.toThrow('Unknown Error');
    });
  });

  describe('parseHostedEnforcementError', () => {
    it('returns null for non-enforcement error strings', () => {
      expect(parseHostedEnforcementError(null)).toBeNull();
      expect(parseHostedEnforcementError(undefined)).toBeNull();
      expect(parseHostedEnforcementError('Some random error')).toBeNull();
      expect(parseHostedEnforcementError(42)).toBeNull();
    });

    it('returns the parsed payload when prefixed', () => {
      const payload = { errorCode: 'HOSTED_DEPLOYMENT_BLOCKED', correlationId: 'abc' };
      const message = HOSTED_ENFORCEMENT_ERROR_PREFIX + JSON.stringify(payload);
      expect(parseHostedEnforcementError(message)).toEqual(payload);
    });

    it('returns null for prefixed-but-invalid JSON', () => {
      const message = HOSTED_ENFORCEMENT_ERROR_PREFIX + '{not-json';
      expect(parseHostedEnforcementError(message)).toBeNull();
    });
  });
});
