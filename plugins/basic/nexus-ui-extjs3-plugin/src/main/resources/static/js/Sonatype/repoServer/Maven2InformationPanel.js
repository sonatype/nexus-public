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
NX.define('Sonatype.repoServer.Maven2InformationPanel', {
  extend : 'Ext.form.FormPanel',
  requirejs : ['Sonatype/init'],

  constructor : function(config) {
    Ext.apply(this, config || {}, {
      halfSize : false
    });

    this.sp = Sonatype.lib.Permissions;

    this.linkDivId = Ext.id();
    this.linkLabelId = Ext.id();

    Sonatype.repoServer.Maven2InformationPanel.superclass.constructor.call(this, {
      title : 'Maven',
      autoScroll : true,
      border : true,
      frame : true,
      collapsible : false,
      collapsed : false,
      items : [
        {
          xtype : 'displayfield',
          fieldLabel : 'Group',
          name : 'groupId',
          anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
          allowBlank : true,
          readOnly : true
        },
        {
          xtype : 'displayfield',
          fieldLabel : 'Artifact',
          name : 'artifactId',
          anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
          allowBlank : true,
          readOnly : true
        },
        {
          xtype : 'displayfield',
          fieldLabel : 'Version',
          name : 'baseVersion',
          anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
          allowBlank : true,
          readOnly : true
        },
        {
          xtype : 'displayfield',
          fieldLabel : 'Classifier',
          name : 'classifier',
          anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
          allowBlank : true,
          readOnly : true
        },
        {
          xtype : 'displayfield',
          fieldLabel : 'Extension',
          name : 'extension',
          anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
          allowBlank : true,
          readOnly : true
        },
        {
          xtype : 'textarea',
          fieldLabel : 'XML',
          anchor : Sonatype.view.FIELD_OFFSET,
          height : 120,
          name : 'dependencyXmlChunk',
          allowBlank : true,
          readOnly : true
        }
      ]
    });
  },

  showArtifact : function(data, artifactContainer) {
    this.data = data;
    if (data) {
      Ext.Ajax.request({
        url : this.data.resourceURI + '?describe=maven2&isLocal=true',
        callback : function(options, isSuccess, response) {
          if (isSuccess) {
            var infoResp = Ext.decode(response.responseText);

            // hide classifier if empty
            if (this.data.classifier) {
              this.find('name', 'classifier')[0].show();
            }
            else {
              this.find('name', 'classifier')[0].hide();
            }
            this.form.setValues(infoResp.data);
            artifactContainer.showTab(this);
          }
          else {
            if (response.status === 404) {
              artifactContainer.hideTab(this);
            }
            else {
              Sonatype.utils.connectionError(response, 'Unable to retrieve Maven information.');
            }
          }
        },
        scope : this,
        method : 'GET',
        suppressStatus : 404
      });
    }
    else {
      this.find('name', 'groupId')[0].setRawValue(null);
      this.find('name', 'artifactId')[0].setRawValue(null);
      this.find('name', 'baseVersion')[0].setRawValue(null);
      this.find('name', 'classifier')[0].setRawValue(null);
      this.find('name', 'extension')[0].setRawValue(null);
      this.find('name', 'dependencyXmlChunk')[0].setRawValue(null);
    }
  }
}, function() {

  Sonatype.Events.addListener('fileContainerInit', function(items) {
    items.push(new Sonatype.repoServer.Maven2InformationPanel({
      name : 'maven2InformationPanel',
      tabTitle : 'Maven',
      preferredIndex : 10
    }));
  });

  Sonatype.Events.addListener('fileContainerUpdate', function(artifactContainer, data) {
    var panel = artifactContainer.find('name', 'maven2InformationPanel')[0];

    if (data && data.leaf) {
      panel.showArtifact(data, artifactContainer);
    }
    else {
      panel.showArtifact(null, artifactContainer);
    }
  });

  Sonatype.Events.addListener('artifactContainerInit', function(items) {
    items.push(new Sonatype.repoServer.Maven2InformationPanel({
      name : 'maven2InformationPanel',
      tabTitle : 'Maven',
      preferredIndex : 10
    }));
  });

  Sonatype.Events.addListener('artifactContainerUpdate', function(artifactContainer, payload) {
    var panel = artifactContainer.find('name', 'maven2InformationPanel')[0];

    if (payload && payload.leaf) {
      panel.showArtifact(payload, artifactContainer);
    }
    else {
      panel.showArtifact(null, artifactContainer);
    }

  });

});
