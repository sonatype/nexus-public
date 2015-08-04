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
/*global require*/

define('ext/data/store',['extjs'], function(Ext){
// Extension to the store to allow for multi field sorting
// from http://www.extjs.com/forum/showthread.php?t=48324
Ext.override(Ext.data.Store, {
  /**
   * Sort by multiple fields in the specified order.
   *
   * @param {Array}
        *          An Array of field sort specifications, or, if ascending sort
   *          is required on all columns, an Array of field names. A field
   *          specification looks like: { field: 'orderNumber', direction:
   *          'ASC' }
   */
  sortByFields : function(fields) {
    // Collect sort type functions,
    // Convert string field names to field+direction spec objects.
    var st = [], i, fn;
    for (i = 0; i < fields.length; i=i+1)
    {
      if (typeof fields[i] === 'string')
      {
        fields[i] = {
          field : fields[i],
          direction : 'ASC'
        };
      }
      st.push(this.fields.get(fields[i].field).sortType);
    }

    fn = function(r1, r2) {
      var result, i, v1, v2;
      for (i = 0; !result && i < fields.length; i=i+1)
      {
        v1 = st[i](r1.data[fields[i].field]);
        v2 = st[i](r2.data[fields[i].field]);
        result = (v1 > v2) ? 1 : ((v1 < v2) ? -1 : 0);
        if (fields[i].direction === 'DESC') {
          result = -result;
        }
      }
      return result;
    };
    this.data.sort('ASC', fn);
    if (this.snapshot && this.snapshot !== this.data)
    {
      this.snapshot.sort('ASC', fn);
    }
    this.fireEvent("datachanged", this);
  }
});
});
