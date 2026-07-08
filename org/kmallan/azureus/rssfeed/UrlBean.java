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
import java.util.*;
import java.util.regex.*;
import java.net.*;

public class UrlBean implements Serializable {

  static final long serialVersionUID = -436386969314243288L;

  private String name, location, storeDir, referer, cookie;
  private long urlId;
  private boolean obeyTTL = true, locReferer = true, useCookie = false, enabled;
  private int delay = 0, prevBackLogSize;
  private List backLog;
  private List skipList;

  private transient String status = "", error = "";
  private transient boolean hitting = false, refreshNow = false;
  private transient int percent = 0, amount = 0;
  private transient ListGroup currentItems;
  private transient long lastModifed;
  private transient String lastEtag;

  public UrlBean() {
    urlId = System.currentTimeMillis();
  }

  public long getID() {
    return urlId;
  }

  public void setID(long urlId) {
    this.urlId = urlId;
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

  public String getBaseURL() {
    if(location == null || "".equals(location)) return null;
    try {
      URL u = new URL(location);
      if(!u.getProtocol().toLowerCase().startsWith("http")) return null;
      u = new URL(u.getProtocol() + "://" + u.getHost() + (u.getPort() == -1?"":":"+u.getPort()) + "/");
      Plugin.debugOut(u.toString());
      return u.toString();
    } catch(MalformedURLException e) {
      e.printStackTrace();
      return null;
    }
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getStoreDir() {
    if(storeDir == null) storeDir = "";
    return storeDir;
  }

  public void setStoreDir(String storeDir) {
    this.storeDir = storeDir;
  }

  public int getDelay() {
    if(this.delay <= 0) return 0;
    if(this.delay < Plugin.MIN_REFRESH) this.delay = Plugin.MIN_REFRESH;
    return this.delay;
  }

  public int getDelay(int delay) {
    if(this.delay <= 0) return delay;
    return getDelay();
  }

  public void setDelay(int delay) {
    this.delay = delay;
  }

  public boolean getObeyTTL() {
    Pattern snfeed = Pattern.compile(".*varchars.*");
    Matcher m = snfeed.matcher(location);
    if(m.matches()) obeyTTL = true;
    return obeyTTL;
  }

  public void setObeyTTL(boolean obeyTTL) {
    this.obeyTTL = obeyTTL;
  }

  public boolean getLocRef() {
    return locReferer;
  }

  public void setLocRef(boolean locReferer) {
    this.locReferer = locReferer;
  }

  public String getReferer() {
    if(referer == null) referer = "";
    return referer;
  }

  public void setReferer(String referer) {
    this.referer = referer;
  }

  public boolean getUseCookie() {
    return useCookie;
  }

  public void setUseCookie(boolean useCookie) {
    this.useCookie = useCookie;
  }

  public String getCookie() {
    if(cookie == null) cookie = "";
    return cookie;
  }

  public void setCookie(String cookie) {
    this.cookie = cookie;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public ListGroup getGroup() {
    return currentItems;
  }

  public ListGroup getGroup(TreeViewManager treeViewManager, int delay) {
    if(currentItems == null) newGroup(treeViewManager, delay);
    return currentItems;
  }

  public void newGroup(TreeViewManager treeViewManager, int delay) {
    if(this.currentItems != null) purgeGroup(treeViewManager);

    currentItems = new ListGroup(treeViewManager);
    currentItems.setUrl(this);
    currentItems.setDelay(getDelay(delay));
    currentItems.setElapsed(getDelay(delay) - 5);

    treeViewManager.addGroup(currentItems);
  }

  public void purgeGroup(TreeViewManager treeViewManager) {
    treeViewManager.remove(currentItems);
    currentItems = null;
  }

  public void resetGroup(int delay) {
    addToBackLog(currentItems);
    currentItems.cleanout();
    currentItems.setDelay(getDelay(delay));
    currentItems.resetElapsed();
  }

  private void addToBackLog(ListGroup oldItems) {
    if(backLog == null) backLog = new ArrayList();
    prevBackLogSize = backLog.size();
    ListBean lb;
    for(Iterator iter = oldItems.iterator(); iter.hasNext(); ) {
      lb = (ListBean)iter.next();
      if(lb == null) continue;
      if(!backLog.contains(lb)) backLog.add(lb);
    }
    cleanOldBackLog();
  }

  public void cleanOldBackLog() {
    if(backLog == null) return;
    ListBean lb;
    int keepOld = Plugin.getIntParameter("KeepOld");
    int keepMax = Plugin.getIntParameter("KeepMax");
    if(keepOld <= 0) return;
    if(keepMax < 0) keepMax = 0;
    for(Iterator iter = new ArrayList(backLog).iterator(); iter.hasNext(); ) {
      lb = (ListBean)iter.next();
      if(lb.getAge() >= keepOld) {
        backLog.remove(lb);
      }
    }
    while(backLog.size() > keepMax) backLog.remove(0);
  }

  public void refreshGroup() {
    currentItems.setElapsed(currentItems.getDelay());
    this.refreshNow = true;
  }

  public boolean getRefreshNow() {
    return this.refreshNow;
  }

  public void setHitting(boolean hitting) {
    this.hitting = hitting;
    if(hitting) {
      this.status = "Pending";
      this.percent = this.amount = 0;
      this.error = "";
      this.refreshNow = false;
    }
  }

  public boolean isHitting() {
    return hitting;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public int getAmount() {
    return amount;
  }

  public void setPercent(int percent) {
    this.percent = percent;
  }

  public int getPercent() {
    return percent;
  }

  public void setError(String err) {
    this.error = err;
  }

  public String getError() {
    if(error == null) error = "";
    return error;
  }

  public String toString() {
    return location;
  }

  public List getBackLog() {
    if(backLog == null || backLog.isEmpty()) {
      backLog = new ArrayList();
    } else {
      for(Iterator iter = currentItems.iterator(); iter.hasNext(); ) backLog.remove(iter.next());
    }
    return backLog;
  }

  public int getPrevBackLogSize() {
    return prevBackLogSize;
  }

  public void setPrevBackLogSize(int prevBackLogSize) {
    this.prevBackLogSize = prevBackLogSize;
  }

  public long getLastModifed() {
    return lastModifed;
  }

  public void setLastModifed(long lastModifed) {
    this.lastModifed = lastModifed;
  }

  public String getLastEtag() {
    return lastEtag;
  }

  public void setLastEtag(String lastEtag) {
    this.lastEtag = lastEtag;
  }

  // --- per-item skip / failed-download tracking ---------------------------

  private SkipBean findSkip(String location) {
    if(skipList == null || location == null) return null;
    for(Iterator iter = skipList.iterator(); iter.hasNext(); ) {
      SkipBean sb = (SkipBean)iter.next();
      if(sb != null && location.equalsIgnoreCase(sb.getLocation())) return sb;
    }
    return null;
  }

  public synchronized SkipBean getSkip(String location) {
    return findSkip(location);
  }

  public synchronized boolean isSkipped(String location, int maxRetries) {
    SkipBean sb = findSkip(location);
    return sb != null && sb.isSkipped(maxRetries);
  }

  public synchronized boolean isManualSkipped(String location) {
    SkipBean sb = findSkip(location);
    return sb != null && sb.isManualSkip();
  }

  /**
   * Record a failed download attempt for the given item. Returns true if the
   * item has now reached the configured retry limit and should be auto-skipped.
   */
  public synchronized boolean recordFailure(String location, String name, int maxRetries) {
    if(location == null || location.length() == 0) return false;
    if(skipList == null) skipList = new ArrayList();
    SkipBean sb = findSkip(location);
    if(sb == null) {
      sb = new SkipBean(location, name);
      skipList.add(sb);
    } else if((sb.getName() == null || sb.getName().length() == 0) && name != null) {
      sb.setName(name);
    }
    sb.recordFailure();
    return sb.isSkipped(maxRetries);
  }

  /**
   * Clear any failure/skip record for the given item (e.g. after a successful
   * download). A manual skip is left in place. Reports whether anything was
   * actually removed so callers can avoid an unnecessary config save.
   */
  public synchronized boolean clearSkipIfPresent(String location) {
    if(skipList == null || location == null) return false;
    SkipBean sb = findSkip(location);
    if(sb != null && !sb.isManualSkip()) {
      skipList.remove(sb);
      return true;
    }
    return false;
  }

  /**
   * Set or clear the manual skip flag for the given item. Clearing also resets
   * the failure count so the item gets a fresh set of retries.
   */
  public synchronized void setManualSkip(String location, String name, boolean skip) {
    if(location == null || location.length() == 0) return;
    if(skipList == null) skipList = new ArrayList();
    SkipBean sb = findSkip(location);
    if(skip) {
      if(sb == null) {
        sb = new SkipBean(location, name);
        skipList.add(sb);
      }
      sb.setManualSkip(true);
    } else if(sb != null) {
      skipList.remove(sb);
    }
  }

  public boolean equals(Object o) {
    if(this == o) return true;
    if(!(o instanceof UrlBean)) return false;
    final UrlBean urlBean = (UrlBean)o;
    if(urlId != urlBean.urlId) return false;
    return true;
  }

  public int hashCode() {
    return (int)(urlId ^ (urlId >>> 32));
  }

}
