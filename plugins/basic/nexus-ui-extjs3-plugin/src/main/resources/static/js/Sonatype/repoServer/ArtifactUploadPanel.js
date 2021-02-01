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
/*global NX, Sonatype, Ext*/
//define('repoServer/FileUploadPanel', ['extjs', 'Sonatype/all'], function(Ext, Sonatype) {
NX.define('Sonatype.repoServer.ArtifactUploadPanel', {
  extend : 'Ext.FormPanel',
  requirejs : ['Sonatype/all'],
  mixins : ['Nexus.LogAwareMixin'],

  constructor : function(cfg) {
    Ext.apply(this, cfg || {});

    var
          ht = Sonatype.repoServer.resources.help.artifact,
          packagingStore = new Ext.data.SimpleStore({
            fields : ['value'],
            data : [
              ['pom'],
              ['jar'],
              ['ejb'],
              ['war'],
              ['ear'],
              ['rar'],
              ['par'],
              ['maven-archetype'],
              ['maven-plugin']
            ]
          });

    this.fileInput = null;
    this.pomInput = null;

    this.uploadModeTitle = this.uploadModeTitle || 'Select GAV Definition Source';
    this.uploadModeLabel = this.uploadModeLabel || 'GAV Definition';
    this.uploadModeStore = this.uploadModeStore || new Ext.data.SimpleStore({
      fields : ['value', 'display'],
      data : [
        ['pom', 'From POM'],
        ['gav', 'GAV Parameters']
      ]
    });
    this.uploadModeHt = this.uploadModeHt || ht.gavDefinition;
    this.uploadModeExplanation = this.uploadModeExplanation ||
          'Select a source for the GAV definition. GAV can be specified either manually or from a POM file. These settings will be applied to all artifacts specified below.';

    this.extraItems = this.extraItems || {};

    Sonatype.repoServer.ArtifactUploadPanel.superclass.constructor.call(this, {
      region : 'center',
      id : this.id || 'uploadFormId',
      trackResetOnLoad : true,
      autoScroll : true,
      bodyStyle : 'overflow:auto;',
      border : false,
      frame : true,
      collapsible : false,
      collapsed : false,
      width : '100%',
      fileUpload : true,
      layoutConfig : {
        labelSeparator : ''
      },
      items : [
        {
          xtype : 'hidden',
          name : 'r',
          value : this.payload.id
        },
        {
          xtype : 'fieldset',
          checkboxToggle : false,
          title : this.uploadModeTitle,
          collapsible : false,
          autoHeight : true,
          width : '95%',
          items : [
            {
              xtype : 'combo',
              lazyInit : false,
              fieldLabel : this.uploadModeLabel,
              itemCls : 'required-field',
              helpText : this.uploadModeHt,
              name : 'gavDefinition',
              store : this.uploadModeStore,
              valueField : 'value',
              displayField : 'display',
              editable : false,
              forceSelection : true,
              mode : 'local',
              triggerAction : 'all',
              selectOnFocus : true,
              emptyText : 'Select...',
              allowBlank : false,
              listeners : {
                select : {
                  fn : this.gavDefinitionSelectHandler,
                  scope : this
                }
              }
            },
            {
              xtype : 'panel',
              id : 'gav-definition-card-panel',
              header : false,
              layout : 'card',
              region : 'center',
              activeItem : 0,
              deferredRender : false,
              autoScroll : false,
              frame : false,
              items : [
                {
                  xtype : 'fieldset',
                  checkboxToggle : false,
                  border : false,
                  anchor : Sonatype.view.FIELDSET_OFFSET,
                  collapsible : false,
                  autoHeight : true,
                  layoutConfig : {
                    labelSeparator : ''
                  },
                  items : [
                    {
                      xtype : 'label',
                      html : this.uploadModeExplanation
                    }
                  ]
                },
                {
                  xtype : 'fieldset',
                  border : false,
                  checkboxToggle : false,
                  collapsible : false,
                  autoHeight : true,
                  style : 'margin-right:7px;',
                  items : [
                    {
                      hideLabel : true,
                      name : 'uploadPomButton',
                      xtype : 'browsebutton',
                      text : 'Select POM to Upload...',
                      style : 'margin-bottom: 5px;',
                      uploadPanel : this,
                      handler : function(b) {
                        b.uploadPanel.pomInput = b.detachInputFile();
                        var filename = b.uploadPanel.pomInput.getValue();
                        b.uploadPanel.updatePomFilename(b.uploadPanel, filename);
                      }
                    },
                    {
                      xtype : 'textfield',
                      fieldLabel : 'POM Filename',
                      name : 'pomnameField',
                      anchor : Sonatype.view.FIELD_OFFSET,
                      readOnly : true,
                      allowBlank : false,
                      itemCls : 'required-field',
                      disabled : true,
                      regex : /^(.*(\/|\\)pom.xml)|(.*\.pom)|(pom.xml)$/i,
                      regexText : 'POM file name must be pom.xml or file extension must be .pom'
                    },
                    {
                      xtype : 'label',
                      text : 'Note that if you only intend to upload a POM file, there is no need to add additional artifacts below, simply click the Upload Artifact(s) button.'
                    }
                  ]
                },
                {
                  xtype : 'fieldset',
                  border : false,
                  checkboxToggle : false,
                  collapsible : false,
                  style : 'margin-right:7px;',
                  autoHeight : true,
                  items : [
                    {
                      xtype : 'checkbox',
                      fieldLabel : 'Auto Guess',
                      checked : true,
                      name : 'autoguess',
                      helpText : ht.autoguess,
                      listeners : {
                        'check' : {
                          fn : function(checkbox, value) {
                            this.updateFilename(this);
                          },
                          scope : this
                        }
                      },
                      disabled : true
                    },
                    {
                      xtype : 'textfield',
                      fieldLabel : 'Group',
                      itemCls : 'required-field',
                      helpText : ht.groupId,
                      anchor : Sonatype.view.FIELD_OFFSET,
                      name : 'g',
                      allowBlank : false,
                      disabled : true,
                      validator : function(v) {
                        if (!/^[\w\.\-]+$/.test(v)) {
                          return 'Group ID is illegal, only letters, numbers, underscore(_), hyphon(-), and dot(.) are allowed.';
                        }
                        return true;
                      }
                    },
                    {
                      xtype : 'textfield',
                      fieldLabel : 'Artifact',
                      itemCls : 'required-field',
                      helpText : ht.artifactId,
                      anchor : Sonatype.view.FIELD_OFFSET,
                      name : 'a',
                      allowBlank : false,
                      disabled : true,
                      validator : function(v) {
                        if (!/^[\w\.\-]+$/.test(v)) {
                          return 'Artifact ID is illegal, only letters, numbers, underscore(_), hyphon(-), and dot(.) are allowed.';
                        }
                        return true;
                      }
                    },
                    {
                      xtype : 'textfield',
                      fieldLabel : 'Version',
                      itemCls : 'required-field',
                      helpText : ht.version,
                      anchor : Sonatype.view.FIELD_OFFSET,
                      name : 'v',
                      allowBlank : false,
                      uploadPanel : this,
                      validator : function(v) {
                        var
                              isSnapshotVersion = /-SNAPSHOT$/.test(v) || /LATEST$/.test(v)
                                    || /^(.*)-([0-9]{8}.[0-9]{6})-([0-9]+)$/.test(v),
                              isSnapshotRepo = this.uploadPanel.payload.data.repoPolicy === 'snapshot';

                        if (isSnapshotRepo) {
                          if (!isSnapshotVersion) {
                            return 'You cannot upload a release version into a snapshot repository';
                          }
                        } else {
                          if (isSnapshotVersion) {
                            return 'You cannot upload a snapshot version into a release repository';
                          }
                        }
                        if (!/^[\w\.\-]+$/.test(v)) {
                          return 'Version is illegal, only letters, numbers, underscore(_), hyphon(-), and dot(.) are allowed.';
                        }
                        return true;
                      },
                      disabled : true
                    },
                    {
                      xtype : 'combo',
                      fieldLabel : 'Packaging',
                      itemCls : 'required-field',
                      helpText : ht.packaging,
                      store : packagingStore,
                      displayField : 'value',
                      editable : true,
                      forceSelection : false,
                      mode : 'local',
                      triggerAction : 'all',
                      emptyText : 'Select...',
                      selectOnFocus : true,
                      allowBlank : false,
                      name : 'p',
                      width : 150,
                      listWidth : 150,
                      disabled : true
                    }
                  ]
                }
              ]
            }
          ]
        },

        {
          xtype : 'fieldset',
          id : 'artifacts-upload-fieldset',
          checkboxToggle : false,
          title : 'Select Artifact(s) for Upload',
          collapsible : false,
          autoHeight : true,
          style : 'margin-right:7px;width:95%;',
          items : [
            {
              hideLabel : true,
              xtype : 'browsebutton',
              text : 'Select Artifact(s) to Upload...',
              style : 'margin-bottom: 5px;',
              uploadPanel : this,
              name : 'uploadArtifactButton',
              handler : function(b) {
                b.uploadPanel.fileInput = b.detachInputFile();
                var
                      filename = b.uploadPanel.fileInput.getValue(),
                      endStr = '.pom';
                if (filename === "pom.xml" || (filename.indexOf(endStr) === filename.length - endStr.length )) {
                  Ext.Msg.show({
                    title : 'Error',
                    msg : "Select the POM file as part of the GAV Definition.",
                    buttons : Ext.Msg.OK,
                    icon : Ext.MessageBox.ERROR
                  });
                  return;
                }
                b.uploadPanel.updateFilename(b.uploadPanel, filename);
              }
            },
            {
              xtype : 'textfield',
              fieldLabel : 'Filename',
              name : 'filenameField',
              readOnly : true,
              width : '95%',
              allowBlank : true
            },
            {
              xtype : 'textfield',
              fieldLabel : 'Classifier',
              helpText : ht.classifier,
              name : 'classifier',
              width : '95%',
              allowBlank : true
            },
            {
              xtype : 'textfield',
              fieldLabel : 'Extension',
              helpText : ht.extension,
              name : 'extension',
              width : '95%',
              allowBlank : true
            },
            {
              xtype : 'button',
              id : 'add-button',
              text : 'Add Artifact',
              handler : this.addArtifact,
              scope : this,
              disabled : true
            },
            {
              xtype : 'panel',
              layout : 'column',
              autoHeight : true,
              style : 'padding-top: 5px; padding-bottom: 5px;',
              items : [
                {
                  xtype : 'treepanel',
                  name : 'artifact-list',
                  title : 'Artifacts',
                  border : true,
                  bodyBorder : true,
                  bodyStyle : 'background-color:#FFFFFF; border: 1px solid #B5B8C8',
                  style : 'padding: 0px 10px 0px 105px',
                  width : 750,
                  height : 100,
                  animate : true,
                  lines : false,
                  autoScroll : true,
                  containerScroll : true,
                  rootVisible : false,
                  ddScroll : true,
                  enableDD : true,
                  root : new Ext.tree.TreeNode({
                    text : 'root',
                    draggable : false
                  }),
                  invalidText : 'Add one or more artifacts',
                  validate : function() {
                    var pomMode = this.find('name', 'gavDefinition')[0].getValue() === 'pom';

                    if (pomMode) {
                      return true;
                    }

                    return this.find('name', 'artifact-list')[0].root.childNodes.length > 0;
                  },
                  invalid : false,
                  listeners : {
                    'append' : {
                      fn : function(tree, parentNode, insertedNode, i) {
                        if (tree.invalid) {
                          this.clearInvalid();
                        }
                      },
                      scope : this
                    },
                    'remove' : {
                      fn : function(tree, parentNode, removedNode) {
                        if (tree.root.childNodes.length < 1 && tree.required) {
                          this.markTreeInvalid(null);
                        }
                        else {
                          this.clearInvalid();
                        }
                      },
                      scope : this
                    }
                  }
                },
                {
                  xtype : 'panel',
                  width : 120,
                  items : [
                    {
                      xtype : 'button',
                      text : 'Remove',
                      minWidth : 100,
                      id : 'button-remove',
                      handler : this.removeArtifact,
                      scope : this
                    },
                    {
                      xtype : 'button',
                      text : 'Remove All',
                      style : 'margin-top: 5px',
                      minWidth : 100,
                      id : 'button-remove-all',
                      handler : this.removeAllArtifacts,
                      scope : this
                    }
                  ]
                }
              ]
            },
            this.extraItems,
            {
              xtype : 'panel',
              id : 'end-button-card-panel',
              header : false,
              deferredRender : false,
              autoScroll : false,
              layout : 'fit',
              buttonAlign : 'center',
              frame : false,
              items : [
                {}
              ],
              buttons : [
                {
                  xtype : 'button',
                  id : 'upload-button',
                  text : 'Upload Artifact(s)',
                  handler : this.uploadArtifacts,
                  scope : this
                },
                {
                  xtype : 'button',
                  id : 'reset-all-button',
                  text : 'Reset',
                  handler : this.resetFields,
                  scope : this
                }
              ]
            }
          ]
        }
      ]
    });
  },

  clearInvalid : function() {
    var tree = this.find('name', 'artifact-list')[0];
    if (tree.invalid) {
      // remove error messaging
      tree.getEl().child('.x-panel-body').setStyle({
        'background-color' : '#FFFFFF',
        border : '1px solid #B5B8C8'
      });
      Ext.form.Field.msgFx.normal.hide(tree.errorEl, tree);
    }
  },
  markTreeInvalid : function(errortext) {
    var
          tree = this.find('name', 'artifact-list')[0],
          elp = tree.getEl();

    if (!tree.errorEl) {
      tree.errorEl = elp.createChild({
        cls : 'x-form-invalid-msg'
      });
      tree.errorEl.setWidth(elp.getWidth(true)); // note removed -20 like
      // on form fields
    }
    tree.invalid = true;
    var oldErrorText = tree.invalidText;
    if (errortext) {
      tree.invalidText = errortext;
    }
    tree.errorEl.update(tree.invalidText);
    tree.invalidText = oldErrorText;
    elp.child('.x-panel-body').setStyle({
      'background-color' : '#fee',
      border : '1px solid #dd7870'
    });
    Ext.form.Field.msgFx.normal.show(tree.errorEl, tree);
  },
  resetFields : function() {
    this.gavResponse = null;
    // reset the artifact panels
    var
          filenameField = this.find('name', 'filenameField')[0],
          classifierField = this.find('name', 'classifier')[0],
          extensionField = this.find('name', 'extension')[0],
          addArtifactBtn = this.find('id', 'add-button')[0],
          g = this.find('name', 'g')[0],
          a = this.find('name', 'a')[0],
          v = this.find('name', 'v')[0],
          autoGuess = this.find('name', 'autoguess')[0],
          pomField = this.find('name', 'pomnameField')[0],
          desc = this.find('name', 'description')[0];

    filenameField.reset();
    classifierField.reset();
    extensionField.reset();
    addArtifactBtn.setDisabled(true);

    // clear the artifacts fields
    this.removeAllArtifacts();

    // reset the gav panel
    g.reset();
    a.reset();
    v.reset();
    autoGuess.reset();

    // the pom panel
    pomField.reset();
    this.pomInput = null;
    this.fileInput = null;

    if (desc) {
      desc.reset();
    }
  },

  artifactWithClassifierAndExtensionExists : function(classifier, extension) {
    var
          i, currClassifier, currExtension,
          treePanel = this.find('name', 'artifact-list')[0];
    for (i = 0; i < treePanel.root.childNodes.length; i++) {
      currClassifier = treePanel.root.childNodes[i].attributes.payload.classifier;
      currExtension = treePanel.root.childNodes[i].attributes.payload.extension;

      if (classifier === currClassifier && extension === currExtension) {
        return true;
      }
    }
    return false;
  },

  addArtifact : function() {
    var
          treePanel = this.find('name', 'artifact-list')[0],
          filenameField = this.find('name', 'filenameField')[0],
          classifierField = this.find('name', 'classifier')[0],
          extensionField = this.find('name', 'extension')[0],
          classifier = classifierField.getValue(),
          extension = extensionField.getValue(),
          nodeText = Ext.util.Format.htmlEncode(filenameField.getValue());

    if (this.artifactWithClassifierAndExtensionExists(classifier, extension)) {
      Ext.Msg.show({
        title : 'Classifier and Extension Taken',
        msg : "Every artifact must have a unique classifier and extension. The specified classifier and extension is already taken.",
        buttons : Ext.Msg.OK,
        icon : Ext.MessageBox.WARNING
      });
      return;
    }
    if (!Ext.isEmpty(classifier)) {
      nodeText += ' c:' + classifier;
    }
    if (!Ext.isEmpty(extension)) {
      nodeText += ' e:' + extension;
    }

    if (this.fileInput) {
      treePanel.root.appendChild(new Ext.tree.TreeNode({
        id : filenameField.getValue(),
        text : nodeText,
        payload : {
          id : filenameField.getValue(),
          filename : filenameField.getValue(),
          fileInput : this.fileInput,
          classifier : classifier,
          extension : extension
        },
        allowChildren : false,
        draggable : false,
        leaf : true,
        icon : Sonatype.config.extPath + '/resources/images/default/tree/leaf.gif'
      }));
    }
    filenameField.setValue('');
    classifierField.setValue('');
    extensionField.setValue('');
    this.fileInput = null;
    this.find('id', 'add-button')[0].setDisabled(true);
  },
  removeArtifact : function() {
    var treePanel = this.find('name', 'artifact-list')[0];

    var selectedNode = treePanel.getSelectionModel().getSelectedNode();
    if (selectedNode) {
      treePanel.root.removeChild(selectedNode);
    }
  },
  removeAllArtifacts : function() {
    var
          treePanel = this.find('name', 'artifact-list')[0],
          treeRoot = treePanel.root;

    while (treeRoot.lastChild) {
      treeRoot.removeChild(treeRoot.lastChild);
    }
  },
  updateFilename : function(uploadPanel, filename) {
    var
          filenameField = uploadPanel.find('name', 'filenameField')[0],
          g = '', a = '', v = '', c = '', p = '', e = '';

    if (filename) {
      filenameField.setValue(filename);
    }
    else {
      filename = filenameField.getValue();
      if (!filename) {
        return;
      }
    }

    var cardPanel = uploadPanel.find('id', 'gav-definition-card-panel')[0];

    // match extension to guess the packaging
    var extensionIndex = filename.lastIndexOf('.');
    if (extensionIndex > 0) {
      p = filename.substring(extensionIndex + 1);
      e = filename.substring(extensionIndex + 1);
      filename = filename.substring(0, extensionIndex);

      if (e === 'asc') {
        var primaryExtensionIndex = filename.substring(0, extensionIndex).lastIndexOf('.');
        var primaryExtension = '';
        if (primaryExtensionIndex >= 0) {
          primaryExtension = filename.substring(primaryExtensionIndex + 1);
        }

        if (/^[a-z]*$/.test(primaryExtension)) {
          e = primaryExtension + '.' + e;
          filename = filename.substring(0, primaryExtensionIndex);
        }
      }
    }

    // match the path to guess the group
    if (filename.indexOf('\\') >= 0) {
      filename = filename.replace(/\\/g, '\/');
    }
    var slashIndex = filename.lastIndexOf('/');
    if (slashIndex) {
      g = filename.substring(0, slashIndex);

      filename = filename.substring(slashIndex + 1);
    }

    // separate the artifact name and version
    var versionIndex = filename.search(/\-[\d]/);
    if (versionIndex === -1) {
      versionIndex = filename.search(/-LATEST-/i);
      if (versionIndex === -1) {
        versionIndex = filename.search(/-CURRENT-/i);
      }
    }
    if (versionIndex >= 0) {
      a = filename.substring(0, versionIndex).toLowerCase();

      // guess the version
      filename = filename.substring(versionIndex + 1);
      var classifierIndex = filename.lastIndexOf('-');
      if (classifierIndex >= 0) {
        var classifier = filename.substring(classifierIndex + 1);
        if (classifier && !(/^SNAPSHOT$/i.test(classifier) || /^\d/.test(classifier)
              || /^LATEST$/i.test(classifier)
              || /^CURRENT$/i.test(classifier))) {
          c = classifier;
          filename = filename.substring(0, classifierIndex);
          // dont guess packaging when there is a classifier
          p = '';
          extensionIndex = c.indexOf('.');
          if (extensionIndex >= 0) {
            e = c.substring(extensionIndex + 1) + '.' + e;
            c = c.substring(0, extensionIndex);
          }
        }
      }
      v = filename;

      if (g) {
        // if group ends with version and artifact name, strip those parts
        // (useful if uploading from a local maven repo)
        var i = g.search(new RegExp('\/' + v + '$'));
        if (i > -1) {
          g = g.substring(0, i);
        }
        i = g.search(new RegExp('\/' + a + '$'));
        if (i > -1) {
          g = g.substring(0, i);
        }

        // strip extra path parts, leave only com.* or org.* or net.* or the
        // last element
        i = g.lastIndexOf('/com/');
        if (i === -1) {
          i = g.lastIndexOf('/org/');
          if (i === -1) {
            i = g.lastIndexOf('/net/');
            if (i === -1) {
              i = g.lastIndexOf('/');
            }
          }
        }
        g = g.substring(i + 1).replace(/\//g, '.').toLowerCase();
      }
    }
    else {
      g = '';
    }

    if (cardPanel.find('name', 'autoguess')[0].checked) {
      var groupField = cardPanel.find('name', 'g')[0];
      if (Ext.isEmpty(groupField.getValue())) {
        groupField.setRawValue(g);
      }
      var artifactField = cardPanel.find('name', 'a')[0];
      if (Ext.isEmpty(artifactField.getValue())) {
        artifactField.setRawValue(a);
      }
      var versionField = cardPanel.find('name', 'v')[0];
      if (Ext.isEmpty(versionField.getValue())) {
        versionField.setRawValue(v);
      }
      var packagingField = cardPanel.find('name', 'p')[0];
      if (Ext.isEmpty(packagingField.getValue())) {
        packagingField.setRawValue(p);
      }
    }
    uploadPanel.find('name', 'classifier')[0].setRawValue(c);
    uploadPanel.find('name', 'extension')[0].setRawValue(e);
    if (!a) {
      uploadPanel.form.clearInvalid();
    }
    uploadPanel.find('id', 'add-button')[0].setDisabled(false);
  },
  updatePomFilename : function(uploadPanel, filename) {
    var filenameField = uploadPanel.find('name', 'pomnameField')[0];
    if (filename) {
      filenameField.setValue(filename);
    }
  },
  gavDefinitionSelectHandler : function(combo, record, index) {
    var gavDefinitionPanel = this.findById('gav-definition-card-panel');
    // First disable all the items currently on screen, so they wont be
    // validated/submitted etc
    gavDefinitionPanel.getLayout().activeItem.items.each(function(item) {
      if (item.xtype !== 'browsebutton') {
        item.disable();
      }
    });
    this.clearInvalid();
    // Then find the proper card to activate (based upon the selected
    // schedule type)
    if (record.data.value === 'pom') {
      gavDefinitionPanel.getLayout().setActiveItem(gavDefinitionPanel.items.itemAt(1));
    }
    else if (record.data.value === 'gav') {
      gavDefinitionPanel.getLayout().setActiveItem(gavDefinitionPanel.items.itemAt(2));
    }
    else {
      gavDefinitionPanel.getLayout().setActiveItem(gavDefinitionPanel.items.itemAt(0));
    }
    // Then enable the fields in that card
    gavDefinitionPanel.getLayout().activeItem.items.each(function(item) {
      if (item.xtype !== 'browsebutton') {
        item.enable();
        if (item.readOnly) {
          item.getEl().dom.readOnly = true;
        }
      }
      else {
        item.setClipSize();
      }
    });
    this.doLayout();
  },
  uploadArtifacts : function() {
    var tree = this.find('name', 'artifact-list')[0];
    if (!tree.validate.call(this)) {
      this.markTreeInvalid(null);
    }
    else if (this.form.isValid()) {
      this.doUpload();
    }
  },
  doUpload : function() {
    var treePanel = this.find('name', 'artifact-list')[0];
    var hasArtifacts = treePanel.root.childNodes && treePanel.root.childNodes.length > 0;
    if (!this.pomInput && !hasArtifacts) {
      Sonatype.MessageBox.show({
        title : 'No Artifacts Selected',
        msg : 'The Artifacts list must contain at least one artifact to upload (or a POM file must be selected).',
        buttons : Sonatype.MessageBox.OK,
        icon : Sonatype.MessageBox.ERROR
      });
      return;
    }

    Sonatype.MessageBox.wait('Uploading ...');

    if (hasArtifacts) {
      // FIXME this.currentChildNode is always 0 here? side effect?
      this.currentChildNode = 0;
      this.createUploadForm(treePanel,
            treePanel.root.childNodes[this.currentChildNode].attributes.payload.fileInput,
            treePanel.root.childNodes[this.currentChildNode].attributes.payload.classifier,
            treePanel.root.childNodes[this.currentChildNode].attributes.payload.extension,
            this.currentChildNode === (treePanel.root.childNodes.length - 1));
    }
    else if (this.pomInput) {
      this.createUploadForm(treePanel, null, null, null, true);
    }
  },
  createUploadForm : function(treePanel, fileInput, classifier, extension, lastItem) {
    var repoId = this.payload.id;
    repoId = repoId.substring(repoId.lastIndexOf('/') + 1);
    var pomMode = this.find('name', 'gavDefinition')[0].getValue() === 'pom';

    if (this.gavResponse) {
      pomMode = false;
      this.form.findField('g').setValue(this.gavResponse.groupId);
      this.form.findField('a').setValue(this.gavResponse.artifactId);
      this.form.findField('v').setValue(this.gavResponse.version);
      this.form.findField('p').setValue(this.gavResponse.packaging);
    }

    var repoTag = {
      tag : 'input',
      type : 'hidden',
      name : 'r',
      value : repoId
    };

    var tmpForm = Ext.getBody().createChild({
      tag : 'form',
      cls : 'x-hidden',
      id : Ext.id(),
      children : pomMode ? [repoTag, {
        tag : 'input',
        type : 'hidden',
        name : 'hasPom',
        value : 'true'
      }, {
        tag : 'input',
        type : 'hidden',
        name : 'c',
        value : classifier
      }, {
        tag : 'input',
        type : 'hidden',
        name : 'e',
        value : extension
      }] : [repoTag, {
        tag : 'input',
        type : 'hidden',
        name : 'g',
        value : this.form.findField('g').getValue()
      }, {
        tag : 'input',
        type : 'hidden',
        name : 'a',
        value : this.form.findField('a').getValue()
      }, {
        tag : 'input',
        type : 'hidden',
        name : 'v',
        value : this.form.findField('v').getValue()
      }, {
        tag : 'input',
        type : 'hidden',
        name : 'p',
        value : this.form.findField('p').getValue()
      }, {
        tag : 'input',
        type : 'hidden',
        name : 'c',
        value : classifier
      }, {
        tag : 'input',
        type : 'hidden',
        name : 'e',
        value : extension
      }]
    });

    if (pomMode) {
      this.pomInput.appendTo(tmpForm);
    }
    if (fileInput) {
      fileInput.appendTo(tmpForm);
    }

    Ext.Ajax.request({
      url: Sonatype.config.repos.urls.upload,
      form : tmpForm,
      isUpload : true,
      cbPassThru : {
        treePanel : treePanel
      },
      callback : function(options, success, response) {
        tmpForm.remove();

        // This is a hack to get around the fact that upload submit
        // always returns
        // success = true
        var
              indexOfErrorMsg = response.responseText.toLowerCase().indexOf('<error>'),
              endIndexOfErrorMsg = response.responseText.toLowerCase().indexOf('</error>'),
              msg = 'Artifact upload failed.<br />',
              indexOfMsgContent = indexOfErrorMsg + 7,
              treePanel = options.cbPassThru.treePanel;

        if (indexOfErrorMsg === -1) {
          // get the json response and set the gavResponse
          this.gavResponse = Ext.decode(response.responseText);

          if (lastItem) {
            Sonatype.MessageBox.show({
              title : 'Upload Complete',
              msg : 'Artifact upload finished successfully',
              buttons : Sonatype.MessageBox.OK,
              icon : Sonatype.MessageBox.INFO
            });
            this.resetFields();
          }
          else {
            this.currentChildNode += 1;
            if (this.currentChildNode < treePanel.root.childNodes.length) {
              this.createUploadForm(treePanel,
                    treePanel.root.childNodes[this.currentChildNode].attributes.payload.fileInput,
                    treePanel.root.childNodes[this.currentChildNode].attributes.payload.classifier,
                    treePanel.root.childNodes[this.currentChildNode].attributes.payload.extension,
                    this.currentChildNode === treePanel.root.childNodes.length - 1);
            }
          }
        } else {
          this.logDebug('Upload failed: ' + response.responseText);

          this.gavResponse = null;

          if (endIndexOfErrorMsg > indexOfMsgContent) {
            msg += response.responseText.substring(indexOfMsgContent, endIndexOfErrorMsg);
          }
          else {
            msg += 'Check Nexus logs for more information.';
          }
          Sonatype.MessageBox.show({
            title : 'Upload Failed',
            msg : msg,
            buttons : Sonatype.MessageBox.OK,
            icon : Sonatype.MessageBox.ERROR
          });
        }
      },
      scope : this
    });
  }
}, function() {

  Sonatype.Events.addListener('repositoryViewInit', function(cardPanel, rec) {
    var sp = Sonatype.lib.Permissions;

    if (rec.data.resourceURI && rec.data.userManaged && sp.checkPermission('nexus:artifact', sp.CREATE)
          && rec.data.repoType === 'hosted' && rec.data.repoPolicy && rec.data.repoPolicy.toUpperCase()
          === 'RELEASE') {

      Ext.Ajax.request({
        url : rec.data.resourceURI,
        scope : this,
        callback : function(options, success, response) {
          if (success) {
            var statusResp = Ext.decode(response.responseText);
            if (statusResp.data) {
              if (statusResp.data.writePolicy === 'ALLOW_WRITE' || statusResp.data.writePolicy
                    === 'ALLOW_WRITE_ONCE') {
                var uploadPanel = new Sonatype.repoServer.ArtifactUploadPanel({
                  payload : rec,
                  name : 'upload'
                });
                var card = cardPanel.add({
                  xtype : 'panel',
                  layout : 'fit',
                  tabTitle : 'Artifact Upload',
                  name : 'uploadPanel',
                  items : [uploadPanel]
                });

                card.on('show', function(p) {
                  // This is a hack to fix the width of the edit box
                  // in IE
                  p.doLayout();
                  p.find('name', 'filenameField')[0].setValue('.');
                  p.find('name', 'filenameField')[0].setValue('');

                  // another hack to make the whole browse button
                  // clickable
                  if (!p.browseButtonsUpdated) {
                    var i, b = p.find('xtype', 'browsebutton');
                    for (i = 0; i < b.length; i++) {
                      b[i].setClipSize();
                    }
                    p.browseButtonsUpdated = true;
                  }

                  var gavDefComboField = p.find('name', 'gavDefinition')[0];
                  // another hack to fix the combo box lists
                  gavDefComboField.syncSize();
                });
              }
              else {
                cardPanel.add({
                  xtype : 'panel',
                  tabTitle : 'Artifact Upload',
                  name : 'upload',
                  items : [
                    {
                      border : false,
                      html : '<div class="little-padding">' + 'Artifact deployment is disabled for '
                            + rec.data.name
                            + '.<br /><br />' + 'You can enable it in the "Access Settings" section of the '
                            + 'repository configuration.</div>'
                    }
                  ]
                });
              }

              cardPanel.doLayout();

              return;
            }
          }
          Sonatype.utils.connectionError(response, 'There was a problem obtaining repository status.');
        }
      });
    }
  });
});

