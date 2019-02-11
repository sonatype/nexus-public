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
define('Sonatype/repoServer/ArtifactInformationPanel', function() {

  Ext.form.RepositoryUrlDisplayField = Ext.extend(Ext.form.DisplayField, {
        setValue : function(repositories) {
          if (!repositories) {
            return this;
          }

          if (typeof repositories === 'string') {
            // display value was already set when field was not visible - repositories already
            // contains the raw value.
            this.setRawValue(repositories);
            return this;
          }

          var links = '';

          for (var i = 0; i < repositories.length; i++)
          {
            if (i != 0)
            {
              links += ', ';
            }

            var path = 'index.html#view-repositories;' + encodeURIComponent(repositories[i].repositoryId) 
                        + '~browsestorage~' + encodeURIComponent(repositories[i].path);
            if (repositories[i].canView)
            {
              links += '<a href="' + path + '">' + NX.htmlRenderer(repositories[i].repositoryName) + '</a>';
            }
            else
            {
              links += repositories[i].repositoryName;
            }
          }

          this.setRawValue(links);
          return this;
        }
      });

  Ext.reg('repositoryUrlDisplayField', Ext.form.RepositoryUrlDisplayField);

  Ext.form.RepositoryPathDisplayField = Ext.extend(Ext.form.DisplayField, {
    setValue : function(repositoryPath) {
      if (repositoryPath) {
        if (typeof repositoryPath === 'string'
            || !repositoryPath.path || !repositoryPath.href) {
          this.setRawValue(repositoryPath);
        }
        else {
          this.setRawValue('<a href="' + repositoryPath.href + '" target="_blank">' + NX.htmlRenderer(repositoryPath.path) + '</a>');
        }
      }
      return this;
    }
  });

  Ext.reg('repositoryPathDisplayField', Ext.form.RepositoryPathDisplayField);

  Sonatype.repoServer.ArtifactInformationPanel = function(config) {
    var config = config || {};
    var defaultConfig = {};
    Ext.apply(this, config, defaultConfig);

    this.sp = Sonatype.lib.Permissions;

    this.deleteButton = new Ext.Button({
          xtype : 'button',
          text : 'Delete',
          handler : this.artifactDelete,
          scope : this
        });

    this.downloadButton = new Ext.Button({
          xtype : 'button',
          text : 'Download',
          handler : this.artifactDownload,
          scope : this
        });

    Sonatype.repoServer.ArtifactInformationPanel.superclass.constructor.call(this, {
          title : 'Artifact',
          autoScroll : true,
          border : true,
          frame : true,
          collapsible : false,
          collapsed : false,
          items : [{
                xtype : 'repositoryPathDisplayField',
                fieldLabel : 'Repository Path',
                name : 'repositoryPath',
                anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                allowBlank : true,
                readOnly : true
              }, {
                xtype : 'displayfield',
                fieldLabel : 'Uploaded by',
                name : 'uploader',
                anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                allowBlank : true,
                readOnly : true,
                formatter : function(value) {
                  if (value) {
                    return NX.htmlRenderer(value);
                  }
                }
              }, {
                xtype : 'byteDisplayField',
                fieldLabel : 'Size',
                name : 'size',
                anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                allowBlank : true,
                readOnly : true
              }, {
                xtype : 'timestampDisplayField',
                fieldLabel : 'Uploaded Date',
                name : 'uploaded',
                anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                allowBlank : true,
                readOnly : true,
                formatter : function(value) {
                  if (value)
                  {
                    return new Date.parseDate(value, 'u').format('m.d.Y  h:m:s');
                  }
                }
              }, {
                xtype : 'timestampDisplayField',
                fieldLabel : 'Last Modified',
                name : 'lastChanged',
                anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                allowBlank : true,
                readOnly : true,
                dateFormat : Ext.util.Format.dateRenderer('m/d/Y')
              }, {
                xtype : 'panel',
                layout : 'column',
                buttonAlign : 'left',
                items : [{}],
                buttons : [this.downloadButton, this.deleteButton]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Checksums',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'displayfield',
                      fieldLabel : 'SHA1',
                      name : 'sha1Hash',
                      anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                      allowBlank : true,
                      readOnly : true
                    }, {
                      xtype : 'displayfield',
                      fieldLabel : 'MD5',
                      name : 'md5Hash',
                      anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                      allowBlank : true,
                      readOnly : true
                    }]
              }, {
                xtype : 'fieldset',
                checkboxToggle : false,
                title : 'Contained In Repositories',
                anchor : Sonatype.view.FIELDSET_OFFSET,
                collapsible : false,
                autoHeight : true,
                layoutConfig : {
                  labelSeparator : ''
                },
                items : [{
                      xtype : 'panel',
                      items : [{
                            xtype : 'repositoryUrlDisplayField',
                            name : 'repositories',
                            anchor : Sonatype.view.FIELD_OFFSET_WITH_SCROLL,
                            allowBlank : true,
                            readOnly : true
                          }]
                    }]
              }]
        });
  };

  Ext.extend(Sonatype.repoServer.ArtifactInformationPanel, Ext.form.FormPanel, {

        artifactDownload : function() {
          if (this.data)
          {
            Sonatype.utils.openWindow(this.data.resourceURI);
          }
        },

        artifactDelete : function() {
          if (this.data)
          {
            var url = this.data.resourceURI;

            Sonatype.MessageBox.show({
                  title : 'Delete Repository Item?',
                  msg : 'Delete the selected artifact ?',
                  buttons : Sonatype.MessageBox.YESNO,
                  scope : this,
                  icon : Sonatype.MessageBox.QUESTION,
                  fn : function(btnName) {
                    if (btnName == 'yes' || btnName == 'ok')
                    {
                      Ext.Ajax.request({
                            url : url,
                            callback : this.deleteRepoItemCallback,
                            scope : this,
                            method : 'DELETE'
                          });
                    }
                  }
                });
          }
        },

        deleteRepoItemCallback : function(options, isSuccess, response) {
          if (isSuccess)
          {
            var panel = Sonatype.view.mainTabPanel.getActiveTab();
            var id = panel.getId();
            if (id == 'nexus-search')
            {
              panel.startSearch(panel, false);
            }
            else if (id == 'view-repositories')
            {
              panel.refreshHandler(null, null);
            }
          }
          else
          {
            Sonatype.MessageBox.alert('Error', response.status == 401 ? 'You don\'t have permission to delete artifacts in this repository' : 'The server did not delete the file/folder from the repository');
          }
        },

        setupNonLocalView : function(repositoryPath) {
          if (this.data.resourceURI) {
            this.find('name', 'repositoryPath')[0].setValue({
              path: repositoryPath + ' (Not Locally Cached)',
              href: this.data.resourceURI
            });
          } else {
            this.find('name', 'repositoryPath')[0].setRawValue(repositoryPath + ' (Not Locally Cached)');
          }
          this.find('name', 'uploader')[0].setRawValue(null);
          this.find('name', 'size')[0].setRawValue(null);
          this.find('name', 'uploaded')[0].setRawValue(null);
          this.find('name', 'lastChanged')[0].setRawValue(null);
          this.find('name', 'sha1Hash')[0].setRawValue(null);
          this.find('name', 'md5Hash')[0].setRawValue(null);
          this.find('name', 'repositories')[0].setRawValue(null);

          this.find('name', 'uploader')[0].hide();
          this.find('name', 'size')[0].hide();
          this.find('name', 'uploaded')[0].hide();
          this.find('name', 'lastChanged')[0].hide();
          this.deleteButton.hide();
          var fieldsets = this.findByType('fieldset');

          for (var i = 0; i < fieldsets.length; i++)
          {
            fieldsets[i].hide();
          }
        },

        clearNonLocalView : function(showDeleteButton) {
          this.find('name', 'uploader')[0].show();
          this.find('name', 'size')[0].show();
          this.find('name', 'uploaded')[0].show();
          this.find('name', 'lastChanged')[0].show();
          if (showDeleteButton)
          {
            this.deleteButton.show();
          }
          var fieldsets = this.findByType('fieldset');

          for (var i = 0; i < fieldsets.length; i++)
          {
            fieldsets[i].show();
          }
        },

        showArtifact : function(data, artifactContainer) {
          this.data = data;
          if (data == null)
          {
            this.find('name', 'repositoryPath')[0].setRawValue(null);
            this.find('name', 'uploader')[0].setRawValue(null);
            this.find('name', 'size')[0].setRawValue(null);
            this.find('name', 'uploaded')[0].setRawValue(null);
            this.find('name', 'lastChanged')[0].setRawValue(null);
            this.find('name', 'sha1Hash')[0].setRawValue(null);
            this.find('name', 'md5Hash')[0].setRawValue(null);
            this.find('name', 'repositories')[0].setRawValue(null);
          }
          else
          {
            Ext.Ajax.request({
                  url : this.data.resourceURI + '?describe=info&isLocal=true',
                  callback : function(options, isSuccess, response) {
                    if (isSuccess)
                    {
                      var infoResp = Ext.decode(response.responseText);

                      if (!infoResp.data.presentLocally)
                      {
                        this.setupNonLocalView(infoResp.data.repositoryPath);
                      }
                      else
                      {
                        this.clearNonLocalView(infoResp.data.canDelete);
                        this.form.setValues(Ext.apply(infoResp.data, {
                          repositoryPath: {
                            path: infoResp.data.repositoryPath,
                            href: this.data.resourceURI
                          }
                        }));
                      }
                      artifactContainer.showTab(this);
                    }
                    else
                    {
                      if (response.status = 404)
                      {
                        artifactContainer.hideTab(this);
                      }
                      else
                      {
                        Sonatype.utils.connectionError(response, 'Unable to retrieve artifact information.');
                      }
                    }
                  },
                  scope : this,
                  method : 'GET',
                  suppressStatus : '404'
                });
          }
        }
      });

  Sonatype.Events.addListener('fileContainerInit', function(items) {
        items.push(new Sonatype.repoServer.ArtifactInformationPanel({
              name : 'artifactInformationPanel',
              tabTitle : 'Artifact',
              preferredIndex : 20
            }));
      });

  Sonatype.Events.addListener('fileContainerUpdate', function(artifactContainer, data) {
        var panel = artifactContainer.find('name', 'artifactInformationPanel')[0];

        if (data == null || !data.leaf)
        {
          panel.showArtifact(null, artifactContainer);
        }
        else
        {
          panel.showArtifact(data, artifactContainer);
        }
      });

  Sonatype.Events.addListener('artifactContainerInit', function(items) {
        items.push(new Sonatype.repoServer.ArtifactInformationPanel({
              name : 'artifactInformationPanel',
              tabTitle : 'Artifact',
              preferredIndex : 20
            }));
      });

  Sonatype.Events.addListener('artifactContainerUpdate', function(artifactContainer, payload) {
        var panel = artifactContainer.find('name', 'artifactInformationPanel')[0];

        if (payload == null || !payload.leaf)
        {
          panel.showArtifact(null, artifactContainer);
        }
        else
        {
          panel.showArtifact(payload, artifactContainer);
        }
      });
});