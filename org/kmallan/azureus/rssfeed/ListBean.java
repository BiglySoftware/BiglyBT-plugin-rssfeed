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

public class ListBean implements Serializable {

  static final long serialVersionUID = -4786592909020746490L;

  public static final int DOWNLOAD_INCL = 10, DOWNLOAD_EXCL = 11, DOWNLOAD_FAIL = 12, DOWNLOAD_HIST = 13;
  public static final int NO_DOWNLOAD = 99;

  private String name, location, description;
  private long listId;
  private UrlBean urlBean;

  private String err;
  private int state = NO_DOWNLOAD;
  public boolean completed, canceled, error;

  private transient int percent, amount;
  public transient Downloader downloader = null;

  public ListBean() {
    listId = System.currentTimeMillis();
  }

  public long getID() {
    return listId;
  }

  public String getName() {
    if(name == null) name = "";
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocation() {
    if(location == null) location = "";
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getDescription() {
    if(description == null) description = "";
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public UrlBean getFeed() {
    return urlBean;
  }

  public void setFeed(UrlBean urlBean) {
    this.urlBean = urlBean;
  }

  public void resetInfo() {
    this.state = NO_DOWNLOAD;
    this.percent = 0;
    this.amount = 0;
    this.err = "";
    this.completed = this.canceled = this.error = false;
  }

  public String getInfo() {
    String info = "";

    switch(state) {
      case Downloader.DOWNLOADER_NON_INIT:
        info = "Incl";
        break;
      case Downloader.DOWNLOADER_INIT:
        info = "Init";
        break;
      case Downloader.DOWNLOADER_START:
        info = "Start";
        break;
      case Downloader.DOWNLOADER_DOWNLOADING:
        info = "Get " + (this.percent > 0?Integer.toString(this.percent) + "%":(this.amount > 0?Double.toString(Math.floor(new Integer(this.amount).doubleValue() / (double) 1024 * (double) 100) / (double) 100) + "KB":""));
        break;
      case Downloader.DOWNLOADER_FINISHED:
        this.completed = true;
        info = "Done";
        break;
      case Downloader.DOWNLOADER_ERROR:
        this.error = true;
        info = "Error" + (!"".equals(err)?" - " + err:"");
        break;
      case Downloader.DOWNLOADER_CANCELED:
        this.canceled = true;
        info = "Canned";
        break;
      case Downloader.DOWNLOADER_CHECKING:
        info = "Search " + this.amount + "/" + this.percent + " ...";
        break;
      case DOWNLOAD_INCL:
        info = "Incl";
        break;
      case DOWNLOAD_EXCL:
        this.completed = true;
        info = "Excl";
        break;
      case DOWNLOAD_FAIL:
        this.error = true;
        info = "Fail" + (!"".equals(err)?" - " + err:"");
        break;
      case DOWNLOAD_HIST:
        this.completed = true;
        info = "Hist";
        break;
    }

    return info;
  }

  public boolean checkDone() {
    return (this.completed || this.canceled || this.error);
  }

  public int getState() {
    return this.state;
  }

  public void setState(int state) {
    if(checkDone()){
    		// couple of cases where we want to pick up the new state as these can happen
    		// after the download of the torrent is complete...
    	if ( state != DOWNLOAD_FAIL && state != DOWNLOAD_EXCL ){
    	
    		return;
    	}
    }
    this.state = state;
  }

  public int getPercent() {
    return this.percent;
  }

  public void setPercent(int percent) {
    this.percent = percent;
  }

  public int getAmount() {
    return this.amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public String getError() {
    return this.err;
  }

  public void setError(String err) {
    this.err = err;
  }

  public int getAge() {
    long msAge = System.currentTimeMillis() - listId;
    return (int)(msAge / (3600000 * 24));
  }

  public String getAgeStr() {
    return makeDurationStr((System.currentTimeMillis() - listId) / 1000, false);
  }

  public boolean equals(Object o) {
    if(this == o) return true;
    if(!(o instanceof ListBean)) return false;
    final ListBean listBean = (ListBean)o;
    if(!location.equals(listBean.location)) return false;
    // if(!name.equals(listBean.name)) return false;
    return true;
  }

  public int hashCode() {
    int result;
    result = name.hashCode();
    result = 29 * result + location.hashCode();
    return result;
  }

  public String toString() {
    return name;
  }

  public static String makeDurationStr(long s, boolean detail) {
    if(s < 60) return "";
    int i = 0;
    long tmp = -1;
    String res = "";
    while(s > 0) {
      switch(i++) {
        case 0:
          tmp = s % 60;
          res += tmp + (tmp!=1?" secs":" sec");
          s = s / 60;
        case 1:
          tmp = s % 60;
          if(tmp > 0) res = tmp + (tmp!=1?" mins ":" min ") + (detail?res:"");
          s = s / 60;
        case 2:
          tmp = s % 24;
          if(tmp > 0) res = tmp + (tmp!=1?" hours ":" hour ") + (detail?res:"");
          s = s / 24;
        case 3:
          tmp = s % 7;
          if(tmp > 0) res = tmp + (tmp!=1?" days ":" day ") + res;
          s = s / 24;
        case 4:
          if(s > 0) res = s + (s!=1?" weeks ":" week ") + res;
          s = 0;
      }
    }
    return res.trim();
  }

}
