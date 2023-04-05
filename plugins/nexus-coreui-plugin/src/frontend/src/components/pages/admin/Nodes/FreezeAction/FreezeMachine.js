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
import {assign, createMachine} from 'xstate';
import {SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS} from '@sonatype/react-shared-components';
import {APIConstants, ExtAPIUtils, ExtJS} from '@sonatype/nexus-ui-plugin';
import UIStrings from '../../../../../constants/UIStrings';

const {
  NODES: {READ_ONLY}
} = UIStrings;

const {
  EXT: {FREEZE}
} = APIConstants;

export default createMachine(
  {
    id: 'FreezeMachine',
    initial: 'idle',
    states: {
      idle: {
        on: {
          CONFIRM: {
            target: 'confirm'
          }
        }
      },
      toggling: {
        invoke: {
          src: 'toggle',
          onDone: {
            target: 'success',
            actions: ['setFrozen']
          },
          onError: {
            target: 'idle',
            actions: 'onError'
          }
        }
      },
      releasing: {
        invoke: {
          src: 'forceRelease',
          onDone: {
            target: 'success',
            actions: ['setFrozen']
          },
          onError: {
            target: 'idle',
            actions: 'onError'
          }
        }
      },
      confirm: {
        on: {
          TOGGLE: {
            target: 'toggling'
          },
          FORCE_RELEASE: {
            target: 'releasing'
          },
          CANCEL: {
            target: 'idle'
          }
        }
      },
      success: {
        after: {
          [SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS]: 'idle'
        }
      }
    }
  },
  {
    actions: {
      setFrozen: assign({
        frozen: (_, event) => event.data.frozen
      }),
      onError: ({frozen}, event) => {
        const {ERROR} = frozen ? READ_ONLY.DISABLE : READ_ONLY.ENABLE;
        ExtJS.showErrorMessage(ERROR + event.data?.message);
      }
    },
    services: {
      toggle: async ({frozen}) => {
        const response = await ExtAPIUtils.extAPIRequest(FREEZE.ACTION, FREEZE.METHODS.UPDATE, {
          data: [{frozen: !frozen}]
        });
        ExtAPIUtils.checkForError(response);
        return ExtAPIUtils.extractResult(response);
      },
      forceRelease: async () => {
        const response = await ExtAPIUtils.extAPIRequest(
          FREEZE.ACTION,
          FREEZE.METHODS.FORCE_RELEASE
        );
        ExtAPIUtils.checkForError(response);
        return ExtAPIUtils.extractResult(response);
      }
    }
  }
);
