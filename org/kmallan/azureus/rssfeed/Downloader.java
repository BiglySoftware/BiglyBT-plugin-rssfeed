/*
 *
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

import com.biglybt.core.security.SESecurityManager;
import com.biglybt.core.util.Constants;
import com.biglybt.core.util.Debug;
import com.biglybt.core.util.RandomUtils;
import com.biglybt.core.networkmanager.admin.NetworkAdmin;
import com.biglybt.core.proxy.AEProxyFactory;
import com.biglybt.core.proxy.AEProxyFactory.PluginProxy;
import com.biglybt.core.proxy.AEProxySelectorFactory;

import javax.net.ssl.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class Downloader extends InputStream {

  final static int DOWNLOADER_NON_INIT = 0, DOWNLOADER_INIT = 1, DOWNLOADER_START = 2;
  final static int DOWNLOADER_DOWNLOADING = 3, DOWNLOADER_FINISHED = 4, DOWNLOADER_CANCELED = 5;
  final static int DOWNLOADER_NOTMODIFIED = 6, DOWNLOADER_CHECKING = 7, DOWNLOADER_ERROR = -1;

  private String target_url_str;
  
  private URLConnection con;

  private int state = DOWNLOADER_NON_INIT;
  private int percentDone = 0;
  private int readTotal = 0;
  private int size = 0;
  private int refCount = 0;

  protected String fileName, contentType, etag;
  protected long lastModified;
  protected InputStream in;
  protected List listeners = new ArrayList();

  public int getState() {
    return state;
  }

  private synchronized void error(String err) {
    synchronized(listeners) {
      state = DOWNLOADER_ERROR;
      fireDownloaderUpdate(state, 0, 0, err);
    }
  }

/*
  public static void testDownload(String url) throws Exception {
    Downloader downloader = new Downloader();
    downloader.addListener(new DownloaderListener() {
      public void downloaderUpdate(int state, int percent, int amount, String err) {
        System.out.println(state + " " + percent + " " + amount + " " + err);
      }
    });
    downloader.init(url, "application/x-bittorrent, application/x-httpd-php", null, null, 0, null);

    if(downloader.getState() == Downloader.DOWNLOADER_CANCELED || downloader.getState() == Downloader.DOWNLOADER_ERROR) {
      System.out.println("null");
      return;
    }

    String filename = downloader.fileName;
    if(!downloader.fileName.toLowerCase().endsWith(".torrent"))
      filename = "temp-" + Long.toString((new Date()).getTime()) + "-" + Long.toString((new Random()).nextLong()) + ".torrent";

    File torrentLocation = new File(filename);
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
      downloader.done();
      torrentLocation.delete();
      return;
    }

    if(!downloader.fileName.toLowerCase().endsWith(".torrent")) {
      Plugin.debugOut("contentType: " + downloader.contentType);

      if(downloader.contentType != null && downloader.contentType.toLowerCase().startsWith("text/html")) {

        // html file encountered, look for link to torrent
        String href = TorrentDownloader.findTorrentHref(torrentLocation, url, null);
        if(href != null) {
          Plugin.debugOut("href: " + href);
          torrentLocation.delete();
          downloader.done();
          testDownload(href);
        } else throw new Exception("Html content returned, but no links to torrent files found.");

      } else if(downloader.contentType != null && !downloader.contentType.toLowerCase().startsWith("application/x-bittorrent")) {

        // something else encountered, just move it to outputdir
        System.out.println("other " + downloader.fileName);

      }
    }
    System.out.println("torrent?");
  }

  public static void main(String[] args) throws Exception {
    String url = "";
    testDownload(url);
  }
*/
  public void init(String _urlStr, String accept, String referer, String cookie, long lastModSince, String oldEtag) {

	  int proxy_opt = Plugin.getProxyOption();

	  boolean is_magnet = _urlStr.toLowerCase().startsWith( "magnet:" );

	  if ( !is_magnet ){
		  Pattern exprHost = Pattern.compile(".*(https?://.*?)", Pattern.CASE_INSENSITIVE);
		  Matcher m = exprHost.matcher(_urlStr);
		  if(m.matches() && !m.group(1).equalsIgnoreCase(_urlStr)) _urlStr = m.group(1);
	  }

	  target_url_str = _urlStr.replaceAll(" ", "%20");

	  if ( NetworkAdmin.getSingleton().hasMissingForcedBind()){

		error( "Forced bind address is missing" );
		
	  }else{
		  try {
			  URL target_url = new URL(target_url_str);
	
			  if ( proxy_opt == Plugin.PROXY_FORCE_NONE ){
	
				  AEProxySelectorFactory.getSelector().startNoProxy();
			  }
			  
			  synchronized( listeners ){
	
				  boolean	first_effort		= true;
				  
				  boolean	cert_hack 			= false;
				  boolean	internal_error_hack	= false;
				  
				  while( true ){
					  
					  PluginProxy plugin_proxy = null;
					  
					  URL	url			= target_url;
					  URL	initial_url	= url;
					  
					  try{
					      Proxy	proxy		= null;
	
						  if ( proxy_opt == Plugin.PROXY_TRY_PLUGIN ){
	
							  plugin_proxy = AEProxyFactory.getPluginProxy( "RSSFeed plugin", url );
		
							  if ( plugin_proxy != null ){
		
								  url 	= plugin_proxy.getURL();
								  proxy	= plugin_proxy.getProxy();
							  }
						  }
						  
						  if (url.getProtocol().equalsIgnoreCase("https")) {
							  
							  HttpsURLConnection sslCon;
							  
							  if ( proxy == null ){
								  
								  sslCon = (HttpsURLConnection)url.openConnection();
								  
							  }else{
								  
								  sslCon = (HttpsURLConnection)url.openConnection( proxy );
							  }
	
							  if ( !first_effort ){
								  
								  TrustManager[] trustAllCerts = SESecurityManager.getAllTrustingTrustManager();
	
								  try{
									  SSLContext sc = SSLContext.getInstance("SSL");
	
									  sc.init(null, trustAllCerts, RandomUtils.SECURE_RANDOM);
	
									  SSLSocketFactory factory = sc.getSocketFactory();
	
									  sslCon.setSSLSocketFactory( factory );
	
								  }catch( Throwable e ){
								  }
							  }else{
								  	// allow for certs that contain IP addresses rather than dns names
								  sslCon.setHostnameVerifier(new HostnameVerifier() {
									  @Override
									  public boolean verify(String host, SSLSession session) {return true;}
								  });
							  }
							  con = sslCon;
							  
						  } else {
							  if ( proxy == null ){
							  
								  con = url.openConnection();
								  
							  }else{
								  
								  con = url.openConnection( proxy );
							  }
						  }
	
						  con.setDoInput(true);
						  con.setUseCaches(false);
	
						  if(con instanceof HttpURLConnection) {
							  if ( !is_magnet ){
								  Pattern exprHost = Pattern.compile("https?://([^/]+@)?([^/@:]+)(:[0-9]+)?/.*");
								  Matcher m = exprHost.matcher(target_url_str.toLowerCase());
								  if(m.matches()) con.setRequestProperty("Host", m.group(2)); // isn't this handled automatically? /bow
							  }
							  con.setRequestProperty("User-Agent", Plugin.PLUGIN_VERSION);
							  if(referer != null && referer.length() > 0) con.setRequestProperty("Referer", referer);
							  if(accept != null && accept.length() > 0) con.setRequestProperty("Accept", accept);
							  if(cookie != null && cookie.length() > 0) con.setRequestProperty("Cookie", cookie);
							  if(lastModSince > 0) con.setIfModifiedSince(lastModSince);
							  if(oldEtag != null) con.setRequestProperty("If-None-Match", oldEtag);
							  
							  if ( plugin_proxy != null ){
									
								con.setRequestProperty( "HOST", plugin_proxy.getURLHostRewrite() + (initial_url.getPort()==-1?"":(":" + initial_url.getPort())));
							  }
						  }
	
						  state = DOWNLOADER_INIT;
						  fireDownloaderUpdate(state, 0, 0, "");
	
						  con.connect();        
	
						  if(con instanceof HttpURLConnection) {
							  int response = ((HttpURLConnection)con).getResponseCode();
							  Plugin.debugOut("response code: " + response);
	
							  if(response == -1) { // HttpURLConnection in undefined state? weird stuff... occurs sporadically
								  Thread.sleep(10000); // waiting and trying again seems to do the trick
								  if(refCount++ < 5) {
									  init(_urlStr, accept, referer, cookie, lastModSince, oldEtag);
									  return;
								  }
							  }
	
							  String refresh = con.getHeaderField("Refresh");
							  if(refresh != null) {
								  Plugin.debugOut("refresh: " + refresh);
								  int idx = refresh.indexOf("url=");
								  if(idx > -1) {
									  refresh = refresh.substring(idx + 4);
									  if(refresh.indexOf(' ') > -1) refresh = refresh.substring(0, refresh.lastIndexOf(' '));
									  ((HttpURLConnection)con).disconnect();
									  if(refresh.indexOf("://") == -1) refresh = HtmlAnalyzer.resolveRelativeURL(target_url_str, refresh);
									  Plugin.debugOut("new url: " + refresh);
									  if(refCount++ < 3) init(refresh, accept, referer, cookie, lastModSince, oldEtag);
								  }
							  }
	
							  if(response == HttpURLConnection.HTTP_NOT_MODIFIED) {
								  state = DOWNLOADER_NOTMODIFIED;
								  return;
							  } else if((response != HttpURLConnection.HTTP_ACCEPTED) && (response != HttpURLConnection.HTTP_OK)) {
	                              if (response == HttpURLConnection.HTTP_MOVED_PERM || response == HttpURLConnection.HTTP_MOVED_TEMP) {
	                                final String location = con.getHeaderField("Location");
	                                Plugin.debugOut("Redirected: " + _urlStr + " -> " + location);
	                                if (location != null && !location.equals(_urlStr)) {
	                                  init(location, accept, referer, cookie, lastModSince, oldEtag);
	                                } else {
	                                  error("Suspicious redirect: " + location);
	                                }
	                                return;
	                              } else {
	                                error("Bad response for '" + url.toString() + "': " + Integer.toString(
	                                    response) + " " + ((HttpURLConnection) con).getResponseMessage());
	                                return;
	                              }
							  }
							  contentType = con.getContentType();
							  lastModified = con.getLastModified();
							  etag = con.getHeaderField("ETag");
							  
							  url = con.getURL();
	
							  // some code to handle b0rked servers.
							  fileName = con.getHeaderField("Content-Disposition");
							  if((fileName != null) && fileName.toLowerCase().matches(".*attachment.*"))
								  while(fileName.toLowerCase().charAt(0) != 'a') fileName = fileName.substring(1);
							  if((fileName == null) || !fileName.toLowerCase().startsWith("attachment") || (fileName.indexOf('=') == -1)) {
								  String tmp = url.getFile();
								  if(tmp.lastIndexOf('/') != -1) tmp = tmp.substring(tmp.lastIndexOf('/') + 1);
								  // remove any params in the url
								  int paramPos = tmp.indexOf('?');
								  if(paramPos != -1) tmp = tmp.substring(0, paramPos);
	
								  fileName = URLDecoder.decode(tmp, Constants.DEFAULT_ENCODING);
							  } else {
								  fileName = fileName.substring(fileName.indexOf('=') + 1);
								  if(fileName.startsWith("\"") && fileName.endsWith("\"")) fileName = fileName.substring(1, fileName.lastIndexOf('\"'));
								  File temp = new File(fileName);
								  fileName = temp.getName();
							  }
						  }
	
						  break;
					  }catch( SSLException e ){
	
						  first_effort = false;
						  
						  String msg = Debug.getNestedExceptionMessage( e );
	
						  if ( !cert_hack ){
	
							  cert_hack = true;
							  
							  Plugin.getPluginInterface().getUtilities().getSecurityManager().installServerCertificate( url );
	
							  continue;
						  }
						  
						  if ( !internal_error_hack ){
							  
							  if ( msg.contains( "internal_error" ) || msg.contains( "handshake_failure" )){
								  
								  internal_error_hack = true;
							  
								  continue;
							  }
						  }
	
						  throw( e );
						 
					  }finally{
					 
						  if ( plugin_proxy != null ){
	
							  plugin_proxy.setOK( true );
						  }
					  }
				  }
	
			  }
		  } catch(java.net.MalformedURLException e) {
			  e.printStackTrace();
			  error("Bad URL '" + target_url_str + "':" + e.getMessage());
		  } catch(java.net.UnknownHostException e) {
			  e.printStackTrace();
			  error("Unknown Host '" + e.getMessage() + "'");
		  } catch(IOException ioe) {
			  ioe.printStackTrace();
			  error("Failed: " + ioe.getMessage());
		  } catch(Throwable e) {
			  e.printStackTrace();
			  error("Failed: " + e.toString());
		  }finally{
	
			  if ( proxy_opt == Plugin.PROXY_FORCE_NONE ){
	
				  AEProxySelectorFactory.getSelector().endNoProxy();
			  }
		  }
	  }
	  
	  if(state != DOWNLOADER_ERROR) {
		  synchronized(listeners) {
			  state = DOWNLOADER_START;
			  fireDownloaderUpdate(state, 0, 0, "");
			  state = DOWNLOADER_DOWNLOADING;
			  fireDownloaderUpdate(state, 0, 0, "");
		  }
		  try {
			  in = con.getInputStream();

			  String encoding = con.getHeaderField( "content-encoding");

			  boolean compressed = false;

			  if ( encoding != null ){

				  if ( encoding.equalsIgnoreCase( "gzip"  )){

					  compressed = true;

					  in = new GZIPInputStream( in );

				  }else if ( encoding.equalsIgnoreCase( "deflate" )){

					  compressed = true;

					  in = new InflaterInputStream( in );
				  }
			  }

			  size = compressed?-1:con.getContentLength();
			  percentDone = readTotal = 0;
		  } catch(Exception e) {
			  error("Exception while downloading '" + target_url_str + "':" + e.getMessage());
		  }
	  }
  }

  public InputStream getStream() {
    return in;
  }

  public void cancel() {
    if(state == DOWNLOADER_ERROR) return;
    synchronized(listeners) {
      state = DOWNLOADER_CANCELED;
      fireDownloaderUpdate(state, 0, 0, "");
    }
  }

  public void done() {
    if(state == DOWNLOADER_ERROR || state == DOWNLOADER_CANCELED || state == DOWNLOADER_FINISHED) return;
    synchronized(listeners) {
      if(state != DOWNLOADER_NOTMODIFIED && readTotal == 0) {
        error("No data contained in '" + target_url_str + "'");
        return;
      }
      state = DOWNLOADER_FINISHED;
      fireDownloaderUpdate(state, 0, 0, "");
    }
  }

  public void notModified() {
    if(state == DOWNLOADER_NOTMODIFIED) return;
    synchronized(listeners) {
      state = DOWNLOADER_NOTMODIFIED;
      fireDownloaderUpdate(state, 0, 0, "");
    }
  }

  @Override
  protected void finalize() {
    try {
    	InputStream _in = in;
    	
    	if ( _in != null ){
    		_in.close();
    	}
    } catch(Throwable e) {}
    
    try{
    	URLConnection _con = con;
    	if ( _con instanceof HttpURLConnection ){
    		((HttpURLConnection)_con).disconnect();
    		
    	}
    }catch( Throwable e ){}
    done();
  }

  // listeners
  public void addListener(DownloaderListener l) {
    synchronized(listeners) {
      listeners.add(l);
    }
  }

  public void removeListener(DownloaderListener l) {
    synchronized(listeners) {
      listeners.remove(l);
    }
  }

  private void fireDownloaderUpdate(int state, int percentDone, int readTotal, String str) {
    for(int i = 0; i < listeners.size(); i++) {
      ((DownloaderListener)listeners.get(i)).downloaderUpdate(state, percentDone, readTotal, str);
    }
  }

  // InputStream Filtered Stuff
  @Override
  public int read() throws IOException{
    try {
      synchronized(listeners) {
        int read = in.read();
        if ( read == -1 ){
        	return( -1 );
        }
        readTotal += read;
        if(this.size > 0) {
          this.percentDone = (100 * this.readTotal) / this.size;
          fireDownloaderUpdate(state, percentDone, 0, "");
        } else fireDownloaderUpdate(state, 0, readTotal, "");
        return read;
      }
    } catch(IOException e) {error( e.getMessage());}
    return -1;
  }

  @Override
  public int read(byte b[]) throws IOException{
    return read(b, 0, b.length);
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    try {
      synchronized(listeners) {
        int read = in.read(b, off, len);
        if ( read == -1 ){
        	return( -1 );
        }
        this.readTotal += read;
        if(this.size > 0) {
          this.percentDone = (100 * this.readTotal) / this.size;
          fireDownloaderUpdate(state, percentDone, 0, "");
        } else fireDownloaderUpdate(state, 0, readTotal, "");
        return read;
      }
    } catch(IOException e) {
    	error( e.getMessage());
    }
    return -1;
  }

  // InputStream PassThru Stuff
  @Override
  public long skip(long n) {
    try { return in.skip(n); } catch(IOException e) { }
    return 0;
  }

  @Override
  public int available() {
    try { return in.available(); } catch(IOException e) { }
    return 0;
  }

  @Override
  public void close() { try { in.close(); } catch(IOException e) { } }
  @Override
  public synchronized void mark(int readlimit) { in.mark(readlimit); }
  @Override
  public synchronized void reset() { try { in.reset(); } catch(IOException e) { } }
  @Override
  public boolean markSupported() { return in.markSupported(); }
}
