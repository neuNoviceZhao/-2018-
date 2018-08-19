package TianChi;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
public class Alibaba {

	/**
	 * @param args
	 */
    public static final double  alpha             = 10.;
    public static final double  beta              = 0.5;
    public static final int     T                 = 98;
    public static final int     EXEC_LIMIT        = 100000;
    public static final double  avgUseAge         = 0.4817;//实际上可以通过调小这个参数，去找更多的异常机器
    public static       int     moveNum;         //需要迁移的实例
    public static       int     initIn;          //初始部署个数
	//读取文件
    private static List<String[]> a_app_resources = FileIO.readCsvFile("E:/Myeclipse/scheduling_preliminary_a_app_resources_20180606.csv");
	private static List<String[]> a_app_interference = FileIO.readCsvFile("E:/Myeclipse/scheduling_preliminary_a_app_interference_20180606.csv");
	private static List<String[]> a_instance_deploy = FileIO.readCsvFile("E:/Myeclipse/scheduling_preliminary_a_instance_deploy_20180606.csv");
	private static List<String[]> a_machine_resources = FileIO.readCsvFile("E:/Myeclipse/scheduling_preliminary_a_machine_resources_20180606.csv");
	
	//需要放置的instance
	private static Map <String ,MyPair<Integer,Integer>> instancesIn;//inst->machIndex
	private static  List<MyPair<String ,Integer>> instancesOut;//记录顺序->inst->appIndex
	private static  List<MyPair<String ,Integer>> initConfict;//记录顺序->inst->appIndex
	private static  List<Machine>         machines;
	private static  List<App>             apps;
	private static  double[][]            appResources;       //app的资源
	private static  double[][]            machineResources;   // 机器的资源
	private static  double[][]            machineResourcesUsed; //机器用了多少
	
    public static void main (String[]args) {
		Alibaba ali = new Alibaba();
		ali.putInst2();
		
	//	ali.write();
		String [] result = new String[instancesIn.size()];
		int num = 0;
		
		int x = 0;
		for (String inst: instancesIn.keySet() ) {
			if(x++ < initIn - moveNum)continue;//有29853个 初始状态不冲突的实例不需要移动，即不需要体现在结果文件中
			result[num++] = inst + ","+machines.get(instancesIn.get(inst).getSecond()).getMachineId();

		}
		FileUtil.write("C:/Users/zyf/Desktop/阿里数据分析/result.csv", result,false);
	}
    
    public static double getScore(){
    	double costs = 0;
        for (int j = 0; j < 6000; j++) {
            // 技术得分
            for (int k = 0; k < 98; k++) {
                double usage = machineResourcesUsed[j][k] / machineResources[j][k];
                costs += 1. + alpha * (Math.exp(Math.max(0., usage - beta)) - 1.);
            }
        }
        costs /= T;
       return costs;
    }
  
	
	public Alibaba(){
		init();
	}
	
	
	public void init(){
		/**
		 * 初始化app
		 */
		apps = new ArrayList<App>();
		appResources = new double[a_app_resources.size()][2 * T + a_app_resources.get(0).length - 3];
		for (int i = 0; i < a_app_resources.size(); i++) {
			apps.add(new App(a_app_resources.get(i)[0]));
			String cpu[] = a_app_resources.get(i)[1].split("\\|",-1);
			String mem[] = a_app_resources.get(i)[2].split("\\|",-1);
		    
			for (int j = 0; j < T; j++) {//将98个cpu和mem分时数据
				appResources[i][j] = Double.parseDouble(cpu[j]);
				appResources[i][T+j] = Double.parseDouble(mem[j]);
			}
			for (int j = 3; j < a_app_resources.get(0).length; j++) {
				appResources[i][2*T+j-3] = Double.parseDouble(a_app_resources.get(i)[j]);
			}
		
		}
		//将互斥信息加入
		 for (int i = 0; i < a_app_interference.size(); i++) {
		     int index1 = Integer.parseInt(a_app_interference.get(i)[0].substring(4, a_app_interference.get(i)[0].length()));
		     int index2 = Integer.parseInt(a_app_interference.get(i)[1].substring(4, a_app_interference.get(i)[1].length()));
		     if(index1 != index2){
		    	 apps.get(index1-1).getCompatibleApp().put(index2-1, 
		    			 				Integer.parseInt(a_app_interference.get(i)[2]));
		     }else{
		    	 //若相同多加一个<APP1,App1,0>说明允许一个App1存在
		    	 apps.get(index1-1).getCompatibleApp().put(index2 - 1, Integer.parseInt(a_app_interference.get(i)[2])+1);
		     }
		 }
		 
		 /**
		  * 初始化mechine
		  */
		
		machines = new ArrayList<Machine>();
		machineResources = new double [a_machine_resources.size()][2 * T + a_machine_resources.get(0).length - 3];
		machineResourcesUsed = new double [a_machine_resources.size()][2 * T + a_machine_resources.get(0).length - 3];
		for (int i = 0; i < a_machine_resources.size(); i++) {
			machines.add(new Machine(a_machine_resources.get(i)[0]));
			for (int j = 0; j < T; j++) {//将98个cpu和mem分时数据
				machineResources[i][j] = Double.parseDouble(a_machine_resources.get(i)[1]);
				machineResources[i][T+j] = Double.parseDouble(a_machine_resources.get(i)[2]);
			}
			for (int j = 3; j < a_machine_resources.get(0).length; j++) {
				machineResources[i][2*T+j-3] = Double.parseDouble(a_machine_resources.get(i)[j]);
			}
			for (int j = 0; j < machineResources[0].length; j++) {
				machineResourcesUsed[i][j] = 0;
			}
		}
		
		/**
		 * 初始化instances
		 */
        instancesIn = new LinkedHashMap<String, MyPair<Integer,Integer>>();//instName-machindex
        instancesOut = new ArrayList<MyPair<String,Integer>>();
        initConfict = new ArrayList<MyPair<String,Integer>>();
		for (int i = 0; i < a_instance_deploy.size(); i++) {
			MyPair<String, Integer> temp = new  MyPair<String, Integer>();
			MyPair<Integer, Integer> tempIn = new  MyPair<Integer, Integer>();
			String instName = a_instance_deploy.get(i)[0];
		    int appIndex = Integer.parseInt(a_instance_deploy.get(i)[1].substring(4, a_instance_deploy.get(i)[1].length()))-1;
		    temp.setFirst(instName);
			temp.setSecond(appIndex);
			
			if(!"".equals(a_instance_deploy.get(i)[2])){
				int machIndex = Integer.parseInt(a_instance_deploy.get(i)[2].substring(8,a_instance_deploy.get(i)[2].length()))-1;
				tempIn.setFirst(appIndex);
				tempIn.setSecond(machIndex);
				if(!toMachineHalf(instName,appIndex, machIndex)){
					initConfict.add(temp);
					toMachine(instName,appIndex, machIndex,false);
					instancesIn.put(instName,tempIn);
				}else{
					instancesIn.put(instName,tempIn);
				}
			}else{
				instancesOut.add(temp);
			}
		}
		initIn = instancesIn.size();
		moveNum = initConfict.size();
		System.out.println("in:"+instancesIn.size()+",out:"+instancesOut.size()+",conf:"+initConfict.size());
		System.out.println();
	}
	 public static void pickInstance(String inst,int machIndex){
	        if (!instancesIn.containsKey(inst)) return;
	        int appIt       = instancesIn.get(inst).getFirst();
	        int fromMachine = instancesIn.get(inst).getSecond();
	        // 更新machineHasApp
	        Map<Integer, Integer> fromHasApp = machines.get(fromMachine).getHasApps();
	        fromHasApp.put(appIt, fromHasApp.get(appIt) - 1);
	        if (fromHasApp.get(appIt) <= 0)
	            fromHasApp.remove(appIt);
	        // 更新machineResourcesUsed
	        for (int i = 0; i < machineResourcesUsed[0].length; i++)
	            machineResourcesUsed[fromMachine][i] -= appResources[appIt][i];
	        // 更新inst2Machine
	        instancesIn.remove(inst);
	   }

	/**
	 * 
	 * 
	 * @param appId
	 * @param machineId
	 * @return 判断某个实例是否能放到某个机器，如果能放的话将其放入，并更新machineResourcesUsed
	 */
	public static boolean toMachine(String inst,int appIndex,int machIndex){
        return toMachine(inst, appIndex,machIndex, true);
    }
	public static boolean toMachine(String inst,int appIndex,int machIndex,boolean docheck){
		Map<Integer, Integer> hasApp = new LinkedHashMap <Integer, Integer>();
		hasApp = machines.get(machIndex).getHasApps();
        if(docheck){
	        // 检查互斥规则
	        int nowHas = 0;
	        if (hasApp.containsKey(appIndex))
	            nowHas = hasApp.get(appIndex);
	        for (Integer inApp : hasApp.keySet()) {//检测<inApp,outApp ,k>
	            if (hasApp.get(inApp) <= 0) continue;
	            if (!apps.get(inApp).getCompatibleApp().containsKey(appIndex)) continue;// 不与当前矛盾，则不用管<app1,app2,k>;
	            if (nowHas + 1 > apps.get(inApp).getCompatibleApp().get(appIndex)) {
	                return false ;
	            }
	        }
	        for (Integer checkApp : hasApp.keySet()) {//检测<outApp,inApp ,k>
	        	if (!apps.get(appIndex).getCompatibleApp().containsKey(checkApp)) continue;
	            if (hasApp.get(checkApp) > apps.get(appIndex).getCompatibleApp().get(checkApp)) {
	            	return false; 
	            }
	        }
	        // 检查资源限制
	        for (int i = 0; i < machineResources[0].length; i++){
        		if (machineResourcesUsed[machIndex][i] + appResources[appIndex][i] -  machineResources[machIndex][i] > 0) {
	            	return false;  
	            }
	        }
        }
        
        if (!hasApp.containsKey(appIndex))hasApp.put(appIndex, 0);
        hasApp.put(appIndex, hasApp.get(appIndex) + 1);
        
        for (int i = 0; i < machineResources[0].length; i++){
            machineResourcesUsed[machIndex][i] += appResources[appIndex][i];
        }
        return true;
	}
	public boolean toMachineHalf(String instName, int appIndex, int machIndex) {
		Map<Integer, Integer> hasApp = new LinkedHashMap <Integer, Integer>();
		hasApp = machines.get(machIndex).getHasApps();
   
        // 检查互斥规则
        int nowHas = 0;
        if (hasApp.containsKey(appIndex))
            nowHas = hasApp.get(appIndex);
        for (Integer inApp : hasApp.keySet()) {//检测<inApp,outApp ,k>
            if (hasApp.get(inApp) <= 0) continue;
            if (!apps.get(inApp).getCompatibleApp().containsKey(appIndex)) continue;// 不与当前矛盾，则不用管<app1,app2,k>;
            if (nowHas + 1 > apps.get(inApp).getCompatibleApp().get(appIndex)) {
                return false ;
            }
        }
        for (Integer checkApp : hasApp.keySet()) {//检测<outApp,inApp ,k>
        	if (!apps.get(appIndex).getCompatibleApp().containsKey(checkApp)) continue;
            if (hasApp.get(checkApp) > apps.get(appIndex).getCompatibleApp().get(checkApp)) {
            	return false; 
            }
        }
        double avgCpuRate = 0;
        
        // 检查资源限制
        for (int i = 0; i < machineResources[0].length; i++){
        	double res = machineResourcesUsed[machIndex][i] + appResources[appIndex][i];
        	if(i<T){
        		avgCpuRate += res/machineResources[machIndex][i];
        	}
    		if (res -  machineResources[machIndex][i] > 0) {
            	return false;  
            }
        }
        avgCpuRate /= T;
        if(avgCpuRate > avgUseAge){
        	return false;
        }
        
        if (!hasApp.containsKey(appIndex))hasApp.put(appIndex, 0);
        hasApp.put(appIndex, hasApp.get(appIndex) + 1);
        
        for (int i = 0; i < machineResources[0].length; i++){
            machineResourcesUsed[machIndex][i] += appResources[appIndex][i];
        }
        return true;
	
	}
	
	public  void putInst2() {
		int machIndexE = machines.size() - 1;
		int currentMachIndex = machIndexE;
		//优先部署冲突的
		for(int i = 0;i < initConfict.size();i++){
			
			MyPair<Integer,Integer> tempIn = new MyPair<Integer, Integer>(); 
			String instanceId = initConfict.get(i).getFirst();
			int appIndex = initConfict.get(i).getSecond();
			tempIn.setFirst(appIndex);
			pickInstance(instanceId, currentMachIndex);
			int num = 0;
			int tempIndex = currentMachIndex;;
			while(true){//一直到能装下
				if(toMachineHalf(instanceId,appIndex, currentMachIndex)){
					tempIn.setSecond(currentMachIndex);
					instancesIn.put(instanceId,tempIn);
					initConfict.remove(i--);
					break;
				} 
			    if(num++==0){
			    	currentMachIndex = machIndexE;//第一次进来将指针指回第一个机器
			    	continue;
				}
				 currentMachIndex--;//递减
			}
			if(currentMachIndex < tempIndex)continue;//如果之前的箱子都装不进去，就不变动指针
			currentMachIndex = tempIndex;//装进去以后，就让指针回归原处
		}

		//部署大disk进1024
		for(int i = 0;i < instancesOut.size();i++){
			MyPair<Integer,Integer> tempIn = new MyPair<Integer, Integer>(); 
			String instanceId = instancesOut.get(i).getFirst();
			int appIndex = instancesOut.get(i).getSecond();
			tempIn.setFirst(appIndex);
			double d = appResources[appIndex][2*T];
			if(d != 1024.0) continue;
			int num = 0;
			int tempIndex = currentMachIndex;;
			while(true){//一直到能装下
				if(toMachineHalf(instanceId,appIndex, currentMachIndex)){
					tempIn.setSecond(currentMachIndex);
					instancesIn.put(instanceId,tempIn);
					instancesOut.remove(i--);
					break;
				} 
			    if(num++==0){
			    	currentMachIndex = machIndexE;//第一次进来将指针指回第一个机器
			    	continue;
				}
				 currentMachIndex--;//递减
			}
			if(currentMachIndex < tempIndex)continue;//如果之前的箱子都装不进去，就不变动指针
			currentMachIndex = tempIndex;//装进去以后，就让指针回归原处
		}
		//2.将67个disk为167的instance，两两为一组，放入disk为1024的服务器中的里面
		//int machIndexEnd = machIndex;
		for (int i = 0;i < instancesOut.size();i++) {
			MyPair<Integer,Integer> tempIn = new MyPair<Integer, Integer>(); 
			String instanceId = instancesOut.get(i).getFirst();
			int appIndex = instancesOut.get(i).getSecond();
			tempIn.setFirst(appIndex);
			double d = appResources[appIndex][2*T];
			if(d != 167.0) continue;
			int num = 0;
			int tempIndex = currentMachIndex;;
			while(true){//一直到能装下
				if(toMachineHalf(instanceId,appIndex, currentMachIndex)){
					tempIn.setSecond(currentMachIndex);
					instancesIn.put(instanceId,tempIn);
					instancesOut.remove(i--);
					break;
				} 
			    if(num++==0){
			    	currentMachIndex = machIndexE;//第一次进来将指针指回第一个机器
			    	continue;
				}
				 currentMachIndex--;//递减
			}
			if(currentMachIndex < tempIndex)continue;//如果之前的箱子都装不进去，就不变动指针
			currentMachIndex = tempIndex;//装进去以后，就让指针回归原处
		}
		
	
		//3.部署几个特殊的实例：无法放入小容量的机器中
		List <MyPair<String,Integer>> special = new ArrayList<MyPair<String,Integer>>();
		for (int i = 0; i < instancesOut.size(); i++) {
			int appIndex = instancesOut.get(i).getSecond();
			double cpuSum = 0;
			double memSum = 0;
			double cpuAvg = 0;
			double memAvg = 0;
			for (int j = 0; j < 2 * T; j++) {
				if(j < T){
					cpuSum += appResources[appIndex][j];
				}else{
					memSum += appResources[appIndex][j];
				}
			}
			cpuAvg = cpuSum / T;
			memAvg = memSum / T;
			if(cpuAvg >= 16 || memAvg > 64 || appResources[appIndex][196] >= 600){
				special.add(instancesOut.get(i));
				instancesOut.remove(i--);
			}
		}
		for (int i = 0; i < special.size(); i++) {
			MyPair<Integer,Integer> tempIn = new MyPair<Integer, Integer>(); 
			String instanceId = special.get(i).getFirst();
			int appIndex = special.get(i).getSecond();
			tempIn.setFirst(appIndex);
			int num = 0;
			int tempIndex = currentMachIndex;;
			while(true){//一直到能装下
				if(toMachineHalf(instanceId,appIndex, currentMachIndex)){
					tempIn.setSecond(currentMachIndex);
					instancesIn.put(instanceId,tempIn);
					special.remove(i--);
					break;
				} 
			    if(num++==0){
			    	currentMachIndex = machIndexE;//第一次进来将指针指回第一个机器
			    	continue;
				}
				 currentMachIndex--;//递减
			}
			if(currentMachIndex < tempIndex)continue;//如果之前的箱子都装不进去，就不变动指针
			currentMachIndex = tempIndex;//装进去以后，就让指针回归原处
		}		
		for (int i = 0; i < instancesOut.size(); i++) {//开始装箱
			MyPair<Integer,Integer> tempIn = new MyPair<Integer, Integer>(); 
			String instanceId = instancesOut.get(i).getFirst();
			int appIndex = instancesOut.get(i).getSecond();
			tempIn.setFirst(appIndex);
			int num = 0;
			int tempIndex = currentMachIndex;;
			while(true){//一直到能装下
				if(toMachineHalf(instanceId,appIndex, currentMachIndex)){
					tempIn.setSecond(currentMachIndex);
					instancesIn.put(instanceId,tempIn);
					System.out.println(instancesIn.size());
					instancesOut.remove(i--);
					break;
				} 
			    if(num++==0){
			    	currentMachIndex = machIndexE;//第一次进来将指针指回第一个机器
			    	continue;
				}
				 currentMachIndex--;//递减
			}
			if(currentMachIndex < tempIndex)continue;//如果之前的箱子都装不进去，就不变动指针
			currentMachIndex = tempIndex;//装进去以后，就让指针回归原处
		}
    	System.out.println("in:"+instancesIn.size()+"个实例（？=68219）,out:"+instancesOut.size()+",score:"+getScore()+",index:"+currentMachIndex);	
	}
    /**
     * 数据分析
     */
	  public static void write(){
	    	/**
	    	 * app的cpu和mem分时平均数
	    	 */
			Map <Double,String> appMEM = new TreeMap<Double,String>();
			Map <Double,String> appCPU = new TreeMap<Double,String>();
			for (int i = 0; i < appResources.length; i++) {
				double cpuS = 0;
				double memS = 0;
				for (int j = 0; j < 2*T; j++) {
					if(j < T){
						cpuS +=appResources[i][j];
					}else{
						memS +=appResources[i][j];
					}
				}
				if(appCPU.containsKey(cpuS/T)){
					appCPU.put(cpuS/T, apps.get(i).getAppId()+","+appCPU.get(cpuS/T));
				}else{
					appCPU.put(cpuS/T, apps.get(i).getAppId());
				}
				if(appMEM.containsKey(memS/T)){
					appMEM.put(memS/T, apps.get(i).getAppId()+","+appMEM.get(memS/T));
				}else{
					appMEM.put(memS/T, apps.get(i).getAppId());
				}
			}
		    String cpu[] = new String[9338];
		    int cpuu = 0;
		    String mem[] = new String[9338];
		    int meme = 0;
			for (Double i:appCPU.keySet()) {
				cpu[cpuu++] = appCPU.get(i)+","+i;
			}
			for (Double i:appMEM.keySet()) {
				mem[meme++] = appMEM.get(i)+","+i;
			}
			FileUtil.write("C:/Users/zyf/Desktop/阿里数据分析/mem.csv", mem, false);
			FileUtil.write("C:/Users/zyf/Desktop/阿里数据分析/cpu.csv", cpu,false);
			/**
			 * 统计实例 disk不同用量个数
			 */
			Map <Double,Double>  disks= new HashMap<Double,Double>();
			for(int i = 0;i < instancesOut.size();i++){
				double d = appResources[instancesOut.get(i).getSecond()][2*T];
				if(disks.containsKey(d)){
					disks.put(d, disks.get(d)+1);
				}else{
					disks.put(d, 1.0);
				}
			}
			String disk[] = new String[disks.size()];
			int diskk = 0;
			for (Double i : disks.keySet()) {
				disk[diskk++] = i+","+disks.get(i);
			}
		    FileUtil.write("C:/Users/zyf/Desktop/阿里数据分析/instanceDisk.csv", disk,false);
	    }
	
}
