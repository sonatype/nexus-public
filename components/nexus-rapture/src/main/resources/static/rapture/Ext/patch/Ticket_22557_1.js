/**
 * https://support.sencha.com/index.php#ticket-22557
 */
Ext.define('Ext.patch.Ticket_22557_1', {
  override : 'Ext.view.Table',

  getMaxContentWidth: function(header) {
    return this.callParent(arguments) + 4;
  }
});
