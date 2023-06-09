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
import Axios from 'axios';
import {assign} from 'xstate';
import {mergeDeepRight} from 'ramda';

import {APIConstants, ExtJS, FormUtils, UIStrings} from '@sonatype/nexus-ui-plugin';

const {REST} = APIConstants;

export default FormUtils.buildFormMachine({
  id: 'SupportZipHaMachine',

  config: (config) =>
    mergeDeepRight(config, {
      context: {
        nxrmNodes: [],
        selectedNode: null,
        showCreateZipModal: false,
        targetNode: null
      },

      states: {
        loaded: {
          on: {
            SHOW_SUPPORT_ZIP_FORM_MODAL: {
              actions: 'showCreateZipModalForm'
            },
            HIDE_SUPPORT_ZIP_FORM_MODAL: {
              actions: 'hideCreateZipModalForm'
            },
            CREATE_SUPPORT_ZIP_FOR_NODE: {
              target: 'creatingNodeSupportZip'
            },
            DOWNLOAD_ZIP: {
              target: 'initSupportZipDownload',
              actions: 'setTargetNode'
            }
          }
        },

        downloadingZip: {
          invoke: {
            src: 'downloadZip',
            onDone: {
              target: 'loaded'
            },
            onError: {
              target: 'loaded'
            }
          }
        },

        creatingNodeSupportZip: {
          invoke: {
            src: 'createHaZip',
            onDone: {
              target: 'loaded',
              actions: 'hideCreateZipModalForm'
            },
            onError: {
              target: 'loaded'
            }
          }
        },

        initSupportZipDownload: {
          invoke: {
            src: 'verifyCanZipBeDownloaded',
            onDone: {
              target: 'downloadingZip'
            },
            onError: {
              target: 'loaded',
              actions: 'setCreateError'
            }
          }
        }
      }
    })
}).withConfig({
  actions: {
    setData: assign({
      nxrmNodes: (_, event) => event?.data?.data || []
    }),

    showCreateZipModalForm: assign({
      showCreateZipModal: true,
      selectedNode: (_, event) => event?.data?.selectedNode || null
    }),

    hideCreateZipModalForm: assign({
      showCreateZipModal: false,
      selectedNode: null
    }),

    setCreateError: () => {
      ExtJS.showErrorMessage(UIStrings.ERROR.NOT_FOUND_ERROR('Support zip'));
    },

    setTargetNode: assign({
      targetNode: (_, event) => event?.data?.node
    })
  },

  services: {
    fetchData: () => Axios.get(REST.INTERNAL.GET_SUPPORT_ZIP_ACTIVE_NODES),

    createHaZip: (_, event) => {
      const node = event?.data?.node || null;
      const zipParams = event?.data?.params || null;

      if (node && zipParams) {
        const selectedNodeId = node.nodeId;
        const params = {
          ...zipParams,
          hostname: node.hostname
        };
        return Axios.post(REST.INTERNAL.SUPPORT_ZIP + selectedNodeId, params);
      }
      return Promise.reject();
    },

    verifyCanZipBeDownloaded: async ({targetNode}) => {
      const {data} = await Axios.get(REST.PUBLIC.NODE_ID);
      return targetNode && data.nodeId === targetNode.nodeId ? Promise.resolve() : Promise.reject();
    },

    downloadZip: ({targetNode}) => {
      if (targetNode) {
        const url = ExtJS.urlOf(`service/rest/wonderland/download/${targetNode.blobRef}`);
        try {
          ExtJS.downloadUrl(url);
        } catch(e) {
          console.error('NX.util.DownloadHelper is not available in debug mode.\n', e);
          window.open(url, '_self');
        }
        return Axios.delete(REST.INTERNAL.CLEAR_SUPPORT_ZIP_HISTORY + targetNode.nodeId);
      }
    }
  }
});
