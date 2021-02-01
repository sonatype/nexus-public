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
define('Nexus/error/ErrorHandler', ['extjs', 'nexus', 'Nexus/log'], function(Ext, Nexus, Log) {
  Ext.namespace('Nexus.error');

  Nexus.error.ErrorHandler = function() {
    return {
      init : function() {
        if ( !window.onerror ) {
          window.onerror = Nexus.error.handle;
        } else if ( window.onerror.createSequence ) {
          window.onerror = window.onerror.createSequence(Nexus.error.handle);
        } // else we don't have error display (e.g. with nexus-ui-testsuite siesta tests)
      },
      getFormattedMessage : function(args) {
        var
              lines = ["The following error has occurred:"],
              err = args[0],
              x;
        if (args[0] instanceof Error) { // Error object thrown in try...catch
          lines[lines.length] = "Message: (" + err.name + ") " + err.message;
          lines[lines.length] = "Error number: " + (err.number & 0xFFFF); //Apply binary arithmetic for IE number, firefox returns message string in element array element 0
          lines[lines.length] = "Description: " + err.description;
          lines[lines.length] = "Stacktrace: ";
          lines[lines.length] = err.stack;
        } else if ((args.length === 3) && (typeof(args[2]) === "number")) { // Check the signature for a match with an unhandled exception
          lines[lines.length] = "Message: " + args[0];
          lines[lines.length] = "URL: " + args[1];
          lines[lines.length] = "Line Number: " + args[2];
        } else {
          lines = ["An unknown error has occurred."];
          lines[lines.length] = "The following information may be useful:";
          for (x = 0; x < args.length; x+=1) {
            lines[lines.length] = Ext.encode(args[x]);
          }
        }
        return lines.join("\n");
      },
      displayError : function(args) {
        // purposely creating a new window for each exception (to handle concurrent exceptions)
        var errWindow = new Ext.Window({
          autoScroll : true,
          bodyStyle : {padding : 5},
          height : 150,
          html : this.getFormattedMessage(args).replace(/\n/g, "<br />").replace(/\t/g, " &nbsp; &nbsp;"),
          title : "An error has occurred",
          width : 400
        });
        errWindow.show();
        errWindow.alignTo(Ext.getCmp('st-main-tab-panel').el, 'tr-tr');
      },
      handleError : function() {
        var args = [];
        for (var x = 0; x < arguments.length; x++) {
          args[x] = arguments[x];
        }
        try {
          Log.error(this.getFormattedMessage(args));

          if ('?debug' === window.location.search) {
            this.displayError(args);
          }
        }
        catch (e) {
          // don't introduce even more errors when displaying errors
        }
        // let handling continue to bubble up to firebug console etc.
        return false;
      }
    };
  }();

  Nexus.error.handle = Nexus.error.ErrorHandler.handleError.createDelegate(Nexus.error.ErrorHandler);

  Nexus.error.ErrorHandler.init();

  return Nexus.error.ErrorHandler;
});
