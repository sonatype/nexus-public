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
/*global Ext, Nexus, Sonatype*/
(function() {

  var
        utils = Ext.namespace('Sonatype.utils'),
        passwordPlaceholder = '|$|N|E|X|U|S|$|',
        idPattern = /^[a-zA-Z0-9_\-\.]+$/;

  Ext.define('Nexus.util.Strings', {
          statics : {
            lowercase : function(str) {
              if (Ext.isEmpty(str))
              {
                return str;
              }
              str = str.toString();
              return str.toLowerCase();
            },
            sortFn : function(r1, r2) {
              var v1 = Nexus.util.Strings.lowercase(r1), v2 = Nexus.util.Strings.lowercase(r2);
              return v1 > v2 ? 1 : (v1 < v2 ? -1 : 0);
            },
            lowercaseFirstChar : function(str) {
              if (Ext.isEmpty(str))
              {
                return str;
              }
              str = str.toString();
              return str.charAt(0).toLowerCase() + str.slice(1);
            },
            upperFirstCharLowerRest : function(str) {
              if (Ext.isEmpty(str))
              {
                return str;
              }
              str = str.toString();
              return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
            },
            uppercase : function(str) {
              if (Ext.isEmpty(str))
              {
                return str;
              }
              str = str.toString();
              return str.toUpperCase();
            },
            capitalize : function(str) {
              if (Ext.isEmpty(str))
              {
                return str;
              }
              str = str.toString();
              return str.charAt(0).toUpperCase() + str.slice(1);
            },
            returnEmptyStr : function() {
              return '';
            },
            returnValidStr : function(str) {
              if (str)
              {
                return str;
              }

              return Nexus.util.Strings.returnEmptyStr();
            },
            validateNoSpaces : function(value) {
              if (value && value.indexOf(' ') !== -1)
              {
                return 'Spaces are not allowed in ID';
              }

              return true;
            },

            validateId : function(value) {
              if (idPattern.test(value))
              {
                return true;
              }

              return 'Only letters, digits, underscores(_), hyphens(-), and dots(.) are allowed in ID';
            },
            convert : {
              stringContextToBool : function(str) {
                return (str.toLowerCase() === 'true');
              },
              passwordToString : function(str) {
                if (passwordPlaceholder === str)
                {
                  return null;
                }

                if (str)
                {
                  return str;
                }

                return Nexus.util.Strings.returnEmptyStr();
              }
            }

          }
        }, function() {
          // Add string util methods to Sonatype.utils for backward compat
          Ext.applyIf(Sonatype.utils, Nexus.util.Strings);
        }
  );
}());
