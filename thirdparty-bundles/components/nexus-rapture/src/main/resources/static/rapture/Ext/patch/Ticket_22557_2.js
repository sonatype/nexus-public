/**
 * https://support.sencha.com/index.php#ticket-22557
 */
Ext.define('Ext.patch.Ticket_22557_2', {
  override: 'Ext.grid.header.Container',
  onHeaderCtEvent: function(e, t) {
    var me = this,
      headerEl = e.getTarget('.' + Ext.grid.column.Column.prototype.baseCls),
      header,
      targetEl,
      isTriggerClick;

    if (headerEl && !me.ddLock) {
      header = Ext.getCmp(headerEl.id);
      if (header) {
        targetEl = header[header.clickTargetName];
        if (e.within(targetEl)) {
          if (e.type === 'click') {
            isTriggerClick = header.onTitleElClick(e, targetEl);
            if (isTriggerClick) {
              me.onHeaderTriggerClick(header, e, t);
            } else {
              me.onHeaderClick(header, e, t);
            }
          }
          else if (e.type === 'contextmenu') {
            me.onHeaderContextMenu(header, e, t);
          } else if (e.type === 'dblclick') {
            header.onTitleElDblClick(e, targetEl.dom);
          }
        }
      }
    }
  }
});
