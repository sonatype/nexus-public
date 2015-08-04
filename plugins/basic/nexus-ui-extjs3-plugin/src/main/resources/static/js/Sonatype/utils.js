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
/*global define*/
define('Sonatype/utils',['../extjs', 'Nexus/config', 'Nexus/util/Format', 'Sonatype/strings', 'sonatype'], function(Ext, Config, format, Strings, Sonatype) {

  var ns = Ext.namespace('Sonatype.utils');

  Ext.applyIf(ns, {
    edition : '',
    editionShort : '',
    version : '',
    attributionsURL : '',
    purchaseURL : '',
    userLicenseURL : '',

    // deep copy of an object. All references independent from object passed in.
    cloneObj : function(o) {
      if (typeof(o) !== 'object' || o === null)
      {
        return o;
      }

      var i, newObj = {};

      for (i in o)
      {
        if (o.hasOwnProperty(i)) {
          newObj[i] = ns.cloneObj(o[i]);
        }
      }

      return newObj;
    },

    // (Array : arr, string : child, [string seperator])
    // array to join, name of element of contained object, seperator (defaults
    // to ", ")
    joinArrayObject : function(arr, child, seperator) {
      var i, sOut = '', sep = seperator || ', ';

      for (i = 0; i < arr.length; i=i+1)
      {
        if ((arr[i])[child])
        {
          sOut += (arr[i])[child] + sep;
        }
      }

      if (sOut !== "")
      {
        return sOut.substring(0, sOut.length - sep.length);
      }
      else
      {
        return sOut;
      }
    },

    updateGlobalTimeout : function() {
      Ext.Ajax.request({
            method : 'GET',
            scope : this,
            url : Config.repos.urls.restApiSettings,
            callback : function(options, success, response) {
              if (success)
              {
                var dec = Ext.decode(response.responseText);
                Ext.Ajax.timeout = dec.data.uiTimeout * 1000;
              }
              else
              {
                Ext.Ajax.timeout = 60000;
                ns.connectionError(response, 'Error retrieving rest timeout');
              }
            }
          });
    },

    parseHTMLErrorMessage : function(text) {
      var
            n1 = text.toLowerCase().indexOf('<p>') + 3,
            n2 = text.toLowerCase().indexOf('</p>');

      if (n2 > n1) {
        return text.substring(n1, n2);
      }

      return '';
    },

    /**
     * Show a message box with content according to the response.
     *
     * If the login window is visible, this method will look at the X-Nexus-Reason error
     * and offer a password change if that header content is 'expired'.
     *
     * Used options:
     * # options.decodeErrorResponse: Try to parse the responseText as a JSON error response, or a HTML error page.
     * # options.hideErrorStatus: If a message could be taken out of the responseText, HTTP status code and reason will
     *                            not be included in the message.
     *
     * @param response The response object of the request.
     * @param message The message to show.
     * @param offerRestart Whether to offer a reload of the UI.
     * @param options The options object of the request.
     * @param showResponseText Show the plain responseText for 'Bad Request' errors.
     *
     */
    connectionError : function(response, message, offerRestart, options, showResponseText) {
      // prime options object if necessary
      options = options || {};

      var
            i, errorResponse,
            serverMessage = '',
            r = response.responseText,
            displayMessage;

      if (r)
      {
        if (options.decodeErrorResponse) {
          var contentType = response.getResponseHeader("Content-Type");
          if( contentType === "application/vnd.siesta-error-v1+json") {
            // response will be a json serialized org.sonatype.sisu.siesta.common.error.ErrorXO
            serverMessage = Ext.decode(r).message;
          } else if( contentType === "application/vnd.siesta-validation-errors-v1+json") {
            // response will be a json serialized array of org.sonatype.sisu.siesta.common.validation.ValidationErrorXO
            validationErrors = Ext.decode(r);
            for (i = 0; i < validationErrors.length; i=i+1)
            {
              if(serverMessage.length > 0) {
                serverMessage += '<br /><br />';
              }
              serverMessage += validationErrors[i].message;
            }
          } else if(r.toLowerCase().indexOf('"errors"') > -1) {
            // last, check if we have an json serialized restlet1x org.sonatype.plexus.rest.resource.error.ErrorResponse
            errorResponse = Ext.decode(r);
            for (i = 0; i < errorResponse.errors.length; i=i+1)
            {
              if(serverMessage.length > 0) {
                serverMessage += '<br /><br />';
              }
              serverMessage += errorResponse.errors[i].msg;
            }
          }
        }
        else
        {
          serverMessage = ns.parseHTMLErrorMessage(r);
        }
      }

      if (Sonatype.repoServer.RepoServer.loginWindow.isVisible() && (response.status == 403 || response.status == 401))
      {
        if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
        {
          var nexusReason = response.getResponseHeader['X-Nexus-Reason'];
          if (nexusReason && nexusReason.substring(0, 7) == 'expired')
          {
            Sonatype.repoServer.RepoServer.loginWindow.hide();
            ns.changePassword(Sonatype.repoServer.RepoServer.loginForm.find('name', 'username')[0].getValue());
          }
          else
          {
            Ext.MessageBox.show({
                  title : 'Login Error',
                  msg : 'Incorrect username, password or no permission to use the Nexus User Interface.<br />Try again.' + serverMessage,
                  buttons : Ext.MessageBox.OK,
                  icon : Ext.MessageBox.ERROR,
                  animEl : 'mb3',
                  fn : this.focusPassword
                });
          }
        }
      }
      else
      {
        if (message == null)
        {
          message = '';
        }
        if (serverMessage)
        {
          message += serverMessage;
        }

        var responseStatus = response.status;
        var responseStatusText = response.statusText;

        // the prompt is in the following format

        // <message> (optional)
        //
        // <response text> (if 400)
        //
        // <detail> (include status code if valid)
        //
        // <retry message> (optional)

        displayMessage = '';

        // caller provided message + serverMessage
        // status == -1 is request timed out (?), don't show message then
        if (message && responseStatus != -1) {
          displayMessage = message;
        }

        // show response text if requested (and makes sense, status 400)
        if (responseStatus == 400 && showResponseText) {
          if (displayMessage.length > 0) {
            displayMessage += '<br/><br/>';
          }
          displayMessage += response.responseText;
        }
        // otherwise show statusLine if not requested to hide and we have a serverMessage
        // (we want to always provide some kind of server message, either error msg or status line)
        else {
          if (!(options.hideErrorStatus && serverMessage)) {
            if (displayMessage.length > 0) {
              displayMessage += '<br/><br/>';
            }
            // hide -1 status
            if (responseStatus && responseStatus != -1) {
              displayMessage += 'Nexus returned an error: ERROR ' + responseStatus + ': ' + responseStatusText;
            }
            else {
              displayMessage += 'There was an error communicating with the server: request timed out.';
            }
          }
        }

        if (offerRestart) {
          if (displayMessage.length > 0) {
            displayMessage += '<br/><br/>';
          }
          displayMessage += 'Click OK to reload the console or CANCEL if you wish to retry the same action in a little while.';
        }

        Ext.MessageBox.show({
          title: "Error",
          msg: displayMessage,
          buttons: offerRestart ? Ext.MessageBox.OKCANCEL : Sonatype.MessageBox.OK,
          icon: Ext.MessageBox.ERROR,
          animEl: 'mb3',
          fn: function (button) {
            if (offerRestart && button == "ok") {
              window.location.reload();
            }
          }
        });
      }
    },
    /**
     * Call this after the error signing in dialog appears. Otherwise focus
     * doesn't get put in the password field correctly.
     */
    focusPassword : function() {
      if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
      {
        Sonatype.repoServer.RepoServer.loginForm.find('name', 'password')[0].focus(true);
      }
    },
    /**
     * Base64 encode / decode http://www.webtoolkit.info/
     */
    base64 : function() {
      // private property
      var _keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

      // private method for UTF-8 encoding
      var _utf8_encode = function(string) {
        string = string.replace(/\r\n/g, "\n");
        var utftext = "";

        for (var n = 0; n < string.length; n++)
        {
          var c = string.charCodeAt(n);

          if (c < 128)
          {
            utftext += String.fromCharCode(c);
          }
          else if ((c > 127) && (c < 2048))
          {
            utftext += String.fromCharCode((c >> 6) | 192);
            utftext += String.fromCharCode((c & 63) | 128);
          }
          else
          {
            utftext += String.fromCharCode((c >> 12) | 224);
            utftext += String.fromCharCode(((c >> 6) & 63) | 128);
            utftext += String.fromCharCode((c & 63) | 128);
          }
        }

        return utftext;
      };

      // private method for UTF-8 decoding
      var _utf8_decode = function(utftext) {
        var string = "";
        var i = 0;
        var c = c1 = c2 = 0;

        while (i < utftext.length)
        {
          c = utftext.charCodeAt(i);

          if (c < 128)
          {
            string += String.fromCharCode(c);
            i++;
          }
          else if ((c > 191) && (c < 224))
          {
            c2 = utftext.charCodeAt(i + 1);
            string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
            i += 2;
          }
          else
          {
            c2 = utftext.charCodeAt(i + 1);
            c3 = utftext.charCodeAt(i + 2);
            string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
            i += 3;
          }
        }

        return string;
      };

      return {
        // public method for encoding
        encode : function(input) {
          var output = "";
          var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
          var i = 0;

          input = _utf8_encode(input);

          while (i < input.length)
          {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2))
            {
              enc3 = enc4 = 64;
            }
            else if (isNaN(chr3))
            {
              enc4 = 64;
            }

            output = output + _keyStr.charAt(enc1) + _keyStr.charAt(enc2) + _keyStr.charAt(enc3) + _keyStr.charAt(enc4);
          }

          return output;
        },

        // public method for decoding
        decode : function(input) {
          var output = "";
          var chr1, chr2, chr3;
          var enc1, enc2, enc3, enc4;
          var i = 0;

          input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

          while (i < input.length)
          {
            enc1 = _keyStr.indexOf(input.charAt(i++));
            enc2 = _keyStr.indexOf(input.charAt(i++));
            enc3 = _keyStr.indexOf(input.charAt(i++));
            enc4 = _keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64)
            {
              output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64)
            {
              output = output + String.fromCharCode(chr3);
            }
          }

          output = _utf8_decode(output);

          return output;
        }
      }
    }(),

    defaultToNo : function() {
      // @note: this handler selects the "No" button as the default
      // @todo: could extend Ext.MessageBox to take the button to select as
      // a param
      Ext.MessageBox.getDialog().on('show', function() {
            this.focusEl = this.buttons[2]; // ack! we're offset dependent here
            this.focus();
          }, Ext.MessageBox.getDialog(), {
            single : true
          });
    },

    getCookie : function(cookieName) {
      var c = document.cookie + ";";
      var re = /\s?(.*?)=(.*?);/g;
      var matches;
      while ((matches = re.exec(c)) != null)
      {
        if (matches[1] == cookieName)
        {
          return matches[2];
        }
      }
      return null;
    },

    setCookie : function(cookieName, value) {
      document.cookie = cookieName + "=" + value + "; path=" + Sonatype.config.resourcePath
    },

    clearCookie : function(cookieName) {
      document.cookie = cookieName + "=null; expires=Thu, 01-Jan-70 00:00:01 GMT" + "; path=" + Sonatype.config.resourcePath
    },

    recoverUsername : function() {
      var w = new Ext.Window({
            title : 'Username Recovery',
            closable : true,
            autoWidth : false,
            width : 300,
            autoHeight : true,
            modal : true,
            constrain : true,
            resizable : false,
            draggable : false,
            items : [{
                  xtype : 'form',
                  labelAlign : 'right',
                  labelWidth : 60,
                  frame : true,
                  defaultType : 'textfield',
                  monitorValid : true,
                  items : [{
                        xtype : 'panel',
                        style : 'padding-left: 70px; padding-bottom: 10px',
                        html : 'Please enter the e-mail address you used to register your account and we will send you your username.'
                      }, {
                        fieldLabel : 'E-mail',
                        name : 'email',
                        width : 200,
                        allowBlank : false
                      }],
                  buttons : [{
                        text : 'E-mail Username',
                        formBind : true,
                        scope : this,
                        handler : function() {
                          var email = w.find('name', 'email')[0].getValue();

                          Ext.Ajax.request({
                                scope : this,
                                method : 'POST',
                                jsonData : {
                                  data : {
                                    email : email
                                  }
                                },
                                url : Sonatype.config.repos.urls.usersForgotId + '/' + email,
                                success : function(response, options) {
                                  w.close();
                                  Ext.MessageBox.show({
                                        title : 'Username Recovery',
                                        msg : 'Username request completed successfully.' + '<br /><br />' + 'Check your mailbox, the username reminder should arrive in a few minutes.',
                                        buttons : Ext.MessageBox.OK,
                                        icon : Ext.MessageBox.INFO,
                                        animEl : 'mb3'
                                      });
                                },
                                failure : function(response, options) {
                                  var errorOptions = {
                                    hideErrorStatus : true
                                  };
                                  ns.connectionError(response, 'There is a problem retrieving your username.', false, errorOptions)
                                }
                              });
                        }
                      }, {
                        text : 'Cancel',
                        formBind : false,
                        scope : this,
                        handler : function() {
                          w.close();
                        }
                      }]
                }]
          });

      w.show();
    },

    recoverPassword : function() {
      var w = new Ext.Window({
            title : 'Password Recovery',
            closable : true,
            autoWidth : false,
            width : 300,
            autoHeight : true,
            modal : true,
            constrain : true,
            resizable : false,
            draggable : false,
            items : [{
                  xtype : 'form',
                  labelAlign : 'right',
                  labelWidth : 60,
                  frame : true,
                  defaultType : 'textfield',
                  monitorValid : true,
                  items : [{
                        xtype : 'panel',
                        style : 'padding-left: 70px; padding-bottom: 10px',
                        html : 'Please enter your username and e-mail address below. We will send you a new password.'
                      }, {
                        fieldLabel : 'Username',
                        name : 'username',
                        width : 200,
                        allowBlank : false
                      }, {
                        fieldLabel : 'E-mail',
                        name : 'email',
                        width : 200,
                        allowBlank : false
                      }],
                  buttons : [{
                        text : 'Reset Password',
                        formBind : true,
                        scope : this,
                        handler : function() {
                          var username = w.find('name', 'username')[0].getValue();
                          var email = w.find('name', 'email')[0].getValue();

                          Ext.Ajax.request({
                                scope : this,
                                method : 'POST',
                                jsonData : {
                                  data : {
                                    userId : username,
                                    email : email
                                  }
                                },
                                url : Sonatype.config.repos.urls.usersForgotPassword,
                                success : function(response, options) {
                                  w.close();
                                  Ext.MessageBox.show({
                                        title : 'Reset Password',
                                        msg : 'Password request completed successfully.' + '<br /><br />' + 'Check your mailbox, your new password should arrive in a few minutes.',
                                        buttons : Ext.MessageBox.OK,
                                        icon : Ext.MessageBox.INFO,
                                        animEl : 'mb3'
                                      });
                                },
                                failure : function(response, options) {
                                  var errorOptions = {
                                    hideErrorStatus : true
                                  };
                                  ns.connectionError(response, 'There is a problem resetting your password.', false, errorOptions)
                                }
                              });
                        }
                      }, {
                        text : 'Cancel',
                        formBind : false,
                        scope : this,
                        handler : function() {
                          w.hide();
                          w.close();
                        }
                      }]
                }]
          });

      w.show();
    },

    changePassword : function(expiredUsername) {
      var w = new Ext.Window({
            id : 'change-password-window',
            title : 'Change Password',
            closable : true,
            autoWidth : false,
            width : 350,
            autoHeight : true,
            modal : true,
            constrain : true,
            resizable : false,
            draggable : false,
            items : [{
                  xtype : 'form',
                  labelAlign : 'right',
                  labelWidth : 110,
                  frame : true,
                  defaultType : 'textfield',
                  monitorValid : true,
                  items : [{
                        xtype : 'panel',
                        style : 'padding-left: 70px; padding-bottom: 10px',
                        html : (expiredUsername ? 'Your password has expired, you need to reset it. ' : '') + 'Please enter your current password and then the new password twice to confirm.'
                      }, {
                        fieldLabel : 'Current Password',
                        inputType : 'password',
                        name : 'currentPassword',
                        width : 200,
                        allowBlank : false,
                        validateOnBlur : false
                      }, {
                        fieldLabel : 'New Password',
                        inputType : 'password',
                        name : 'newPassword',
                        width : 200,
                        allowBlank : false,
                        validateOnBlur : false
                      }, {
                        fieldLabel : 'Confirm Password',
                        inputType : 'password',
                        name : 'confirmPassword',
                        width : 200,
                        allowBlank : false,
                        validateOnBlur : false,
                        validator : function(s) {
                          var firstField = this.ownerCt.find('name', 'newPassword')[0];
                          if (firstField && firstField.getRawValue() != s)
                          {
                            return "Passwords don't match";
                          }
                          return true;
                        }
                      }],
                  buttons : [{
                        id : 'change-password-button',
                        text : 'Change Password',
                        formBind : true,
                        scope : this,
                        handler : function() {
                          var currentPassword = w.find('name', 'currentPassword')[0].getValue();
                          var newPassword = w.find('name', 'newPassword')[0].getValue();

                          Ext.Ajax.request({
                                scope : this,
                                method : 'POST',
                                cbPassThru : {
                                  newPassword : newPassword
                                },
                                jsonData : {
                                  data : {
                                    userId : expiredUsername ? expiredUsername : Sonatype.user.curr.username,
                                    oldPassword : currentPassword,
                                    newPassword : newPassword
                                  }
                                },
                                url : Sonatype.config.repos.urls.usersChangePassword,
                                success : function(response, options) {
                                  if (expiredUsername)
                                  {
                                    ns.doLogin(w, expiredUsername, newPassword);
                                    w.close();
                                  }
                                  else
                                  {
                                    w.close();
                                    Ext.MessageBox.show({
                                          title : 'Password Changed',
                                          msg : 'Password change request completed successfully.',
                                          buttons : Ext.MessageBox.OK,
                                          icon : Ext.MessageBox.INFO,
                                          animEl : 'mb3'
                                        });
                                  }
                                },
                                failure : function(response, options) {
                                  ns.connectionError(response, 'There is a problem changing your password.', false, { hideErrorStatus: true })
                                }
                              });
                        }
                      }, {
                        text : 'Cancel',
                        formBind : false,
                        scope : this,
                        handler : function() {
                          w.close();
                        }
                      }]
                }]
          });

      w.show();
    },

    getAuthToken : function(username, password) {
      return ns.base64.encode(username + ':' + password);
    },

    refreshTask : (function() {
      var config, running;

      running = null;

      config = {
        run : function() {
          Ext.Ajax.request({
            method : 'GET',
            options : {
              ignore401 : true
            },
            url : Sonatype.config.repos.urls.status,
            callback : function(options, success, response) {
              // do nothing, we only request to refresh session
              // but stop the task if something goes wrong (forbidden, not found, connection refused etc.)
              if (!success) {
                ns.refreshTask.stop();
              }
            }
          });
        },
        interval : 15 * 60 * 1000
      };

      // catch the case of page reload, which will leave the user logged in but not go through the login handler
      Sonatype.Events.addListener('nexusSettings', function () {
        ns.refreshTask.start();
      }, this);

      // public API
      return {
        stop : function() {
          if (running !== null) {
            Ext.TaskMgr.stop(running);
            running = null;
          }
        },
        start : function() {
          if (running === null && Sonatype.utils.settings.keepAlive) {
            running = Ext.TaskMgr.start(config);
          }
        }};
    })(),

    doLogin : function(activeWindow, username, password) {
      if (activeWindow)
      {
        activeWindow.getEl().mask("Logging you in...");
      }

      Ext.Ajax.request({
            method : 'GET',
            cbPassThru : {
              username : username
            },
            headers : {
              'Authorization' : 'Basic ' + ns.getAuthToken(username, password)
            }, // @todo: send HTTP basic auth data
            url : Sonatype.config.repos.urls.login,
            success : function(response, options) {
              if (activeWindow)
              {
                activeWindow.getEl().unmask();
              }

              if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
              {
                Sonatype.view.loginSuccessfulToken = Sonatype.view.afterLoginToken;
                Sonatype.repoServer.RepoServer.loginWindow.hide();
                Sonatype.repoServer.RepoServer.loginForm.getForm().reset();
              }

              ns.loadNexusStatus();
            },
            failure : function(response, options) {
              Ext.namespace('Sonatype.utils').refreshTask.stop();
              if (activeWindow)
              {
                activeWindow.getEl().unmask();
              }

              if (Sonatype.repoServer.RepoServer.loginWindow.isVisible())
              {
                Sonatype.repoServer.RepoServer.loginForm.find('name', 'password')[0].focus(true);
              }

              Ext.Ajax.request({
                  scope : this,
                  method : 'GET',
                  url : Sonatype.config.repos.urls.logout,
                  callback : function(options, success, response) {
                    Sonatype.view.justLoggedOut = true;
                  }
                });

            }

          });
    },

    parseFormattedAppName : function(formattedAppName) {
      return formattedAppName;
    },

    /**
     * Loads Nexus settings.
     */
    loadNexusSettings: function () {
      Ext.Ajax.request({
        method: 'GET',
        suppressStatus: true,
        url: Sonatype.config.contextPath + '/service/siesta/wonderland/settings',
        callback: function (options, success, response) {
          var respObj;

          ns.settings = {};
          if (success) {
            respObj = Ext.decode(response.responseText);

            Ext.each(respObj, function(entry){
              ns.settings[entry.key] = entry.value;
            });
          }
          ns.settings.keepAlive = ns.settings.keepAlive === 'true';
          Sonatype.Events.fireEvent('nexusSettings');
        }
      });
    },

    loadNexusStatus : function() {
      Sonatype.user.curr = ns.cloneObj(Sonatype.user.anon);

      Ext.Ajax.request({
            method : 'GET',
            options : {
              ignore401 : true
            },
            url : Sonatype.config.repos.urls.status,
            callback : function(options, success, response) {
              var baseUrlMismatch = false;

              Sonatype.view.historyDisabled = true;

              if (success)
              {
                var respObj = Ext.decode(response.responseText);

                ns.edition = respObj.data.editionLong;
                ns.editionShort = respObj.data.editionShort;
                ns.version = respObj.data.version;
                ns.attributionsURL = respObj.data.attributionsURL;
                ns.purchaseURL = respObj.data.purchaseURL;
                ns.userLicenseURL = respObj.data.userLicenseURL;
                ns.licenseInstalled = respObj.data.licenseInstalled;
                ns.licenseExpired = respObj.data.licenseExpired;
                ns.trialLicense = respObj.data.trialLicense;

                ns.formattedAppName = Ext.namespace('Sonatype.utils').parseFormattedAppName(respObj.data.formattedAppName);

                Ext.get('logo').update('<span>' + ns.formattedAppName + '</span>');

                Sonatype.view.headerPanel.doLayout();

                Sonatype.user.curr.repoServer = respObj.data.clientPermissions.permissions;
                Sonatype.user.curr.isLoggedIn = respObj.data.clientPermissions.loggedIn;
                Sonatype.user.curr.username = respObj.data.clientPermissions.loggedInUsername;
                Sonatype.user.curr.loggedInUserSource = respObj.data.clientPermissions.loggedInUserSource;

                var baseUrl = respObj.data.baseUrl;
                baseUrlMismatch = (baseUrl.toLowerCase() != window.location.href.substring(0, baseUrl.length).toLowerCase());
              }
              else
              {
                Ext.namespace('Sonatype.utils').edition = '';
                Ext.namespace('Sonatype.utils').licenseInstalled = null;
                Ext.namespace('Sonatype.utils').licenseExpired = null;
                Ext.namespace('Sonatype.utils').trialLicense = null;
                Ext.namespace('Sonatype.utils').anonDisabled = (response.status === 401);

                Sonatype.user.curr.repoServer = null;
                Sonatype.user.curr.isLoggedIn = null;
                Sonatype.user.curr.username = null;
                Sonatype.user.curr.loggedInUserSource = null;

              }

              var availSvrs = Sonatype.config.installedServers;
              for (var srv in availSvrs)
              {
                if (availSvrs[srv] && typeof(Sonatype[srv]) != 'undefined')
                {
                  Sonatype[srv][Strings.capitalize(srv)].statusComplete(respObj);
                }
              }

              Sonatype.Events.fireEvent('initHeadLinks');

              Sonatype.view.serverTabPanel.doLayout();

              if (baseUrlMismatch && Sonatype.lib.Permissions.checkPermission('nexus:settings', Sonatype.lib.Permissions.READ))
              {
                ns.postWelcomePageAlert('<b>WARNING:</b> ' + 'Base URL setting of <a href="' + baseUrl + '">' + baseUrl + '</a> ' + 'does not match your actual URL! ' + 'If you\'re running Apache mod_proxy, here\'s '
                    + '<a href="http://links.sonatype.com/products/nexus/oss/docs-mod-proxy">' + 'more information</a> on configuring Nexus with it.' );
              }

              Sonatype.Events.fireEvent('nexusStatus');

              Sonatype.view.historyDisabled = false;
              var token = Ext.History.getToken();
              if (Sonatype.view.loginSuccessfulToken)
              {
                token = Sonatype.view.loginSuccessfulToken;
                Sonatype.view.loginSuccessfulToken = null;
              }
              ns.onHistoryChange(token);
              ns.updateHistory();
              Sonatype.view.justLoggedOut = false;

              if (Sonatype.lib.Permissions.checkPermission('nexus:settings', Sonatype.lib.Permissions.READ)) {
                ns.loadNexusSettings();
              }
            }
          });
    },

    postWelcomePageAlert : function(msg) {
      // we want to insert the toolbar first, rather than add to the page in
      // random spot
      Sonatype.view.welcomeTab.insert(0, {
            xtype : 'panel',
            html : '<div class="x-toolbar-warning-box"><div class="x-form-invalid-msg" style="width: 95%;">' + msg + '</div></div>'
          });
      Sonatype.view.welcomeTab.doLayout();
    },

    onHistoryChange : function(token) {
      if (!token && window.location.hash)
      {
        token = window.location.hash.substring(1);
      }

      Sonatype.view.historyDisabled = true;
      var eventResult = Sonatype.Events.fireEvent('historyChanged',token);
      Sonatype.view.historyDisabled = false;

      //if event returns true, that means no event listener handled the data and stopped the process, so handle by default means
      if (token && Sonatype.user.curr.repoServer && Sonatype.user.curr.repoServer.length && eventResult)
      {
        Sonatype.view.historyDisabled = true;

        var toks = token.split(Sonatype.view.HISTORY_DELIMITER);

        var tabId = toks[0];
        var tabPanel = Sonatype.view.mainTabPanel.getComponent(tabId);
        if (tabPanel)
        {
          Sonatype.view.mainTabPanel.setActiveTab(tabId);
        }
        else
        {
          var navigationPanel = Ext.getCmp('navigation-' + tabId);
          if (navigationPanel)
          {
            var c = navigationPanel.initialConfigNavigation;
            if (c)
            {
              tabPanel = Sonatype.view.mainTabPanel.addOrShowTab(c.tabId, c.tabCode, {
                    title : c.tabTitle ? c.tabTitle : c.title
                  });
            }
          }
          else if (Sonatype.view.supportedNexusTabs[tabId] && !Sonatype.view.justLoggedOut && !Sonatype.user.curr.isLoggedIn)
          {
            Sonatype.view.historyDisabled = false;
            Sonatype.view.afterLoginToken = token;
            Sonatype.repoServer.RepoServer.loginHandler();
            return;
          }
        }
        Sonatype.view.historyDisabled = false;

        if (tabPanel && tabPanel.applyBookmark && toks.length > 1)
        {
          tabPanel.applyBookmark(toks[1]);
        }
      }
    },

    updateHistory : function(tab) {
      var bookmark = ns.getBookmark(tab);
      if (!bookmark)
      {
        return;
      }
      var oldBookmark = Ext.History.getToken();
      if (bookmark != oldBookmark)
      {
        Ext.History.add(bookmark);
      }
    },

    replaceHistory : function(tab) {
      var bookmark = ns.getBookmark(tab);
      if (!bookmark)
      {
        return;
      }
      location.replace(window.location.href.replace(/\#.*$/, '') + '#' + bookmark);
    },

    getBookmark : function(tab) {
      if (Sonatype.view.historyDisabled)
        return;

      var bookmark = null;

      if (tab)
      {
        if (tab.ownerCt != Sonatype.view.mainTabPanel || tab != Sonatype.view.mainTabPanel.getActiveTab())
          return;

        bookmark = tab.id;
      }
      else
      {
        // this is to handle very special where we are passing a bookmark on
        // redirect, in IE this is not handled very well at all, so we trick it
        var oldBookmark = Ext.History.getToken();
        if (oldBookmark && oldBookmark.indexOf('force') == 0)
        {
          bookmark = oldBookmark.substring('force'.length);
        }
        else
        {
          tab = Sonatype.view.mainTabPanel.getActiveTab();
          bookmark = tab.id;
        }
      }

      if (tab && tab.getBookmark)
      {
        var b2 = tab.getBookmark();
        if (b2)
        {
          bookmark += Sonatype.view.HISTORY_DELIMITER + b2;
        }
      }

      return bookmark;
    },

    openWindow : function(url) {
      window.open(url);
    }
  });

  return Sonatype;
});

