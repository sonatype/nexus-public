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
/**
 * A {@link NX.util.condition.Condition} that is satisfied if the NXRM instance does not have Firewall.
 *
 * @since 3.0
 */
Ext.define('NX.util.condition.HasNoFirewall', {
  extend: 'NX.util.condition.Condition',
  requires: [
    'NX.State'
  ],
  api: {
    load: 'NX.direct.healthcheck_Status.hasFirewall',
  },

  bind: function() {
    var me = this,
        controller;
    if (!me.bounded) {
      // Listen for 'clm' state changes to re-evaluate the condition
      controller = NX.getApplication().getController('State');
      me.mon(controller, {
        'clmchanged': me.evaluate,
        scope: me
      });
      me.callParent();
      me.evaluate();
    }
    return me;
  },

  evaluate: function() {
    var me = this;
    if (me.bounded) {
      var clmState = NX.State.getValue("clm", {'enabled': false, 'hasFirewall': false});
      var enabled = clmState['enabled'];
      var hasFirewall = clmState['hasFirewall'];

      // Show Health Check (satisfied) when: IQ is disabled OR IQ doesn't have Firewall
      // Hide Health Check (not satisfied) when: IQ is enabled AND has Firewall
      me.setSatisfied(!(enabled && hasFirewall));
    }
  }
});
