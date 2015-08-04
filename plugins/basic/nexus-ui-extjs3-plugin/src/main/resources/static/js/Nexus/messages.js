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
define('Nexus/messages', ['extjs', 'nexus'], function() {
    /**
     * Helper to show transient messages to users.
     *
     * This is based on the Ext.examples.msg helper in the 2.3 sources.
     */
    Nexus.messages = function () {
        var msgCt;

        // FIXME: Sort out a better way to do this so we can include icons and other muck if needed

        function createBox(t, s) {
            return [
                '<div class="nx-msg">',
                '<div class="x-box-tl"><div class="x-box-tr"><div class="x-box-tc"></div></div></div>',
                '<div class="x-box-ml"><div class="x-box-mr"><div class="x-box-mc"><h3>', t, '</h3>', s, '</div></div></div>',
                '<div class="x-box-bl"><div class="x-box-br"><div class="x-box-bc"></div></div></div>',
                '</div>'
            ].join('');
        }

        return {
            show: function (title, format) {
                var s, m;

                if (!msgCt) {
                    msgCt = Ext.DomHelper.insertFirst(document.body, {id: 'nx-msg-div'}, true);
                }
                msgCt.alignTo(document, 't-t');
                s = String.format.apply(String, Array.prototype.slice.call(arguments, 1));
                m = Ext.DomHelper.append(msgCt, {html: createBox(title, s)}, true);
                m.slideIn('t').pause(2).ghost("t", {remove: true});
            }
        };
    }();
});