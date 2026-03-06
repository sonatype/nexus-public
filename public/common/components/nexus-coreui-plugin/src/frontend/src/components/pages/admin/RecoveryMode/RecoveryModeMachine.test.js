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
import {interpret} from 'xstate';
import {when} from 'jest-when';
import axios from 'axios';

import {ExtJS, APIConstants} from '@sonatype/nexus-ui-plugin';
import {awaitTransition} from '@sonatype/nexus-ui-plugin/src/frontend/__jest__/xstateTestUtils';

import RecoveryModeMachine from './RecoveryModeMachine';
import UIStrings from '../../../../constants/UIStrings';

const {RECOVERY_MODE} = UIStrings;

const {
  RECOVERY_MODE: RECOVERY_MODE_PUBLIC_API
} = APIConstants.REST.PUBLIC;

const {
  RECOVERY_MODE: RECOVERY_MODE_UI_API
} = APIConstants.REST.INTERNAL;

const mockRecoveryModeEnabled = {
  enabled: true,
  unexecutedPlans: false,
  blockedTaskNames: ['Task A', 'Task B'],
  reconcileTasks: []
};

const mockRecoveryModeEnabledWithUnexecutedPlans = {
  ...mockRecoveryModeEnabled,
  unexecutedPlans: true
};

jest.mock('@sonatype/nexus-ui-plugin', () => {
  return {
    ...jest.requireActual('@sonatype/nexus-ui-plugin'),
    ExtJS: {
      showSuccessMessage: jest.fn(),
      showErrorMessage: jest.fn(),
      state: jest.fn(() => ({
        getValue: jest.fn(),
        setValue: jest.fn()
      }))
    }
  };
});

describe('RecoveryModeMachine', () => {
  beforeEach(() => {
    when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
      data: mockRecoveryModeEnabled
    });
    axios.post.mockResolvedValue();
    axios.delete.mockResolvedValue();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('initial state', () => {
    it('starts in loading state', () => {
      const service = interpret(RecoveryModeMachine);
      service.start();

      expect(service.state.matches('loading')).toBe(true);
      service.stop();
    });

    it('loads data and transitions to loaded state', async () => {
      await awaitTransition(
        RecoveryModeMachine,
        'loaded',
        (state) => {
          expect(state.context.data).toEqual(mockRecoveryModeEnabled);
          expect(axios.get).toHaveBeenCalledWith(RECOVERY_MODE_UI_API);
        }
      );
    });
  });

  describe('DISABLE event', () => {
    it('transitions directly to saving when no unexecuted plans exist', async () => {
      when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
        data: mockRecoveryModeEnabled
      });

      let hasSeenLoaded = false;
      let hasSeenSaving = false;

      await awaitTransition(
        RecoveryModeMachine,
        (state) => {
          if (state.matches('loaded')) {
            hasSeenLoaded = true;
          }
          if (state.matches('setDisabled')) {
            hasSeenSaving = true;
          }
          return state.matches('loaded') && hasSeenLoaded && hasSeenSaving;
        },
        (state) => {
          expect(state.context.data.enabled).toBe(false);
          expect(axios.delete).toHaveBeenCalledWith(RECOVERY_MODE_PUBLIC_API);
        },
        (service) => {
          // Wait for loaded state, then send DISABLE
          const unsubscribe = service.subscribe((state) => {
            if (state.matches('loaded') && !hasSeenSaving) {
              service.send({type: 'DISABLE'});
              unsubscribe.unsubscribe();
            }
          });
        }
      );
    });

    it('transitions to confirmingDisable when unexecuted plans exist', async () => {
      when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
        data: mockRecoveryModeEnabledWithUnexecutedPlans
      });

      let hasSeenLoaded = false;

      await awaitTransition(
        RecoveryModeMachine,
        'confirmingDisable',
        (state) => {
          expect(state.context.data.unexecutedPlans).toBe(true);
        },
        (service) => {
          const unsubscribe = service.subscribe((state) => {
            if (state.matches('loaded') && !hasSeenLoaded) {
              hasSeenLoaded = true;
              service.send({type: 'DISABLE'});
              unsubscribe.unsubscribe();
            }
          });
        }
      );
    });
  });

  describe('confirmingDisable state', () => {
    it('transitions to saving on CONFIRM event', async () => {
      when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
        data: mockRecoveryModeEnabledWithUnexecutedPlans
      });

      let hasSeenConfirmingDisable = false;
      let hasSeenSaving = false;

      await awaitTransition(
        RecoveryModeMachine,
        (state) => {
          if (state.matches('confirmingDisable')) {
            hasSeenConfirmingDisable = true;
          }
          if (state.matches('setDisabled')) {
            hasSeenSaving = true;
          }
          return state.matches('loaded') && hasSeenConfirmingDisable && hasSeenSaving;
        },
        (state) => {
          expect(state.context.data.enabled).toBe(false);
          expect(axios.delete).toHaveBeenCalledWith(RECOVERY_MODE_PUBLIC_API);
        },
        (service) => {
          let hasSeenLoaded = false;
          const unsubscribe = service.subscribe((state) => {
            if (state.matches('loaded') && !hasSeenLoaded) {
              hasSeenLoaded = true;
              service.send({type: 'DISABLE'});
            } else if (state.matches('confirmingDisable') && !hasSeenSaving) {
              service.send({type: 'CONFIRM'});
              unsubscribe.unsubscribe();
            }
          });
        }
      );
    });

    it('transitions back to loaded on CANCEL event', async () => {
      when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockResolvedValue({
        data: mockRecoveryModeEnabledWithUnexecutedPlans
      });

      let hasSeenConfirmingDisable = false;
      let cancelledFromConfirming = false;

      await awaitTransition(
        RecoveryModeMachine,
        (state) => {
          if (state.matches('confirmingDisable')) {
            hasSeenConfirmingDisable = true;
          }
          return state.matches('loaded') && hasSeenConfirmingDisable && cancelledFromConfirming;
        },
        (state) => {
          expect(state.context.data.enabled).toBe(true);
          expect(axios.delete).not.toHaveBeenCalled();
        },
        (service) => {
          let hasSeenLoaded = false;
          const unsubscribe = service.subscribe((state) => {
            if (state.matches('loaded') && !hasSeenLoaded) {
              hasSeenLoaded = true;
              service.send({type: 'DISABLE'});
            } else if (state.matches('confirmingDisable') && !cancelledFromConfirming) {
              cancelledFromConfirming = true;
              service.send({type: 'CANCEL'});
              unsubscribe.unsubscribe();
            }
          });
        }
      );
    });
  });

  describe('ENABLE event', () => {
    it('transitions to saving and enables recovery mode', async () => {
      let hasSeenLoaded = false;
      let hasSeenSaving = false;

      await awaitTransition(
        RecoveryModeMachine,
        (state) => {
          if (state.matches('loaded') && !hasSeenSaving) {
            hasSeenLoaded = true;
          }
          if (state.matches('setEnabled')) {
            hasSeenSaving = true;
          }
          return state.matches('loaded') && hasSeenLoaded && hasSeenSaving;
        },
        (state) => {
          expect(state.context.data.enabled).toBe(true);
          expect(axios.post).toHaveBeenCalledWith(RECOVERY_MODE_PUBLIC_API);
        },
        (service) => {
          const unsubscribe = service.subscribe((state) => {
            if (state.matches('loaded') && !hasSeenSaving) {
              service.send({type: 'ENABLE'});
              unsubscribe.unsubscribe();
            }
          });
        }
      );
    });
  });

  describe('guards', () => {
    it('hasUnexecutedPlans returns true when unexecuted plans exist', () => {
      const service = interpret(RecoveryModeMachine.withContext({
        data: {unexecutedPlans: true},
        error: null
      }));
      service.start();

      const config = RecoveryModeMachine.config;
      const guards = RecoveryModeMachine.options.guards;
      const result = guards.hasUnexecutedPlans(service.state.context);

      expect(result).toBe(true);
      service.stop();
    });

    it('hasUnexecutedPlans returns false when no unexecuted plans exist', () => {
      const service = interpret(RecoveryModeMachine.withContext({
        data: {unexecutedPlans: false},
        error: null
      }));
      service.start();

      const guards = RecoveryModeMachine.options.guards;
      const result = guards.hasUnexecutedPlans(service.state.context);

      expect(result).toBe(false);
      service.stop();
    });
  });

  describe('error handling', () => {
    it('transitions to loadError on failed fetch', async () => {
      when(axios.get).calledWith(RECOVERY_MODE_UI_API).mockRejectedValue({
        message: 'Network error'
      });

      await awaitTransition(
        RecoveryModeMachine,
        'loadError',
        () => {
          expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(
            RECOVERY_MODE.MESSAGES.LOAD_ERROR
          );
        }
      );
    });

    it('shows error message on failed save', async () => {
      axios.delete.mockRejectedValueOnce({
        message: 'Save error'
      });

      let hasSeenSaving = false;
      let hasSeenError = false;

      await awaitTransition(
        RecoveryModeMachine,
        (state) => {
          if (state.matches('setDisabled')) {
            hasSeenSaving = true;
          }
          if (state.matches('loaded') && hasSeenSaving && !hasSeenError) {
            hasSeenError = true;
            return true;
          }
          return false;
        },
        () => {
          expect(ExtJS.showErrorMessage).toHaveBeenCalledWith(
            RECOVERY_MODE.MESSAGES.SAVE_ERROR
          );
        },
        (service) => {
          const unsubscribe = service.subscribe((state) => {
            if (state.matches('loaded') && !hasSeenSaving) {
              service.send({type: 'DISABLE'});
              unsubscribe.unsubscribe();
            }
          });
        }
      );
    });
  });
});
