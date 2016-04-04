/**
 * https://support.sencha.com/index.php#ticket-21425
 */
Ext.define('Ext.patch.Ticket_21425', {
  override: 'Ext.util.History',

  getHash: function() {
    // HACK: Firefox decodes the hash when accessed directly, so we need to use
    // location.href to get it instead
    // return this.win.location.hash.substr(1);
    return location.href.split('#').splice(1).join('#');
  }
});
