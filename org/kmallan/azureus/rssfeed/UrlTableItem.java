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

import org.eclipse.swt.widgets.*;
import org.eclipse.swt.SWT;

public class UrlTableItem extends TableItem {

  private Table parent;
  private Config config;
  private UrlBean data;

  public UrlTableItem(Table parent, Config config) {
    super(parent, SWT.NULL);
    this.parent = parent;
    this.config = config;
  }

  @Override
  public void checkSubclass() {
    return;
  }

  public UrlBean getBean() {
    return data;
  }

  public void setBean(int index) {
    this.data = config.getUrl(index);

    update();
  }

  public void setBean(UrlBean data) {
    if(this.data == null) config.addUrl(data);
    else config.setUrl(config.getUrlIndex(this.data), data);
    this.data = data;

    update();
  }

  public void setup(View view) {
    view.urlName.setText(data.getName());
    view.urlLocation.setText(data.getLocation());
    view.urlStoreDir.setText(data.getStoreDir());
    view.urlDelay.setText(Integer.toString(data.getDelay()));
    view.urlObeyTTL.setSelection(data.getObeyTTL());
    view.urlLocRef.setSelection(data.getLocRef());
    view.urlReferer.setText(data.getReferer());
    view.urlUseCookie.setSelection(data.getUseCookie());
    view.urlCookie.setText(data.getCookie());
    view.urlEnabled.setSelection(data.isEnabled());

    view.urlParamShow();
  }

  public void save(View view) {
    data.setName(view.urlName.getText());
    data.setLocation(view.urlLocation.getText());
    data.setStoreDir(view.urlStoreDir.getText());
    data.setDelay(Integer.parseInt(view.urlDelay.getText()));
    data.setObeyTTL(view.urlObeyTTL.getSelection());
    data.setLocRef(view.urlLocRef.getSelection());
    data.setReferer(view.urlReferer.getText());
    data.setUseCookie(view.urlUseCookie.getSelection());
    data.setCookie(view.urlCookie.getText());
    data.setEnabled(view.urlEnabled.getSelection());

    view.urlDelay.setText(String.valueOf(data.getDelay()));
    update();
  }

  public void update() {
    setChecked(data.isEnabled());
    setText(0, data.getName());
  }

  public void remove() {
    config.removeUrl(data);
    parent.remove(parent.indexOf(this));
    data.getGroup().remove();
  }
}
