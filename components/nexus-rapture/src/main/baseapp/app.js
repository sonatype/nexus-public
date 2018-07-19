Ext.require([
  // Require the core ExtJS framework classes, as we can not know ahead of time what plugins may use
  // NOTE: Excluding a few bits for now until we plan to use them
  // NOTE: Exclude api is not working, we have to list all the patterns and Ext.{class} which need to be included
  'Ext.app.*',
  'Ext.button.*',
  'Ext.chart.*',
  'Ext.container.*',
  'Ext.data.*',
  'Ext.dd.*',
  'Ext.direct.*',
  'Ext.dom.*',
  'Ext.draw.*',
  //'Ext.flash.*',
  'Ext.form.*',
  //'Ext.fx.*',
  'Ext.grid.*',
  'Ext.layout.*',
  'Ext.menu.*',
  'Ext.panel.*',
  'Ext.picker.*',
  'Ext.resizer.*',
  'Ext.selection.*',
  'Ext.slider.*',
  'Ext.state.*',
  'Ext.tab.*',
  'Ext.tip.*',
  'Ext.toolbar.*',
  'Ext.tree.*',
  'Ext.util.*',
  'Ext.view.*',
  'Ext.window.*',
  'Ext.Action',
  'Ext.ux.IFrame',
  'Ext.ux.form.MultiSelect',
  'Ext.ux.form.ItemSelector',

  //
  // Require placeholder class to trigger Sencha CMD scanning of application-level scss files
  //
  'baseapp.Application'
]);
