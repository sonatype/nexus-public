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
/*global NX, Sonatype, Ext*/
NX.define('Sonatype.repoServer.HelpAboutPanel', {
  extend : 'Ext.Panel',
  requireSuper : false,
  requirejs : ['Sonatype/all'],
  constructor : function(config) {
    Ext.apply(this, config || {});

    var helpItems = [
      {
        //add this one by default, so existing usage will be preserved
        text : this.getHelpText()
      }
    ];

    Sonatype.Events.fireEvent('aboutPanelContributions', helpItems);

    var i, helpText = '';

    for (i = 0; i < helpItems.length; i++) {
      helpText += helpItems[i].text;
    }

    Sonatype.repoServer.HelpAboutPanel.superclass.constructor.call(this, {
      layout : 'border',
      autoScroll : false,
      width : '100%',
      height : '100%',
      items : [
        {
          xtype : 'panel',
          region : 'center',
          layout : 'fit',
          html : helpText
        }
      ]
    });
  },

  getHelpText : function() {
    return '<div class="little-padding">'
          + 'Nexus Repository Manager'
          + '<br/>Copyright &copy; 2008-present Sonatype, Inc.'
          + '<br/>All rights reserved. Includes the third-party code listed at <a href="'
          + Sonatype.utils.attributionsURL + '" target="_new">' + Sonatype.utils.attributionsURL + '</a>.'
          + '<br/>'
          + '<br/>This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,'
          + '<br/>which accompanies this distribution and is available at <a href="http://www.eclipse.org/legal/epl-v10.html" target="_new">http://www.eclipse.org/legal/epl-v10.html</a>.'
          + '<br/>'
          + '<br/>Nexus Repository Manager is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks'
          + '<br/>of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the'
          + '<br/>Eclipse Foundation. All other trademarks are the property of their respective owners.'
          + '</div>';
  }

});

