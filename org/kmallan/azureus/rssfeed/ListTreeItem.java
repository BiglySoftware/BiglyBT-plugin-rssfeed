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

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.TreeItem;

import com.biglybt.ui.swt.Utils;

import org.eclipse.swt.SWT;

import java.util.Map;

public class ListTreeItem extends TreeItem {

  private View view = null;

  private Object data;
  private boolean isFeed, isBackLog;
  private Color defaultForeground;
  private ListTreeItem backLogItem;

  public ListTreeItem(View view) {
    super(view.listTable, SWT.NULL);
    this.view = view;
    this.isFeed = true;
    if ( !Utils.isDarkAppearanceNativeWindows()){
    	this.defaultForeground = this.getForeground();
    }
  }

  public ListTreeItem(ListTreeItem parent, View view) {
    super(parent, SWT.NULL, parent.isBackLog()?0:parent.getItemCount()); // reverse order for backlog items
    this.view = view;
    this.isFeed = false;
    if ( !Utils.isDarkAppearanceNativeWindows()){
    	this.defaultForeground = this.getForeground();
    }
  }

  public ListTreeItem(ListTreeItem parent, View view, boolean isBackLog) {
    super(parent, SWT.NULL);
    this.view = view;
    this.isFeed = true;
    this.isBackLog = isBackLog;
    if(isBackLog) parent.setBackLogItem(this);
    if ( !Utils.isDarkAppearanceNativeWindows()){
    	this.defaultForeground = this.getForeground();
    }
  }

  public boolean isFeed() {
    return isFeed;
  }

  public boolean isBackLog() {
    return isBackLog;
  }

  @Override
  public void checkSubclass() {
    return;
  }

  public Object getBean() {
    return data;
  }

  public void setBean(Object data) {
    this.data = data;

    update();
  }

  public void update() {
	  if ( isDisposed()){
		  return;
	  }
    if(isFeed) {
      UrlBean urlBean = (UrlBean)data;
      if(isBackLog) {
        setText(0, "Old/Removed");
        setText(1, this.getItemCount() + " items");
      } else {
        setText(0, urlBean.getName());
        // todo set age? (time since last refresh)
      }
    } else {
      ListBean listBean = (ListBean)data;
      setText(0, listBean.getName());
      setText(1, listBean.getLocation());
      setText(2, listBean.getAgeStr());
      setText(3, listBean.getInfo());

      if(listBean.error) this.setForeground(new Color(view.display, 255, 0, 0));
      else resetForeground();
    }
  }

  public void resetForeground() {
    this.setForeground(defaultForeground);
  }

  public void removeAll(Map allItems) {
    TreeItem[] items = this.getItems();
    for(int i = 0; i < items.length; i++) {
      allItems.remove(items[i]);
      items[i].dispose();
    }
  }

  public void setBackLogItem(ListTreeItem item) {
    if(isFeed) this.backLogItem = item;
  }

  public ListTreeItem getBackLogItem() {
    return backLogItem;
  }


}


