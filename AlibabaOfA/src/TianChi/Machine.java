package TianChi;
import java.util.*;
public class Machine {
	private String machineId;
	private Map <Integer,Integer> hasApps;//有哪些app的标号和个数
	
	public Machine(String machineId) {
		this.machineId = machineId;
		hasApps = new HashMap<Integer, Integer>();//第一个Integer为appIndex(id-1),第二个为个数
	}
	
	public String getMachineId() {
		return machineId;
	}

	public void setMachineId(String machineId) {
		this.machineId = machineId;
	}

	public Map<Integer, Integer> getHasApps() {
		return hasApps;
	}

	public void setHasApps(Map<Integer, Integer> hasApps) {
		this.hasApps = hasApps;
	}
	
}
