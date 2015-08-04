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
/*global define*/

define('Nexus/util/observable', ['extjs', 'nexus'], function(Ext, Nexus) {
  Ext.namespace('Nexus.util');
  Nexus.util.Observable = function() {
    this.addEvents({
      /*
       * Fired when the main Nexus navigation panel is being built.
       * Subscribers can use this event to add items to the navigation panel.
       * A Sonatype.navigation.NavigationPanel instance is passed as a
       * parameter. The subscriber can use the "add" method on it to append
       * new sections or individual items to existing sections (using a
       * "sectionId" config property). init: function() {
       * Sonatype.Events.addListener( 'nexusNavigationInit', this.naviHandler,
       * this ); }, naviHandler: function( navigationPanel ) { // add a new
       * section with some items navigationPanel.add( { title: 'My Section',
       * id: 'my-nexus-section', items: [ { title: 'Open New Tab', tabId:
       * 'my-unique-tab-id', tabCode: My.package.ClassName, // JavaScript
       * class implementing Ext.Panel, tabTitle: 'My Tab' // optional tab
       * title (if different from the link title) }, { title: 'Open Another
       * Tab', tabId: 'my-second-tab-id', tabCode: My.package.AnotherClass,
       * enabled: this.isSecondTabEnabled() // condition to show the link or
       * not }, { title: 'Pop-up Dialog', handler: this.popupHandler, // click
       * handler scope: this // handler execution scope } ] } ); // add a link
       * to an existing section navigationPanel.add( { sectionId:
       * 'st-nexus-docs', title: 'Download Nexus', href:
       * 'http://nexus.sonatype.org/using/download.html' } ); }, See
       * Sonatype.repoServer.RepoServer.addNexusNavigationItems() for more
       * examples.
       */
      'nexusNavigationInit' : true,

      /*
       * Fired when a repository context action menu is initialized.
       * Subscribers can add action items to the menu. A menu object and
       * repository record are passed as parameters. If clicked, the action
       * handler receives a repository record as parameter. init: function() {
       * Sonatype.Events.addListener( 'repositoryMenuInit', this.onRepoMenu,
       * this ); }, onRepoMenu: function( menu, repoRecord ) { if (
       * repoRecord.get( 'repoType' ) == 'proxy' ) { menu.add(
       * this.actions.myProxyAction ); } },
       */
      'repositoryMenuInit' : true,

      /*
       * Fired when an repository item (e.g. artifact or folder) context
       * action menu is initialized. Subscribers can add action items to the
       * menu. A menu object, a repository and item records are passed as
       * parameters. If clicked, the action handler receives an item record as
       * parameter. init: function() { Sonatype.Events.addListener(
       * 'repositoryContentMenuInit', this.onArtifactMenu, this ); },
       * onRepoMenu: function( menu, repoRecord, contentRecord ) { if (
       * repoRecord.get( 'repoType' ) == 'proxy' ) { menu.add(
       * this.actions.myProxyContentAction ); } },
       */
      'repositoryContentMenuInit' : true,

      /*
       * Fired when a user action menu is initialized (most likely an admin
       * function). Subscribers can add action items to the menu. A menu
       * object and a user record are passed as parameters. If clicked, the
       * action handler receives an user record as parameter. init: function() {
       * Sonatype.Events.addListener( 'userMenuInit', this.onUserMenu, this ); },
       * onUserMenu: function( menu, userRecord ) { if ( userRecord.get(
       * 'userId' ) != 'anonymous' ) { menu.add( this.actions.myUserAction ); } },
       */
      'userMenuInit' : true,

      /*
       * Fired when a privilege formPanel initializes Subscribers can mangle
       * the PrivilegeEditor as they see fit init: function() {
       * Sonatype.Events.addListener( 'privilegeEditorInit',
       * this.onPrivilegeEditorInit, this ); }, onPrivilegeEditorInit:
       * function( editor ) { editor.mangle(); },
       */
      'privilegeEditorInit' : true,

      /*
       * Fired when a privilege panel initializes Subscribers can mangle the
       * PrivilegePanel as they see fit init: function() {
       * Sonatype.Events.addListener( 'privilegePanelInit',
       * this.onPrivilegePanelInit, this ); }, onPrivilegePanelInit: function(
       * panel ) { panel.mangle(); },
       */
      'privilegePanelInit' : true
    });
  };
  Ext.extend(Nexus.util.Observable, Ext.util.Observable);
  return Nexus;
});

