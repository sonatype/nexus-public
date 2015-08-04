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
/*global NX, Ext, Sonatype*/
NX.define('Sonatype.repoServer.Documentation', {
  extend : 'Ext.Panel',
  requirejs : ['extjs', 'sonatype'],

  constructor : function(cfg) {
    Ext.apply(this, cfg || {});

    Sonatype.repoServer.Documentation.superclass.constructor.call(this,
          {
            layout : 'border',
            autoScroll : false,
            width : '100%',
            height : '100%',
            items : [
              {
                xtype : 'panel',
                region : 'center',
                layout : 'fit',
                html : this.getHelpText()
              }
            ]
          });
  },

    getHelpText : function() {
      return '<div class="little-padding">'
            + '<h1>Sonatype Nexus&trade; '
            + Sonatype.utils.edition
            + '</h1>'
            + '<br><a href="http://nexus.sonatype.org/" target="_new">Nexus OSS Website</a>'
            + '<br><a href=" http://links.sonatype.com/products/nexus/pro/home " target="_new">Nexus Pro Website</a>'
            + '<br><a href="http://links.sonatype.com/products/nexus/pro/trial" target="_new">Nexus Trial - including guide and example projects</a>'
            + '<br><a href="http://links.sonatype.com/products/nexus/pro/kb" target="_new">Knowlege Base</a>'
            + '<br><a href="http://links.sonatype.com/products/nexus/pro/training" target="_new">Training</a>'
            + '<br><a href="http://links.sonatype.com/products/nexus/oss/docs" target="_new">Repository Management With Nexus - free comprehensive book</a>'
            + '<br><a href="http://links.sonatype.com/products/maven/docs" target="_new">Maven: The Complete Reference and other books</a>'
            + '<br><a href="http://links.sonatype.com/products/nexus/oss/release-notes" target="_new">Release Notes</a>'
            + '</div>';
    }

});

