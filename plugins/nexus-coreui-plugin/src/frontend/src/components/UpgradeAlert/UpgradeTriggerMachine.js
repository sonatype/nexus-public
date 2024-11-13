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

import {ExtJS} from "@sonatype/nexus-ui-plugin";
import axios from "axios";
import {createMachine} from "xstate";

const UpgradeTriggerMachine = createMachine({
    id: 'UpgradeTriggerMachine',
    initial: 'loaded',
    states: {
      loaded: {
        on: {
          SAVE_MODAL: {
            target: 'triggerUpdate'
          }
        }
      },
      triggerUpdate: {
        invoke: {
          src: 'triggerUpdate',
          onDone: {
            target: 'loaded',
            actions: 'closeModal'
          },
        }
      }
    },
  },
  {
    actions: {
      closeModal: () => ExtJS.state().setValue('dbUpgrade', {currentState: 'nexusUpgradeInProgress'})
    },
    services: {
      triggerUpdate: () => axios.post('service/rest/v1/clustered/upgrade-database-schema')
    }
  });

export default UpgradeTriggerMachine;
