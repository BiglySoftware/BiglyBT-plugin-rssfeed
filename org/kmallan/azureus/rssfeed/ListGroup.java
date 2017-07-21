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

import java.util.*;

public class ListGroup extends ArrayList {

  private TreeViewManager treeViewManager;
  private UrlBean urlBean = null;
  private List previousItems;
  private int previousDelay;

  private int delay = 0;
  private int elapsed = 0;

  public ListGroup(TreeViewManager treeViewManager) {
    this.treeViewManager = treeViewManager;
  }

  public void setUrl(UrlBean urlBean) {
    this.urlBean = urlBean;
  }

  public UrlBean getUrl() {
    return urlBean;
  }

  public int getDelay() {
    return delay;
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  public void setDelay(int delay, boolean isTTL) {
    this.delay = delay;
    if(isTTL) this.previousDelay = delay;
  }

  public int getElapsed() {
    if(elapsed == delay) return elapsed;
    return elapsed++;
  }

  public void setElapsed(int elapsed) {
    this.elapsed = elapsed;
  }

  public void resetElapsed() {
    this.elapsed = 0;
  }

  public void cleanout() {
    previousItems = new ArrayList(this);
    clear();
    treeViewManager.clearGroup(this);
  }

  public void remove() {
    treeViewManager.remove(this);
  }

  public List getPreviousItems() {
    return previousItems;
  }

  public int getPreviousDelay() {
    return previousDelay;
  }

  public void setPreviousDelay(int previousDelay) {
    this.previousDelay = previousDelay;
  }
}
