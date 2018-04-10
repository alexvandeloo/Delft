package test.java.org.matsim.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.roadpricing.*;
import org.matsim.contrib.otfvis.*;
import org.matsim.vis.otfvis.*;
import org.matsim.core.mobsim.*;


public class runTest {

	public static void main(String[] args)
	{	
		Config 
			config = ConfigUtils.loadConfig("C:/Users/avandeloo/Documents/TU/Research/Workspace/Delft/input/config.xml", new RoadPricingConfigGroup(),
					new OTFVisConfigGroup() ) ;
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
			
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		Controler controler = new Controler(scenario) ;
		controler.addOverridingModule(new RoadPricingModule());
//		controler.addOverridingModule(new OTFVisLiveModule());
		controler.run();
	} 
	
}
