/**
 * https://support.sencha.com/index.php#ticket-18960
 */
Ext.define('Ext.patch.Ticket_18960', {
  override: 'Ext.grid.RowEditor',

  renderColumnData: function(field, record, activeColumn) {
    var me = this,
        grid = me.editingPlugin.grid,
        headerCt = grid.headerCt,
        view = me.scrollingView,
        store = view.dataSource,
        column = activeColumn || field.column,
        value = record.get(column.dataIndex),
        renderer = column.editRenderer || column.renderer,
        scope = column.usingDefaultRenderer && !column.scope ? column : column.scope,
        metaData,
        rowIdx,
        colIdx;

    // honor our column's renderer (TemplateHeader sets renderer for us!)
    if (renderer) {
      metaData = { tdCls: '', style: '' };
      rowIdx = store.indexOf(record);
      colIdx = headerCt.getHeaderIndex(column);

      value = renderer.call(
              scope || headerCt.ownerCt,
          value,
          metaData,
          record,
          rowIdx,
          colIdx,
          store,
          view
      );
    }

    field.setRawValue(value);
    field.resetOriginalValue();
  }
});