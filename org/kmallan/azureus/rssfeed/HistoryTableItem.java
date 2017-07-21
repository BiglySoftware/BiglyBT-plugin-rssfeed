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

import java.text.DateFormat;
import java.util.Date;

public class HistoryTableItem extends TableItem {

  private Table parent;
  private Config config;
  private HistoryBean data;

  public HistoryTableItem(Table parent, Config config, int index) {
    super(parent, SWT.NULL, index);
    this.parent = parent;
    this.config = config;
  }

  @Override
  public void checkSubclass() {
    return;
  }

  public HistoryBean getBean() {
    return data;
  }

  public void setBean(int index) {
    this.data = config.getHistory(index);

    update();
  }

  /*
  public void setBean(HistoryBean data) {
    if(this.data == null) config.addHistory(data);
    else config.setHistory(config.getHistoryIndex(this.data), data);
    this.data = data;

    update();
  }
  */

  public void update() {
    setText(0, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(data.getID())));
    setText(1, data.getFileData());
    setText(2, data.getLocation());

    StringBuilder output = new StringBuilder();
    if(data.getFiltID() != 0) {
      output.append("Filter Matched");
      if(data.getFiltName() != null) output.append(String.format(": '%s'", data.getFiltName()));
      output.append(" Type: ").append(data.getFiltType());
      if ("TVShow".equalsIgnoreCase(data.getFiltType())) {
        if(data.getSeasonStart() >= 0) {
          output.append(String.format(" - %s Ep %dx%d", data.getTitle(), data.getSeasonStart(), data.getEpisodeStart()));
          if(data.getSeasonEnd() > data.getSeasonStart())
            output.append(String.format("-%dx%d", data.getSeasonEnd(), data.getEpisodeEnd()));
          else if(data.getEpisodeEnd() > data.getEpisodeStart())
            output.append("-").append(data.getEpisodeEnd());
        }
      } else if ("Movie".equalsIgnoreCase(data.getFiltType())) {
        output.append(String.format(" - %s (%d)", data.getTitle(), data.getYear()));
      }
    } else {
      output.append("Manual Download");
    }
    setText(3, output.toString());
  }

  public void remove() {
    config.removeHistory(data);
    parent.remove(parent.indexOf(this));
  }


}
