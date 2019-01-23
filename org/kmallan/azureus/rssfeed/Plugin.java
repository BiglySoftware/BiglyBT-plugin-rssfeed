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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.biglybt.core.util.AENetworkClassifier;
import com.biglybt.core.util.Debug;
import com.biglybt.pif.PluginInterface;
import com.biglybt.pif.ui.UIInstance;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.UIManagerEvent;
import com.biglybt.pif.ui.UIManagerListener;
import com.biglybt.pif.ui.config.*;
import com.biglybt.pif.ui.model.BasicPluginConfigModel;
import com.biglybt.pif.utils.LocaleUtilities;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.pif.UISWTInstance;
import com.biglybt.ui.swt.pif.UISWTViewEvent;
import com.biglybt.ui.swt.pif.UISWTViewEventListener;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;


public class Plugin implements com.biglybt.pif.Plugin {

  public static String PLUGIN_VERSION = "x.x.x";
  public static final int MIN_REFRESH = 300;
  public static final boolean DEBUG = false;

  private static PluginInterface pluginInterface;
  private final String VIEWID = "RSSFeed Scanner";
  private ViewListener viewListener = null;
  private UISWTInstance swtInstance = null;
  private static Config rssfeedConfig;
  private static View view = null;

  private List<BooleanParameter>	net_params;
  
  @Override
  public void initialize(PluginInterface pluginIf) {
    pluginInterface = pluginIf;
    rssfeedConfig = new Config();
    
    	final UIManager ui_manager = pluginInterface.getUIManager();
    	
		final LocaleUtilities loc_utils = pluginInterface.getUtilities().getLocaleUtilities();

		BasicPluginConfigModel config = ui_manager.createBasicPluginConfigModel( "RSSFeed.Config" );
		
		BooleanParameter 	auto_reload 		= config.addBooleanParameter2("Enabled", "RSSFeed.Config.Enable", true);
		IntParameter		auto_reload_delay 	= config.addIntParameter2("Delay", "RSSFeed.Config.Delay", 900);
		config.addBooleanParameter2("AutoLoad", "RSSFeed.Config.AutoLoad", false);
		config.addBooleanParameter2("AutoStartManual", "RSSFeed.Config.AutoStartManual", true);
		config.addIntParameter2("KeepOld", "RSSFeed.Config.KeepOld", 2);
		config.addIntParameter2("KeepMax", "RSSFeed.Config.KeepMax", 1000);
		final BooleanParameter fnp = config.addBooleanParameter2("ForceNoProxy", "RSSFeed.Config.ForceNoProxy", false);
		final BooleanParameter tpp = config.addBooleanParameter2("TryPluginProxy", "RSSFeed.Config.TryPluginProxy", false);
			
		ParameterListener proxy_listener = 
			new ParameterListener() {
				
				@Override
				public void parameterChanged(Parameter param) {
					tpp.setEnabled( !fnp.getValue());
				}
			};
			
		fnp.addListener( proxy_listener );
		tpp.addListener( proxy_listener );
		
		proxy_listener.parameterChanged( null );
		
		ActionParameter export_param = config.addActionParameter2(  "RSSFeed.Config.export.info", "RSSFeed.Config.export.label" );
		
		export_param.addListener(
			new ParameterListener(){
				
				@Override
				public void
				parameterChanged(
					Parameter param) 
				{
					try{
						File result = rssfeedConfig.exportToJSON();
						
						String msg = loc_utils.getLocalisedMessageText( "RSSFeed.Config.export.ok.msg", new String[]{ result.getAbsolutePath() });
						
						ui_manager.showMessageBox(
								"RSSFeed.Config.export.ok.title",
								"!" + msg + "!",
								UIManagerEvent.MT_OK );
						
					}catch( Throwable e ){
						
						String msg = loc_utils.getLocalisedMessageText( "RSSFeed.Config.export.fail.msg", new String[]{ Debug.getNestedExceptionMessage(e )});
						
						ui_manager.showMessageBox(
								"RSSFeed.Config.export.fail.title",
								"!" + msg + "!",
								UIManagerEvent.MT_OK );	
					}
				}
			});
		
		ActionParameter import_param = config.addActionParameter2( "RSSFeed.Config.import.info", "RSSFeed.Config.import.label" );
		
		import_param.addListener(
			new ParameterListener(){
				
				@Override
				public void
				parameterChanged(
					Parameter param) 
				{
					try{
						rssfeedConfig.importFromJSON();
						
						String msg = loc_utils.getLocalisedMessageText( "RSSFeed.Config.import.ok.msg" );
						
						ui_manager.showMessageBox(
								"RSSFeed.Config.import.ok.title",
								"!" + msg + "!",
								UIManagerEvent.MT_OK );
						
					}catch( Throwable e ){
						
						String msg = loc_utils.getLocalisedMessageText( "RSSFeed.Config.import.fail.msg", new String[]{ Debug.getNestedExceptionMessage(e )});
						
						ui_manager.showMessageBox(
								"RSSFeed.Config.import.fail.title",
								"!" + msg + "!",
								UIManagerEvent.MT_OK );		
					}
				}
			});
		
		ActionParameter browse_param = config.addActionParameter2( "RSSFeed.Config.browse.info", "RSSFeed.Config.browse.label" );
		
		browse_param.addListener(
			new ParameterListener(){
				
				@Override
				public void
				parameterChanged(
					Parameter param) 
				{
					Utils.launch( rssfeedConfig.getPath());
				}
			});

		config.createGroup(
			 "RSSFeed.Config.impexp",
			 new Parameter[]{
					 export_param, import_param, browse_param,
			 });
		
		auto_reload.addEnabledOnSelection( auto_reload_delay );
		
		net_params = new ArrayList<BooleanParameter>();
		
		final BooleanParameter net_force = config.addBooleanParameter2("RSSFeed.Config.netforce", "RSSFeed.Config.netforce", false);

		net_params.add( net_force );
		
		for ( String net: AENetworkClassifier.AT_NETWORKS ){
			
			BooleanParameter net_param = config.addBooleanParameter2("RSSFeed.Config.netforce." + net, "ConfigView.section.connection.networks." + net, false);

			net_force.addEnabledOnSelection( net_param );
			
			net_params.add( net_param );
		}
		
		config.createGroup( "RSSFeed.Config.netgroup", net_params.toArray( new Parameter[net_params.size()]));
				 

		PLUGIN_VERSION = pluginInterface.getPluginVersion();

		pluginInterface.getUIManager().addUIListener(
			new UIManagerListener()
			{
				@Override
				public void
				UIAttached(
					UIInstance		instance )
				{
					if ( instance instanceof UISWTInstance ){
						view = new View(Plugin.this, pluginInterface, rssfeedConfig);
						
						swtInstance = ((UISWTInstance) instance);

						viewListener = new ViewListener();
						if (viewListener != null) {
							// Add it to the menu
							swtInstance.addView(UISWTInstance.VIEW_MAIN, VIEWID, viewListener);
							// Open it immediately
							if( pluginInterface.getPluginconfig().getPluginBooleanParameter("AutoLoad", false) )
								swtInstance.openMainView(VIEWID, viewListener, null);
						}
						
					}
				}
				
				@Override
				public void
				UIDetached(
					UIInstance		instance )
				{
					if (instance instanceof UISWTInstance) {
						swtInstance = null;

						if (viewListener != null) {
							((UISWTInstance) instance).removeViews(UISWTInstance.VIEW_MAIN, VIEWID);
						}
					}
				}
			});
	
  }
  
	private class ViewListener implements UISWTViewEventListener {

		boolean bInitialized = false;

		@Override
		public boolean eventOccurred(UISWTViewEvent event) {
			switch (event.getType()) {
				case UISWTViewEvent.TYPE_CREATE:
					/* We only want one view
					 * 
					 * If we wanted multiple views, we would need a class to handle
					 * one view.  Then, we could set up a Map, with the key
					 * being the UISWTView, and the value being a new instance of the
					 * class.  When the other types of events are called, we would
					 * lookup our class using getView(), and then pass the work to
					 * the our class.
					 */
					if (bInitialized)
						return false;
					break;

				case UISWTViewEvent.TYPE_INITIALIZE:
					if (bInitialized)
						return false;

					view.initialize((Composite) event.getData());
					bInitialized = true;
					break;

				case UISWTViewEvent.TYPE_REFRESH:
					//view.refresh();
					break;

				case UISWTViewEvent.TYPE_DESTROY:
					view.delete();
					bInitialized = false;
					break;

				case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:
					break;

				case UISWTViewEvent.TYPE_FOCUSGAINED:
					break;

				case UISWTViewEvent.TYPE_FOCUSLOST:
					break;

				case UISWTViewEvent.TYPE_LANGUAGEUPDATE:
					break;
			}
			return true;
		}
	}

  public static String getPluginDirectoryName() {
    return pluginInterface.getPluginDirectoryName();
  }

  public static int getIntParameter(String name) {
    return pluginInterface.getPluginconfig().getPluginIntParameter(name);
  }

  public static boolean getBooleanParameter(String name) {
    return pluginInterface.getPluginconfig().getPluginBooleanParameter(name);
  }

  public static void setParameter(String name, int value) {
    pluginInterface.getPluginconfig().setPluginParameter(name, value);
  }

  public static final int PROXY_DEFAULT		= 0;
  public static final int PROXY_FORCE_NONE	= 1;
  public static final int PROXY_TRY_PLUGIN	= 2;
  
  public static int
  getProxyOption()
  {
	  if ( Plugin.getBooleanParameter( "ForceNoProxy")){
		  
		  return( PROXY_FORCE_NONE );
	  }else{
		  
		  if ( Plugin.getBooleanParameter( "TryPluginProxy")){

			  return( PROXY_TRY_PLUGIN );
			  
		  }else{
			  
			  return( PROXY_DEFAULT );
		  }
	  }
  }
  
  public String[]
  getForcedNetworks()
  {
	  if ( net_params == null ){
		  
		  return( null );
	  }
	  
	  Iterator<BooleanParameter> it = net_params.iterator();
	  
	  if ( !it.next().getValue()){
		  
		  return( null );
	  }
	  
	  List<String>	result = new ArrayList<String>();
	  
	  for ( String net: AENetworkClassifier.AT_NETWORKS ){

		  if (it.next().getValue()){
			  
			  result.add( net );
		  }
	  }
	  
	  return( result.toArray( new String[ result.size()]));
  }
  
  public static PluginInterface
  getPluginInterface()
  {
	  return( pluginInterface );
  }
  
  public static void updateView(final ListBean listBean) {
    if (view != null && view.isOpen() && view.display != null
				&& !view.display.isDisposed()) {
			view.display.asyncExec(new Runnable() {
				@Override
				public void run() {
					if (view.listTable == null || view.listTable.isDisposed())
						return;
					ListTreeItem listItem = view.treeViewManager.getItem(listBean);
					listItem.update();
				}
			});
    }
  }

  public static void debugOut(String s) {
    if (DEBUG) {
			System.out.println(new Date() + "] RSSFeed: " + s + " ["
					+ Thread.currentThread().getName() + "]");
    }
  }

  public static void launchUrl(final String url) {
    new Thread("URLLauncherThread") {
      @Override
      public void run() {
        Program.launch(url);
      }
    }.start();
  }
}