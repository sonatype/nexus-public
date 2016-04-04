/*
 Any code delivered as a result of the subject support
 incident is delivered as is and is not officially supported.
 Any such code is subject to the standard license agreement
 between the customer and Sencha.  It is the customer’s
 responsibility to implement and maintain the workaround/override.
 In addition, the customer will be responsible for removing the
 workaround or override at the proper time when the appropriate
 official release is available.

 BY USING THIS CODE YOU AGREE THAT IT IS DELIVERED "AS IS",
 WITH NO WARRANTIES WHATSOEVER, INCLUDING, BUT NOT LIMITED TO,
 IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 PURPOSE, TITLE AND NON-INFRINGEMENT.
 */

Ext.define('Ext.patch.Ticket_15227', {
  override: 'Ext.view.Table',
  getMaxContentWidth: function(header) {
    var me = this,
      cells = me.el.query(header.getCellInnerSelector()),
      originalWidth = header.getWidth(),
      i = 0,
      ln = cells.length,
      columnSizer = me.body.select(me.getColumnSizerSelector(header)),
      max = Math.max,
      widthAdjust = 0,
      maxWidth;

    if (ln > 0) {
      if (Ext.supports.ScrollWidthInlinePaddingBug) {
        widthAdjust += me.getCellPaddingAfter(cells[0]);
      }
      else {
        // add a pixel to fix Chrome
        widthAdjust += 1;
      }
      if (me.columnLines) {
        widthAdjust += Ext.fly(cells[0].parentNode).getBorderWidth('lr');
      }
    }

    // Set column width to 1px so we can detect the content width by measuring scrollWidth
    columnSizer.setWidth(1);

    // We are about to measure the offsetWidth of the textEl to determine how much
    // space the text occupies, but it will not report the correct width if the titleEl
    // has text-overflow:ellipsis.  Set text-overflow to 'clip' before proceeding to
    // ensure we get the correct measurement.
    header.titleEl.setStyle('text-overflow', 'clip');

    // Allow for padding round text of header
    maxWidth = header.textEl.dom.offsetWidth + header.titleEl.getPadding('lr');

    // revert to using text-overflow defined by the stylesheet
    header.titleEl.setStyle('text-overflow', '');

    for (; i < ln; i++) {
      maxWidth = max(maxWidth, cells[i].scrollWidth);
    }

    // in some browsers, the "after" padding is not accounted for in the scrollWidth
    maxWidth += widthAdjust;

    // 40 is the minimum column width.  TODO: should this be configurable?
    maxWidth = max(maxWidth, 40);

    // Set column width back to original width
    columnSizer.setWidth(originalWidth);

    return maxWidth;
  }
});