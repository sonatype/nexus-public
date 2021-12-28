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
/*global Ext, NX*/

/**
 * Outreach controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.Outreach', {
  extend: 'NX.app.Controller',
  requires: [
    'NX.util.Url',
    'NX.State',
    'NX.Permissions'
  ],

  refs: [
    { ref: 'welcomePage', selector: 'nx-dashboard-welcome' }
  ],

  /**
   * @override
   */
  init: function () {
    var me = this;

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.refreshOutreachContent
        },
        '#State': {
          userchanged: me.refreshOutreachContent
        }
      },
      component: {
        'nx-dashboard-welcome': {
          afterrender: me.refreshOutreachContent
        }
      }
    });
  },

  showOutreach: function (log4jDisclaimerAvailable) {
    var me = this,
        welcomePage = me.getWelcomePage();

    NX.direct.outreach_Outreach.readStatus(function (response) {
      if (Ext.isObject(response) && response.success && response.data != null && welcomePage.rendered) {
        var user = NX.State.getUser(),
            daysToExpiry = NX.State.getValue("license").daysToExpiry,
            usertype,
            url,
            height;

        if (user) {
          usertype = user.administrator ? 'admin' : 'normal';
          height = log4jDisclaimerAvailable ? 'calc(100% - 250px)' : '100%';
        }
        else {
          usertype = 'anonymous';
          height = '100%';
        }

        url = NX.util.Url.urlOf('service/outreach/?version=' + NX.State.getVersion() +
            '&versionMm=' + NX.State.getVersionMajorMinor() +
            '&edition=' + NX.State.getEdition() +
            '&usertype=' + usertype +
            '&daysToExpiry=' + daysToExpiry);

        // add the outreach iframe to the welcome view
        welcomePage.add({
          xtype: 'uxiframe',
          itemId: 'outreach',
          anchor: '100%',
          width: '100%',
          height: height,
          flex: 1,
          border: false,
          frame: false,
          hidden: true,
          src: url,
          // override renderTpl to add title attribute for accessibility purpose
          renderTpl: [
            '<iframe src="{src}" id="{id}-iframeEl" data-ref="iframeEl" name="{frameName}" title="Nexus Repository Manager Outreach" width="100%" height="100%" frameborder="0"></iframe>'
          ],
          listeners: {
            load: function () {
              var iframe = this;
              // if the outreach content has loaded properly, show it
              if (iframe.getWin().iframeLoaded) {
                iframe.show();
              }
              else {
                // else complain and leave it hidden
                //<if debug>
                me.logDebug('Outreach iframe did not load: ' + url);
                //</if>
              }
            }
          }
        });
      }
    });
  },

  /**
   * @private
   * Add/Remove outreach content to/from welcome page, if outreach content is available.
   */
  refreshOutreachContent: function () {
    var me = this,
        welcomePage = me.getWelcomePage(),
        log4jDisclaimerAvailable = true;

    if (welcomePage) {
      welcomePage.removeAll();
      NX.direct.outreach_Outreach.isAvailableLog4jDisclaimer(function(response) {
        if (Ext.isObject(response) && response.success && response.data && welcomePage.rendered) {
          // log4j disclaimer window should be shown in case of Log4j Capability is disabled and vice versa
          log4jDisclaimerAvailable = response.data === 'false';
          var user = NX.State.getUser();
          if (user && user.administrator) {
            welcomePage.add({
              xtype: 'container',
              id: 'log4jDisclaimer',
              hidden: !log4jDisclaimerAvailable,
              style: {
                padding: '24px'
              },
              html:
                  '<div id="log4j" class="nx-log4j-disclaimer">' +
                  // TODO close btn is disabled due to some UI issues
                  ' <div class="dismiss" style="display: none"><a href="javascript:;" onclick=""><i class="fa fa-times-circle nx-log4j-close"></i></a></div>' +
                  ' <i class="fa fa-exclamation-triangle nx-log4j-warning-icon"></i>' +
                  ' <div class="nx-log4j-disclaimer-text">In response to the log4j vulnerability identified in <a href="https://ossindex.sonatype.org/vulnerability/f0ac54b6-9b81-45bb-99a4-e6cb54749f9d" target="_blank">CVE-2021-44228</a> (also known as "log4shell") impacting organizations world-wide, we are providing an experimental Log4j Visualizer capability to help our users identify log4j downloads impacted by CVE-2021-44228 so that they can mitigate the impact. Note that enabling this capability may impact Nexus Repository performance. Also note that the visualizer does not currently identify or track other log4j vulnerabilities.<div>' +
                  ' <div><a class="nx-log4j-button" href="#admin/repository/insightfrontend" style="color: white">Enable Capability</a></div> ' +
                  '</div>',
              listeners: {
                render: function(doc) {
                  doc.el.dom.getElementsByClassName('nx-log4j-button')[0].addEventListener('click', function(event) {
                    NX.direct.outreach_Outreach.setLog4JVisualizerEnabled(true);
                  });

                  // TODO this is workaround to get close button and set event to hide the log4j
                  doc.el.dom.getElementsByClassName('dismiss')[0].addEventListener('click', function(event) {
                    log4jDisclaimerAvailable = false;
                    document.getElementById('log4jDisclaimer').setAttribute('hidden', 'true');
                    // TODO set outreach.setHeight('100%');
                  });
                }
              }
            });
          }
          me.showOutreach(log4jDisclaimerAvailable);
        }
      });
    }
  }

});
