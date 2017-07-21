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


import java.io.*;
import java.util.*;
import java.net.*;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;

public class Config implements Serializable {

  private List urlBeans, filterBeans, histBeans;
  public String path, separator;

  public Config() {
    this.urlBeans = new ArrayList();
    this.filterBeans = new ArrayList();
    this.histBeans = new ArrayList();

    setPath(Plugin.getPluginDirectoryName());
    try {
      loadOptions();
    } catch (Exception e) {
    }
  }

  public String getPath() {
    return path;
  }

  private void setPath(String newPath) {
    this.path = newPath;
    separator = System.getProperty("file.separator");
    if(!path.endsWith(separator)) path = path + separator;
  }

  public synchronized void storeOptions() {
    File optionsFile = new File(getPath() + "rssfeed.options");
    Plugin.debugOut("storing options to file: " + optionsFile.getPath());
    try {
      if(!optionsFile.exists()) optionsFile.createNewFile();
      storeObjectFile(optionsFile);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public synchronized void loadOptions() {
    File optionsFile = new File(getPath() + "rssfeed.options");
    Plugin.debugOut("loading options from file: " + optionsFile.getPath());
    try {
      if(optionsFile.exists()) {
        loadObjectFile(optionsFile);
      } else {
        optionsFile.createNewFile();
        storeObjectFile(optionsFile);
      }
    } catch(OptionalDataException e) {
      Plugin.debugOut("found old file format, attempting import... (" + e + ")");
      try {
        loadLegacyFile(optionsFile);
        optionsFile.renameTo(new File(getPath() + "rssfeed.options.bak"));
        storeOptions();
      } catch(Exception e1) {
        e1.printStackTrace();
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  private void loadLegacyFile(File file) throws IOException, ClassNotFoundException {
    KMAllanInputStream kis = new KMAllanInputStream(new FileInputStream(file));
    urlBeans = new ArrayList(kis.readVector("UrlBean"));
    filterBeans = new ArrayList(kis.readVector("FilterBean"));
    histBeans = new ArrayList(kis.readVector("HistBean"));
    kis.close();
  }

  private void storeObjectFile(File file) throws IOException {
    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
    oos.writeObject(validateBeans(urlBeans));
    oos.writeObject(validateBeans(filterBeans));
    oos.writeObject(validateBeans(histBeans));
    oos.close();
  }

  protected static class
  JSONWrapper
  {
	  private List		urlBeans;
	  private List		filterBeans;
	  private List		historyBeans;
  }
  
  protected File 
  exportToJSON()
  
  	throws Exception
  {
	  	File file = new File(getPath() + "rssfeed.options.json");
	  	
		FileOutputStream fos = new FileOutputStream( file  );
		
		try{
			Map<String,Object> args = new HashMap<String, Object>();
			
			args.put( JsonWriter.PRETTY_PRINT, true );
			
			JsonWriter writer = new JsonWriter( fos, args  );
			
			JSONWrapper wrapper = new JSONWrapper();
			
			wrapper.urlBeans 		= validateBeans(urlBeans);
			wrapper.filterBeans 	= validateBeans(filterBeans);
			wrapper.historyBeans 	= validateBeans(histBeans);
			
			writer.write( wrapper );
			writer.close();
			
			return( file );
			
		}finally{
			
			fos.close();
		}
  }
  
  protected void 
  importFromJSON()
		
  	throws Exception
  {
	  	File file_backup = new File(getPath() + "rssfeed.options.bak");

	    if (!file_backup.exists()){
	    	
	    	file_backup.createNewFile();
	    }
	      
	    storeObjectFile(file_backup);
	    
	  	File file = new File(getPath() + "rssfeed.options.json");

		FileInputStream fis = new FileInputStream( file  );
		
		try{						
			JsonReader reader = new JsonReader( fis );
			
			JSONWrapper wrapper = (JSONWrapper)reader.readObject();
			
			urlBeans 		= validateBeans(wrapper.urlBeans);
			filterBeans 	= validateBeans(wrapper.filterBeans);
			histBeans 	= validateBeans(wrapper.historyBeans);
		
			Collections.sort(histBeans);
			
			reader.close();
			
			storeOptions();
			
		}finally{
			
			fis.close();
		}
  }
  
  private void loadObjectFile(File file) throws IOException, ClassNotFoundException {
    ObjectInputStream ois = null;
    try {
      ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
      urlBeans = validateBeans((List)ois.readObject());
      filterBeans = validateBeans((List)ois.readObject());
      histBeans = validateBeans((List)ois.readObject());
      Collections.sort(histBeans);
      ois.close();
    } catch(IOException e) {
      throw e;
    } finally {
      if(ois != null) ois.close();
    }
  }

  private List validateBeans(List beans) {
    for(int i = (beans.size() - 1); i >= 0; i--) {
      if(beans.get(i) == null) beans.remove(i);
      else if(beans.get(i) instanceof UrlBean) ((UrlBean)beans.get(i)).cleanOldBackLog();
    }
    return beans;
  }

  public UrlBean getUrl(int index) {
    if(index < 0 || index >= getUrlCount()) {
      UrlBean urlBean = new UrlBean();
      urlBeans.add(urlBean);
      return urlBean;
    }
    return (UrlBean)urlBeans.get(index);
  }

  public void addUrl(UrlBean urlBean) {
    urlBeans.add(urlBean);
  }

  public void setUrl(int index, UrlBean urlBean) {
    if(index >= 0 && index < getUrlCount()) urlBeans.set(index, urlBean);
    else urlBeans.add(urlBean);
  }

  public int getUrlCount() {
    return urlBeans.size();
  }

  public int getUrlIndex(UrlBean urlBean) {
    return urlBeans.indexOf(urlBean);
  }

  public void removeUrl(UrlBean urlBean) {
    urlBeans.remove(urlBean);
  }

  public FilterBean getFilter(int index) {
    if(index < 0 || index >= getFilterCount()) {
      FilterBean filterBean = new FilterBean();
      filterBeans.add(filterBean);
      return filterBean;
    }
    return (FilterBean)filterBeans.get(index);
  }

  public void setFilter(FilterBean filterBean) {
    filterBeans.add(filterBean);
  }

  public void setFilter(int index, FilterBean filterBean) {
    if(index >= 0 && index < getFilterCount()) filterBeans.set(index, filterBean);
    else filterBeans.add(filterBean);
  }

  public int getFilterCount() {
    return filterBeans.size();
  }

  public int getFilterIndex(FilterBean filterBean) {
    return filterBeans.indexOf(filterBean);
  }

  public void removeFilter(FilterBean filterBean) {
    filterBeans.remove(filterBean);
  }

  public HistoryBean getHistory(int index) {
    if(index < 0 || index >= getHistoryCount()) {
      HistoryBean histBean = new HistoryBean();
      histBeans.add(histBean);
      return histBean;
    }
    return (HistoryBean)histBeans.get(index);
  }

  public void addHistory(HistoryBean histBean) {
    histBeans.add(0, histBean);
  }

  public void setHistory(int index, HistoryBean histBean) {
    if(index >= 0 && index < getHistoryCount()) histBeans.set(index, histBean);
    else histBeans.add(histBean);
  }

  public int getHistoryCount() {
    return histBeans.size();
  }

  public int getHistoryIndex(HistoryBean histBean) {
    return histBeans.indexOf(histBean);
  }

  public void removeHistory(HistoryBean histBean) {
    histBeans.remove(histBean);
  }

  public static void main(String[] args) throws Exception {
    Socket s = new ServerSocket(5000).accept();
    InputStream is = s.getInputStream();
    while(true) is.read();
  }

}
