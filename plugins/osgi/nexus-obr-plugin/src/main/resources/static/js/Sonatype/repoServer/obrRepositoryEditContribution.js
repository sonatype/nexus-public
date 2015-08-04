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
/*
 * Use anonymous closure to augment the current class behaviour
 */
define('Sonatype/repoServer/obrRepositoryEditContribution', function() {

  (function() {
    var originalHandler = Sonatype.repoServer.VirtualRepositoryEditor.prototype.afterProviderSelectHandler;

    Ext.override(Sonatype.repoServer.VirtualRepositoryEditor, {
      afterProviderSelectHandler : function(combo, rec, index) {

        // first invoke the original behaviour
        originalHandler.apply(this, arguments);

        // virtual OBR can be applied on top of any non-virtual, non-OBR repository
        if (rec.data.provider == 'obr-shadow') {
          this.form.findField('shadowOf').store.filterBy(function fn(rec, id) {
              return rec.data.repoType != 'virtual' && rec.data.format != 'obr';
            });}
      }
    });
  })();
});
