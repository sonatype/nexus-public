// Fixes bug where hover on a submenu causes it to disappear
//
// NOTE: This is a bug specifically in Chrome v43. Once v44 is released, check to see if this is fixed.
// https://www.sencha.com/forum/showthread.php?301116-Submenus-disappear-in-Chrome-43-beta
//
Ext.define('Ext.patch.Ticket_17866', {
  override: 'Ext.menu.Menu',
  onMouseLeave: function(e) {
    var me = this;


    // BEGIN FIX
    var visibleSubmenu = false;
    me.items.each(function(item) {
      if(item.menu && item.menu.isVisible()) {
        visibleSubmenu = true;
      }
    });
    if(visibleSubmenu) {
      //console.log('apply fix hide submenu');
      return;
    }
    // END FIX


    me.deactivateActiveItem();


    if (me.disabled) {
      return;
    }


    me.fireEvent('mouseleave', me, e);
  }
});