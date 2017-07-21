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
import com.biglybt.pif.download.Download;
import com.biglybt.core.util.Constants;

public class FilterTableItem extends TableItem {

  private Table parent;
  private Config config;
  private FilterBean data;

  public FilterTableItem(Table parent, Config config) {
    super(parent, SWT.NULL);
    this.parent = parent;
    this.config = config;
  }

  @Override
  public void checkSubclass() {
    return;
  }

  public FilterBean getBean() {
    return data;
  }

  public void setBean(int index) {
    this.data = config.getFilter(index);

    update();
  }

  public void setBean(FilterBean data) {
    if(this.data == null) config.setFilter(data);
    else config.setFilter(config.getFilterIndex(this.data), data);
    this.data = data;

    update();
  }

  public void setup(View view) {
    view.filtName.setText(data.getName());
    view.filtStoreDir.setText(data.getStoreDir());
    view.filtExpression.setText(data.getExpression());
    view.filtIsRegex.setSelection(data.getIsRegex());
    view.filtIsFilename.setSelection(data.getIsFilename());
    view.filtMatchTitle.setSelection(data.getMatchTitle());
    view.filtMatchLink.setSelection(data.getMatchLink());
    view.filtMoveTop.setSelection(data.getMoveTop());
    view.filtState.select(data.getState());

    view.filtRateUseCustom.setSelection(data.getRateUseCustom());
    view.filtRateUpload.setText(Integer.toString(data.getRateUpload()));
    view.filtRateDownload.setText(Integer.toString(data.getRateDownload()));
    view.filtCategory.setText(data.getCategory());
    view.filtExclude.setText(data.getExclude());
    view.filtFeed.removeAll();
    long curFeed = data.getFeed();
    view.filtFeed.add("All");
    view.filtFeed.select(0);
    for(int i = 0; i < config.getUrlCount(); i++) {
      UrlBean urlBean = config.getUrl(i);
      view.filtFeed.add(urlBean.getName());
      if(curFeed == urlBean.getID()) {
        view.filtFeed.select(i + 1);
      }
    }
    if(data.getType().equalsIgnoreCase("")) {
      view.filtType.select(0);
    } else
      for(int i = 0; i < view.filtType.getItemCount(); i++) {
        if(data.getType().equalsIgnoreCase(view.filtType.getItem(i))) {
          view.filtType.select(i);
        }
      }
    switch(view.filtType.getSelectionIndex()) {
      case 0:
        view.filtStartSeason.setText(Integer.toString(data.getStartSeason()));
        view.filtStartEpisode.setText(Integer.toString(data.getStartEpisode()));
        view.filtEndSeason.setText(Integer.toString(data.getEndSeason()));
        view.filtEndEpisode.setText(Integer.toString(data.getEndEpisode()));
        view.filtSmartHist.setSelection(data.getUseSmartHistory());
      case 1:
        view.filtDisable.setSelection(data.getDisableAfter());
        break;
    }
    view.filtEnabled.setSelection(data.getEnabled());
    if(data.getMode().equalsIgnoreCase("")) {
      view.filtMode.select(0);
    } else
      for(int i = 0; i < view.filtMode.getItemCount(); i++) {
        if(data.getMode().equalsIgnoreCase(view.filtMode.getItem(i))) {
          view.filtMode.select(i);
        }
      }

    view.setMinTorrentSize( data.getMinTorrentSize());
    view.setMaxTorrentSize( data.getMaxTorrentSize());
    view.filtParamShow();
  }

  public void save(View view) {
    save(this, view);

    update();
  }

  public static FilterBean save(FilterTableItem item, View view) {
    FilterBean data = item == null?new FilterBean():item.getBean();

    data.setName(view.filtName.getText());
    data.setStoreDir(view.filtStoreDir.getText());
    data.setExpression(view.filtExpression.getText());
    data.setIsRegex(view.filtIsRegex.getSelection());
    data.setIsFilename(view.filtIsFilename.getSelection());
    data.setMatchTitle(view.filtMatchTitle.getSelection());
    data.setMatchLink(view.filtMatchLink.getSelection());
    data.setMoveTop(view.filtMoveTop.getSelection());
    data.setState(view.filtState.getSelectionIndex());
 
    data.setRateUseCustom(view.filtRateUseCustom.getSelection());
    data.setRateUpload(Integer.parseInt(view.filtRateUpload.getText()));
    data.setRateDownload(Integer.parseInt(view.filtRateDownload.getText()));
    data.setCategory(view.filtCategory.getText());
    data.setExclude(view.filtExclude.getText());
    if(view.filtFeed.getSelectionIndex() > 0 && item != null) {
      UrlBean urlBean = item.config.getUrl(view.filtFeed.getSelectionIndex() - 1);
      data.setFeed(urlBean.getID());
    } else {
      data.setFeed(0);
    }
    data.setType(view.filtType.getText());
    switch(view.filtType.getSelectionIndex()) {
      case 0:
        data.setStartSeason(Integer.parseInt(view.filtStartSeason.getText()));
        data.setStartEpisode(Integer.parseInt(view.filtStartEpisode.getText()));
        data.setEndSeason(Integer.parseInt(view.filtEndSeason.getText()));
        data.setEndEpisode(Integer.parseInt(view.filtEndEpisode.getText()));
        data.setUseSmartHistory(view.filtSmartHist.getSelection());
      case 1:
        data.setDisableAfter(view.filtDisable.getSelection());
        break;
    }
    data.setEnabled(view.filtEnabled.getSelection());
    data.setMode(view.filtMode.getText());
    data.setMinTorrentSize(view.getMinTorrentSize());
    data.setMaxTorrentSize(view.getMaxTorrentSize());
    return data;
  }

  public void update() {
    setChecked(data.getEnabled());
    setText(0, data.getName());
    setText(1, data.getType());
    setText(2, data.getMode());
  }

  public void remove() {
    config.removeFilter(data);
    parent.remove(parent.indexOf(this));
  }
}
