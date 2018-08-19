package TianChi;

import java.util.HashMap;
import java.util.Map;

public class App {
	
	private String appId;
	private Map<Integer,Integer> compatibleApp ;//1:appIndex，2:Integer为兼容个数

	public App(String appId) {
		this.appId = appId;
		compatibleApp = new HashMap<Integer, Integer>();
	}
	
	public String getAppId() {
		return appId;
	}
	public void setAppId(String appId) {
		this.appId = appId;
	}
	
	public Map<Integer, Integer> getCompatibleApp() {
		return compatibleApp;
	}


	public void setCompatibleApp(Map<Integer, Integer> compatibleApp) {
		this.compatibleApp = compatibleApp;
	}

}
