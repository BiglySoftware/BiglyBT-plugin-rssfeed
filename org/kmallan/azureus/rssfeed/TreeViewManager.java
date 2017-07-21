/*
 * RSSFeed - Azureus2 Plugin
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 */

package org.kmallan.azureus.rssfeed;

import org.eclipse.swt.custom.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;

import com.biglybt.ui.swt.mainwindow.Colors;

import java.util.*;

public class TreeViewManager {

  public List allGroups;
  private Map allItems;
  private View view = null;

  public TreeViewManager(View view) {
    this.view = view;
    this.allGroups = new ArrayList();
    this.allItems = new HashMap();
  }

  public void addGroup(ListGroup listBeans) {
    allGroups.add(listBeans);
  }

  public ListTreeItem getItem(UrlBean urlBean) {
    ListTreeItem item = (ListTreeItem)allItems.get(urlBean);
    if(item == null || item.isDisposed()) item = createFeedItem(urlBean);
    return item;
  }

  public ListTreeItem getBackLogItem(UrlBean urlBean) {
    ListTreeItem parent = getItem(urlBean);
    ListTreeItem item = parent.getBackLogItem();
    if(item == null || item.isDisposed()) item = createBackLogItem(urlBean);
    return item;
  }

  public ListTreeItem getItem(ListBean listBean) {
    ListTreeItem item = (ListTreeItem)allItems.get(listBean);
    if(item == null) Plugin.debugOut("got request for nonexisting treeitem: " + listBean);
    return item;
  }

  public void addListBean(final ListBean listBean, final UrlBean urlBean, final boolean addToBackLog) {
    if(view.isOpen() && view.display != null && !view.display.isDisposed())
      view.display.asyncExec(new Runnable() {
        @Override
        public void run() {
          try {
          if(view.listTable == null || view.listTable.isDisposed()) return;
          ListTreeItem listMainItem = addToBackLog?getBackLogItem(urlBean):getItem(urlBean);
          createItem(listMainItem, listBean);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
  }

  public void remove(final ListGroup listBeans) {
    if(view.isOpen() && view.display != null && !view.display.isDisposed())
      view.display.asyncExec(new Runnable() {
        @Override
        public void run() {
          if(view.listTable == null || view.listTable.isDisposed()) return;
          ListTreeItem item = (ListTreeItem)allItems.get(listBeans.getUrl());
          if(item != null) {
            allItems.remove(item.getBean());
            item.dispose();
          }
        }
      });
    allGroups.remove(listBeans);
  }

  public void clearGroup(final ListGroup listBeans) {
    if(view.isOpen() && view.display != null && !view.display.isDisposed())
      view.display.asyncExec(new Runnable() {
        @Override
        public void run() {
          if(view.listTable == null || view.listTable.isDisposed()) return;
          getItem(listBeans.getUrl()).removeAll(allItems);
        }
      });
  }

  private ListTreeItem createFeedItem(UrlBean urlBean) {
    ListTreeItem newItem = new ListTreeItem(view);
    newItem.setBean(urlBean);
    allItems.put(urlBean, newItem);
    return newItem;
  }

  private ListTreeItem createBackLogItem(UrlBean urlBean) {
    ListTreeItem parent = getItem(urlBean);
    ListTreeItem newItem = new ListTreeItem(parent, view, true);
    newItem.setBean(urlBean);
    return newItem;
  }

  private ListTreeItem createItem(ListTreeItem parent, ListBean listBean) {
    ListTreeItem newItem = new ListTreeItem(parent, view);
    newItem.setBean(listBean);
    allItems.put(listBean, newItem);
    if(parent.isBackLog()) parent.update();
    return newItem;
  }

  public void display() {
    ListGroup listGroup;
    ListBean listBean;
    ListTreeItem listMainItem, listBackLogItem;
    List backLog;

    view.listTable.setRedraw(false);

    for(Iterator ig = allGroups.iterator(); ig.hasNext(); ) {
      listGroup = (ListGroup)ig.next();
      if(listGroup == null || listGroup.getUrl() == null) { // todo getUrl().isDestroyed()?
        remove(listGroup);
      } else {
        listMainItem = createFeedItem(listGroup.getUrl());

        for(int i = 0; i < listGroup.size(); i++) {
          listBean = (ListBean)listGroup.get(i);
          if(listBean != null) createItem(listMainItem, listBean);
        }

        backLog = listGroup.getUrl().getBackLog();
        if(!backLog.isEmpty()) {
          listBackLogItem = createBackLogItem(listGroup.getUrl());
          for(Iterator iter = backLog.iterator(); iter.hasNext(); ) {
            listBean = (ListBean)iter.next();
            if(listBean != null) createItem(listBackLogItem, listBean);
          }
        }
      }
    }

    view.listTable.setRedraw(true);
  }

  private void setExpandAll(TableTreeItem[] items, boolean expanded) {
    for(int i = 0; i < items.length; i++) {
      items[i].setExpanded(expanded);
      if(items[i].getItemCount() > 0) setExpandAll(items[i].getItems(), expanded);
    }
  }

  public void expandAll() {
    view.listTable.setRedraw(false);
    setExpandAll(view.listTable.getItems(), true);
    view.listTable.setRedraw(true);
  }

  public void collapseAll() {
    view.listTable.setRedraw(false);
    setExpandAll(view.listTable.getItems(), false);
    view.listTable.setRedraw(true);
  }


}

