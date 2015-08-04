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
/*global NX, Ext, Nexus*/

/**
 * A grid filter box that will also search tag keys for matches.
 * A tag will be a match if matches teh search string and tag has a value.
 *
 * @since 2.7
 */
NX.define('Nexus.capabilities.CapabilitiesGridFilterBox', {
  extend: 'Nexus.ext.GridFilterBox',

  mixins: [
    'Nexus.capabilities.CapabilitiesMediatorMixin'
  ],

  /**
   * @override
   */
  matches: function (filterValue, record, fieldName, fieldValue) {
    var self = this;
    return (fieldValue && self.filteredStore.getTagKeyFrom(fieldName) && self.filterFn(fieldName, filterValue) )
        || Nexus.capabilities.CapabilitiesGridFilterBox.superclass.matches(filterValue, record, fieldName, fieldValue)
        || self.filterFn(self.mediator().getStatusLabel(record.data), filterValue);
  }

});