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
import {assign} from 'xstate';
import Axios from 'axios';
import {mergeDeepRight} from 'ramda';

import {
  FormUtils,
  APIConstants,
  ExtAPIUtils,
  ValidationUtils
} from '@sonatype/nexus-ui-plugin';

import {URLS, remoteHostRequestData} from './SslCertificatesHelper';

const {EXT} = APIConstants;

export const SOURCES = {
  REMOTE_HOST: 'remoteHost',
  PEM: 'PEM'
};

const EMPTY_DATA = {
  remoteHostUrl: '',
  pemContent: ''
};

export default FormUtils.buildFormMachine({
  id: 'SslCertificatesAddFormMachine',
  initial: 'loaded',
  config: (config) =>
      mergeDeepRight(config, {
        context: {
          pristineData: EMPTY_DATA,
          data: EMPTY_DATA,
          source: SOURCES.REMOTE_HOST,
        },
        states: {
          loaded: {
            on: {
              SET_SOURCE: {
                actions: 'setSource',
                target: 'loaded',
              },
              LOAD_DETAILS: {
                target: 'loadingDetails',
              },
            }
          },
          loadingDetails: {
            entry: 'clearSaveError',
            invoke: {
              src: 'fetchDetails',
              onDone: {
                target: 'previewDetails',
                actions: ['clearSaveError', 'setLoadDetailsData'],
              },
              onError: {
                target: 'loaded',
                actions: ['setSaveError', 'logSaveError']
              }
            }
          },
          previewDetails: {
            on: {
              ADD_CERTIFICATE: {
                target: 'saving',
                cond: 'canSave'
              },
            },
          },
          saving: {
            invoke: {
              onError: {
                target: 'previewDetails',
              },
            },
          },
        }
      })
}).withConfig({
  actions: {
    validate: assign({
      validationErrors: ({data, source}) => ({
        remoteHostUrl: source === SOURCES.REMOTE_HOST ? ValidationUtils.validateNotBlank(data.remoteHostUrl): null,
        pemContent: source === SOURCES.PEM ? ValidationUtils.validateNotBlank(data.pemContent) : null,
      })
    }),
    setLoadDetailsData: assign({
      data: (_, event) => event.data,
    }),
    setSource: assign(({data}, {value}) => {
      let {remoteHostUrl, pemContent} = data;
      return {
        source: value,
        data: {
          ...data,
          remoteHostUrl: value === SOURCES.REMOTE_HOST ? remoteHostUrl : '',
          pemContent: value === SOURCES.PEM ? pemContent : ''
        }
      };
    }),
  },
  services: {
    fetchDetails: async ({data: {remoteHostUrl, pemContent}}) => {
      let response;
      if (remoteHostUrl) {
        response = await ExtAPIUtils.extAPIRequest(EXT.SSL.ACTION, EXT.SSL.METHODS.RETRIEVE_FROM_HOST, {
          data: remoteHostRequestData(remoteHostUrl)
        });
      } else if (pemContent) {
        response = await ExtAPIUtils.extAPIRequest(EXT.SSL.ACTION, EXT.SSL.METHODS.DETAILS, {
          data: [pemContent]
        });
      } else {
        return {};
      }

      ExtAPIUtils.checkForError(response);
      return ExtAPIUtils.extractResult(response);
    },
    saveData: ({data}) => Axios.post(URLS.createSslCertificatesUrl, data?.pem),
  }
});
