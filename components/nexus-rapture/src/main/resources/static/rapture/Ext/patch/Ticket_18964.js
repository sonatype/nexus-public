/**
 * https://support.sencha.com/index.php#ticket-18964
 */
Ext.define('Ext.patch.Ticket_18964', {
  override: 'Ext.view.BoundList',

  getRefItems: function() {
    var me = this,
        result = [];

    if (me.pagingToolbar) {
      result.push(me.pagingToolbar);
    }
    // HACK: Disable including this, seems this value is 'true' for itemselector
    // HACK: which totally messes up component queries
    //if (me.loadMask) {
    //  result.push(me.loadMask);
    //}
    return result;
  }
});