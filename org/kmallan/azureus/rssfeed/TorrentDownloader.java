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

import com.biglybt.core.category.CategoryManager;
import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.Debug;
import com.biglybt.core.util.FileUtil;
import com.biglybt.pif.download.*;
import com.biglybt.pif.torrent.*;
import com.biglybt.pifimpl.local.PluginCoreUtils;

import com.biglybt.core.tag.Tag;
import com.biglybt.core.tag.TagManagerFactory;
import com.biglybt.core.tag.TagType;

import javax.swing.text.html.parser.ParserDelegator;

import java.io.*;
import java.util.*;

public class TorrentDownloader {

  private View view;
  private TorrentManager torrentManager;
  private DownloadManager downloadManager;

  public TorrentDownloader(View view, TorrentManager torrentManager, DownloadManager downloadManager) {
    this.view = view;
    this.torrentManager = torrentManager;
    this.downloadManager = downloadManager;
  }

  public boolean addTorrent(ListBean listBean) {
    return addTorrent(null, listBean);
  }

  public void addTorrentThreaded(final ListBean listBean) {
    new Thread("TorrentDownloaderThread") {
      @Override
      public void run() {
        addTorrent(listBean);
        if(view.isOpen() && view.display != null && !view.display.isDisposed())
          view.display.asyncExec(new Runnable() {
            @Override
            public void run() {
              if(view.listTable == null || view.listTable.isDisposed()) return;
              ListTreeItem listItem = view.treeViewManager.getItem(listBean);
              listItem.update();
            }
          });
      }
    }.start();
  }


  public boolean addTorrent(FilterBean filterBean, ListBean listBean) {
    return addTorrent(listBean.getLocation(), listBean.getFeed(), filterBean, listBean);
  }

  public boolean addTorrent(String link, final UrlBean urlBean, final FilterBean filterBean, final ListBean listBean) {
    boolean ret = true;
    String err = "";
    File torrentLocation = null;

    try {
      boolean saveTorrents = false;
      String torrentDirectory = "";
      try {
        saveTorrents = COConfigurationManager.getBooleanParameter("Save Torrent Files", true);
        torrentDirectory = COConfigurationManager.getDirectoryParameter("General_sDefaultTorrent_Directory");
      } catch(Exception egnore) {}

      if(saveTorrents && torrentDirectory != null && torrentDirectory.length() > 0) {
        torrentLocation = getTorrent(link, urlBean, listBean, torrentDirectory);
        if(torrentLocation == null) return false;
        Torrent curTorrent = torrentManager.createFromBEncodedFile(torrentLocation);

        if ( filterBean != null ){
	        long	minSize = filterBean.getMinTorrentSize();
	        long	maxSize = filterBean.getMaxTorrentSize();
	        long	torrentSize = curTorrent.getSize();
	        
	        if ( minSize != 0 && minSize > torrentSize ){
	            listBean.setState(ListBean.DOWNLOAD_EXCL);
	            ret = false;
	        }
	        if ( maxSize != 0 && maxSize < torrentSize ){
	            listBean.setState(ListBean.DOWNLOAD_EXCL);
	            ret = false;
	        }
        }
        if ( ret ){
	        String storeFile = null;
	        if((curTorrent.getFiles()).length == 1) storeFile = curTorrent.getName();
	
	        String defaultPath = "";
	        if(filterBean != null && filterBean.getStoreDir().length() > 0) {
	          defaultPath = filterBean.getStoreDir();
	        } else if(urlBean != null && urlBean.getStoreDir().length() > 0) {
	          defaultPath = urlBean.getStoreDir();
	        } else{
	          defaultPath = COConfigurationManager.getStringParameter("Default save path", "");
	        }
	
	        if(defaultPath.length() > 0) {
	          File dataLocation = setFile(defaultPath, storeFile);
	
	          DownloadWillBeAddedListener dwba =
	        	new DownloadWillBeAddedListener()
	          	{
	        	  @Override
		          public void
	        	  initialised(
	        			Download download) 
	        	  {
	        		  	// handle tags here so that any initial-download-location is taken account of
	        		  
	  	            if ( filterBean != null ){
	  	            	
	  	            	String category_or_tag_name = filterBean.getCategory();
	  	            	
						if ( category_or_tag_name.length() > 0 ){
							
							if ( CategoryManager.getCategory( category_or_tag_name ) == null ){
							
								category_or_tag_name = category_or_tag_name.replace( ';', ',' );
								
								String[] tags = category_or_tag_name.split( "," );
								
								TagType tt_cat	= TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_CATEGORY );
								TagType tt_dm	= TagManagerFactory.getTagManager().getTagType( TagType.TT_DOWNLOAD_MANUAL );

								for ( String tag_name: tags ){
									
									tag_name = tag_name.trim();
											
									if ( tag_name.length() == 0 ){
										
										continue;
									}
										
									Tag tag = tt_cat.getTag( tag_name, true );
									
									if ( tag != null ){
										
											// if a category exists with this name then use that
										
										tag.addTaggable( PluginCoreUtils.unwrap( download ));
										
									}else{
										
											// otherwise use existing/create manual tag
										
										tag = tt_dm.getTag( tag_name, true );
										
										if ( tag == null ){
											
											try{
												tag = tt_dm.createTag( tag_name, true );
												
											}catch( Throwable e ){
												
												Debug.out( e );
											}
										}
		
										if ( tag != null ){
											
											tag.addTaggable( PluginCoreUtils.unwrap( download ));
										}
									}
								}
							}
						}
	  	            }
	          	  }
	          	};
	          	
	          final Download download = addTorrent(curTorrent, torrentLocation, dataLocation, dwba );
	          ret = (download != null);
	          Plugin.debugOut("ret: " + ret + " download: " + download);
	
	          if(ret) {
	            if(filterBean != null) {
	              view.histAdd(listBean, download, dataLocation, filterBean);
	              try {
									if (filterBean.getMoveTop()) {
										download.moveTo(0);
									}
	
									if (filterBean.getRateUseCustom()) {
										download.setUploadRateLimitBytesPerSecond(filterBean.getRateUpload() * 1024);
									}

									switch (filterBean.getState()) {
										case 1:
											download.setForceStart(true);
											break;
										case 2:
											download.stop();
											break;
									}
									if (filterBean.getRateUseCustom()){
										download.setMaximumDownloadKBPerSecond(filterBean.getRateDownload());
									}
									
										// don't need to handle tags here as they are handled above in the 
										// will-be-added listener
									
									String category_or_tag_name = filterBean.getCategory();
									if (category_or_tag_name.length() > 0){
										if ( CategoryManager.getCategory( category_or_tag_name ) != null ){
										
											download.setCategory( category_or_tag_name );
										}
									}
								} catch (NoSuchMethodError e) {
									/** < Azureus 2.1.0.5 **/
								} catch (Exception e) {
								}
	            } else {
	              if(!Plugin.getBooleanParameter("AutoStartManual")){
	            	  int state = download.getState();
	            	  if ( state != Download.ST_STOPPED && state != Download.ST_STOPPING && state != Download.ST_ERROR ){
	            		  download.stop();
	            	  }
	              }
	              view.histAdd(listBean, download, dataLocation);
	            }
	          }
	        } else {
	          ret = false;
	          err = "No Default Data Directory Set (Options > Files > Save to default data directory)";
	        }
        }
      } else {
        ret = false;
        err = "No Torrent Save Directory Set (Options > Files > Torrents > Save .torrent files)";
      }
    } catch(Exception e) {
      e.printStackTrace();
      System.err.println("Failed to add torrent: '" + link + "'");
      err = e.getMessage();
      ret = false;
    }
    if(!ret) {
    	if ( listBean.getState() != ListBean.DOWNLOAD_EXCL ){
    		listBean.setState(ListBean.DOWNLOAD_FAIL);
    	}
      if(!"".equals(err)) listBean.setError(err);
    }

    if(!ret && torrentLocation != null) torrentLocation.delete();
    return ret;
  }

  public boolean addTorrent(Torrent curTorrent, File torrentLocation, ListBean listBean, String storeLoc, String storeFile, DownloadWillBeAddedListener dwba ) throws Exception {
    File dataLocation = null;
    if(storeLoc != null && storeLoc.length() > 0) {
      dataLocation = setFile(storeLoc, storeFile);
    } else if(listBean != null && (listBean.getFeed()).getStoreDir().length() > 0) {
      dataLocation = setFile((listBean.getFeed()).getStoreDir(), null);
    }
    Download download = addTorrent(curTorrent, torrentLocation, dataLocation,dwba);
    if(download != null) view.histAdd(listBean, download, dataLocation);
    return (download != null);
  }

  private Download addTorrent(final Torrent curTorrent, File torrentLocation, File dataLocation, final DownloadWillBeAddedListener external_dwba) throws Exception {
    Download download = null;
    if(torrentLocation != null && dataLocation != null) {
    	
		DownloadWillBeAddedListener dwba = 
			new DownloadWillBeAddedListener() 
			{	
				@Override
				public void
				initialised(
					Download download) 
				{
					if ( Arrays.equals( download.getTorrent().getHash(), curTorrent.getHash())){
						
						String[] networks = view.getPlugin().getForcedNetworks();
						
						if ( networks != null ){
						
							PluginCoreUtils.unwrap( download ).getDownloadState().setNetworks( networks );
						}
						
						if ( external_dwba != null ){
							
							external_dwba.initialised( download );
						}
					}
				}
			};
     		    	
		try{
    		downloadManager.addDownloadWillBeAddedListener(dwba);
    		
    		download = downloadManager.addDownload(curTorrent, torrentLocation, dataLocation);
    		
    	}finally{
    		    			
    		downloadManager.removeDownloadWillBeAddedListener(dwba);
    	}
    }
    
    return download;
  }

  private File setFile(String storePath, String storeFile) {
    File file = null;
    if(!storePath.endsWith(view.rssfeedConfig.separator))
      storePath = storePath + view.rssfeedConfig.separator;
    try {
      if(storeFile != null && storeFile.length() > 0) file = new File(storePath, storeFile);
      else file = new File(storePath);
    } catch(Exception e) {
      e.printStackTrace();
    }
    return file;
  }

  public File getTorrent(String url, final UrlBean urlBean, final ListBean listBean, String directoryName) throws Exception {
    listBean.resetInfo();
    Downloader downloader = new Downloader();
    downloader.addListener(new DownloaderListener() {
      @Override
      public void downloaderUpdate(int state, int percent, int amount, String err) {
        listBean.setState(state);
        if(percent > 0) listBean.setPercent(percent);
        if(amount > 0) listBean.setAmount(amount);
        if(!err.equalsIgnoreCase("")) listBean.setError(err);

        if(view.isOpen() && view.display != null && !view.display.isDisposed())
          view.display.asyncExec(new Runnable() {
            @Override
            public void run() {
              if(view.listTable == null || view.listTable.isDisposed()) return;
              ListTreeItem listItem = view.treeViewManager.getItem(listBean);
              if (listItem == null || listItem.isDisposed()) {
              	return;
              }
              listItem.update();
            }
          });
      }
    });

    downloader.init(url, "application/x-bittorrent, application/x-httpd-php", (urlBean.getLocRef()?urlBean.getLocation():urlBean.getReferer()),
        (urlBean.getUseCookie()?urlBean.getCookie():null), 0, null);
    listBean.downloader = downloader;

    if(downloader.getState() == Downloader.DOWNLOADER_CANCELED || downloader.getState() == Downloader.DOWNLOADER_ERROR) {
      return null;
    }

    String filename = downloader.fileName;
    if(!downloader.fileName.toLowerCase().endsWith(".torrent"))
      filename = "temp-" + Long.toString((new Date()).getTime()) + "-" + Long.toString((new Random()).nextLong()) + ".torrent";

    filename = FileUtil.convertOSSpecificChars( filename, false );
    
    File torrentLocation = new File(directoryName, filename);
    torrentLocation.createNewFile();
    FileOutputStream fileout = new FileOutputStream(torrentLocation, false);

    byte[] buf = new byte[4096];
    int read;
    while((read = downloader.read(buf)) != -1) {
      fileout.write(buf, 0, read);
      if(downloader.getState() == Downloader.DOWNLOADER_CANCELED) break;
    }
    fileout.flush();
    fileout.close();

    if(downloader.getState() == Downloader.DOWNLOADER_CANCELED || downloader.getState() == Downloader.DOWNLOADER_ERROR) {
      listBean.downloader = null;
      downloader.done();
      torrentLocation.delete();
      return null;
    }

    if(!downloader.fileName.toLowerCase().endsWith(".torrent")) {
      Plugin.debugOut("contentType: " + downloader.contentType);

      if(downloader.contentType != null && downloader.contentType.toLowerCase().startsWith("text/html")) {

        // html file encountered, look for link to torrent
        String href = findTorrentHref(torrentLocation, url, listBean);
        Plugin.debugOut("href: " + href);
        torrentLocation.delete();
        if(href != null) {
          listBean.downloader = null;
          downloader.done();
          return getTorrent(href, urlBean, listBean, directoryName);
        } else throw new Exception("Html content returned, but no links to torrent files found.");

      } else if(downloader.contentType != null && !downloader.contentType.toLowerCase().startsWith("application/x-bittorrent")) {

        // something else encountered, just move it to outputdir
        File newFile = new File(directoryName, downloader.fileName != null?downloader.fileName:torrentLocation.getName());
        if(!torrentLocation.renameTo(newFile)) Plugin.debugOut("failed to move " + torrentLocation);
        Plugin.debugOut("Non-torrent download encountered and moved: " + newFile + " (" + downloader.contentType + ")");
        torrentLocation = null;

      } else {

    	if ( torrentLocation.length() == 0 ){
    			// when a magnet link fails to download the result is a zero length file
    		throw( new IOException( "Torrent download failed" ));
    	}
        Torrent torrent = torrentManager.createFromBEncodedFile(torrentLocation);
        String name = torrent.getName() + ".torrent";
        File newFile = new File(directoryName, name);
        if(torrentLocation.renameTo(newFile)) {
          downloader.fileName = name;
          torrentLocation = newFile;
        }
        torrent = null;

      }
    }

    listBean.downloader = null;
    downloader.done();
    return torrentLocation;
  }

  protected static String findTorrentHref(File htmlDoc, String baseUrl, ListBean listBean) throws IOException {
    BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(htmlDoc)));
    HtmlAnalyzer parser = new HtmlAnalyzer(baseUrl, listBean);
    new ParserDelegator().parse(bufReader, parser, true);
    return parser.getTorrentUrl();
  }

}
