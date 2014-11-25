package android.app.printerapp.octoprint;

import android.app.printerapp.devices.DevicesListController;
import android.app.printerapp.devices.database.DatabaseController;
import android.app.printerapp.devices.discovery.PrintNetworkManager;
import android.app.printerapp.model.ModelPrinter;
import android.util.Log;

/**
 * Addresses and static fields for the OctoPrint API connection
 * @author alberto-baeza
 *
 */

public class HttpUtils {
	
	  public static final String CUSTOM_PORT = ":5000"; //Octoprint server listening port
	  public static final String API_KEY = "5A41D8EC149F406F9F222DCF93304B43FALSA"; //Hardcoded API Key
	  
	  /** OctoPrint URLs **/
	  
	  public static final String URL_FILES = "/api/files"; //File operations
	  public static final String URL_CONTROL = "/api/job"; //Job operations
	  public static final String URL_SOCKET = "/sockjs/websocket"; //Socket handling
	  public static final String URL_CONNECTION = "/api/connection"; //Connection handling
	  public static final String URL_PRINTHEAD = "/api/printer/printhead"; //Send print head commands
	  public static final String URL_NETWORK = "/api/plugin/netconnectd"; //Network config
	  public static final String URL_SLICING = "/api/slicing/cura/profiles";
	  public static final String URL_DOWNLOAD_FILES = "/downloads/files/local/";
      public static final String URL_SETTINGS = "/api/settings";
      public static final String URL_AUTHENTICATION = "/apps/auth";

    /** External links **/

      public static final String URL_THINGIVERSE = "http://www.thingiverse.com/newest";

    //Retrieve current API Key from database
    public static String getApiKey(String url){
        String parsedUrl = url.substring(0,url.indexOf("/",1));

        String id = null;

        for (ModelPrinter p : DevicesListController.getList()){
            if (p.getAddress().equals(parsedUrl)) id = PrintNetworkManager.getNetworkId(p.getName());
        }

        if (DatabaseController.isPreference("Keys",id)){

            return DatabaseController.getPreference("Keys",id);

        } else {

            Log.i("Connection", id + " is not preference");
            return "";
        }

    }
}
