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

import java.io.Serializable;

/**
 * Tracks failed download attempts and/or a manual "skip" request for a single
 * feed item (keyed by its link/location). Used to avoid endlessly re-attempting
 * downloads (notably magnet links) that repeatedly fail, and to let the user
 * permanently ignore an item.
 */
public class SkipBean implements Serializable {

  static final long serialVersionUID = 7705468017981324409L;

  private String location, name;
  private int failCount;
  private boolean manualSkip;
  private long lastFailTime;

  public SkipBean() {
  }

  public SkipBean(String location, String name) {
    this.location = location;
    this.name = name;
  }

  public String getLocation() {
    if(location == null) location = "";
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getName() {
    if(name == null) name = "";
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getFailCount() {
    return failCount;
  }

  public void setFailCount(int failCount) {
    this.failCount = failCount;
  }

  /**
   * Record a failed download attempt: bumps the failure count and stamps the
   * time of failure. Returns the new failure count.
   */
  public int recordFailure() {
    this.failCount++;
    this.lastFailTime = System.currentTimeMillis();
    return this.failCount;
  }

  public boolean isManualSkip() {
    return manualSkip;
  }

  public void setManualSkip(boolean manualSkip) {
    this.manualSkip = manualSkip;
  }

  public long getLastFailTime() {
    return lastFailTime;
  }

  public void setLastFailTime(long lastFailTime) {
    this.lastFailTime = lastFailTime;
  }

  /**
   * @param maxRetries maximum number of failed attempts before auto-skipping;
   *                   values &lt;= 0 disable auto-skipping.
   * @return true if this item should be skipped (manually flagged or the
   *         failure count has reached the configured maximum).
   */
  public boolean isSkipped(int maxRetries) {
    if(manualSkip) return true;
    return maxRetries > 0 && failCount >= maxRetries;
  }

  public String toString() {
    return getLocation();
  }
}
