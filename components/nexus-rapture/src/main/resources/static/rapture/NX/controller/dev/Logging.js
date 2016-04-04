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
/*global Ext*/

/**
 * Logging dev-panel controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.dev.Logging', {
  extend: 'NX.app.Controller',
  requires: [
    'Ext.util.Format',
    'NX.Windows'
  ],

  stores: [
    'LogEvent',
    'LogLevel'
  ],

  refs: [
    {
      ref: 'panel',
      selector: 'nx-dev-logging'
    }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    // helper to lookup sink by name via Logging controller
    function sink(name) {
      return me.getController('Logging').getSink(name);
    }

    me.listen({
      component: {
        'nx-dev-logging button[action=clear]': {
          click: function (button) {
            me.getStore('LogEvent').removeAll();
          }
        },

        'nx-dev-logging combobox[itemId=threshold]': {
          afterrender: function (combo) {
            combo.select(me.getController('Logging').getThreshold());
          },
          select: function (combo) {
            me.getController('Logging').setThreshold(combo.getValue());
          }
        },

        'nx-dev-logging checkbox[itemId=buffer]': {
          afterrender: function (checkbox) {
            checkbox.setValue(sink('store').enabled);
          },
          change: function (checkbox) {
            sink('store').setEnabled(checkbox.getValue());
          }
        },

        'nx-dev-logging numberfield[itemId=bufferSize]': {
          afterrender: function (field) {
            field.setValue(sink('store').maxSize);
          },

          // update the max-size when enter or loss of focus
          blur: function(field, event) {
            sink('store').setMaxSize(field.getValue());
          },
          keypress: function (field, event) {
            if (event.getKey() === event.ENTER) {
              sink('store').setMaxSize(field.getValue());
            }
          }
        },

        'nx-dev-logging checkbox[itemId=console]': {
          afterrender: function (checkbox) {
            checkbox.setValue(sink('console').enabled);
          },
          change: function (checkbox) {
            sink('console').setEnabled(checkbox.getValue());
          }
        },

        'nx-dev-logging checkbox[itemId=remote]': {
          afterrender: function (checkbox) {
            checkbox.setValue(sink('remote').enabled);
          },
          change: function (checkbox) {
            sink('remote').setEnabled(checkbox.getValue());
          }
        },

        'nx-dev-logging button[action=export]': {
          click: me.exportSelection
        }
      }
    });
  },

  /**
   * Export selected rows to window.
   *
   * @private
   */
  exportSelection: function() {
    var win,
        doc,
        selected;

    win = NX.Windows.open('', '', 'width=640,height=480');
    if (win !== null) {
      doc = win.document;
      selected = Ext.Array.pluck(this.getPanel().getSelectionModel().getSelection(), 'data');

      doc.write('<html><head>');
      doc.write('<title>' + Ext.util.Format.plural(selected.length, 'Logging Event') + '</title>');
      doc.write('</head><body>');
      doc.write('<pre>');

      Ext.Array.each(selected, function(data) {
        doc.write(data.timestamp + ' ' + data.level + ' ' + data.logger + ' ' + data.message.join(' ') + '<br/>');
      });

      doc.write('<pre>');
      doc.write('</body></html>');
    }
  }
});
