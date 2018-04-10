package test.java.org.matsim.run;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.*;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.roadpricing.RoadPricingScheme;
import org.matsim.roadpricing.RoadPricingTravelDisutilityFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OnTheFlyServer;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;
import org.matsim.vis.snapshotwriters.VisMobsim;
import org.matsim.contrib.otfvis.*;
import org.matsim.contrib.transEnergySim.events.EventManager;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.controller.*;
import org.matsim.withinday.replanning.identifiers.ActivityEndIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.InitialIdentifierImplFactory;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.LegStartedIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.filter.ActivityStartingFilter;
import org.matsim.withinday.replanning.identifiers.filter.ActivityStartingFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.EarliestLinkExitTimeFilter;
import org.matsim.withinday.replanning.identifiers.filter.EarliestLinkExitTimeFilterFactory;
import org.matsim.withinday.replanning.identifiers.filter.ProbabilityFilterFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringActivityIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifier;
import org.matsim.withinday.replanning.identifiers.interfaces.InitialIdentifierFactory;
import org.matsim.withinday.replanning.identifiers.tools.ActivityReplanningMap;
import org.matsim.withinday.replanning.identifiers.tools.LinkReplanningMap;
import org.matsim.withinday.replanning.replanners.CurrentLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.InitialReplannerFactory;
import org.matsim.withinday.replanning.replanners.NextLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;
import org.matsim.withinday.trafficmonitoring.LinkEnteredProvider;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;


import com.google.common.primitives.Ints;



@Singleton
public final class runTest2 implements StartupListener, MobsimAfterSimStepListener {

	private double pDuringLegReplanning = 1.0;
	private DuringLegIdentifierFactory duringLegIdentifierFactory;
	private DuringLegAgentSelector duringLegIdentifier;
	private WithinDayDuringLegReplannerFactory duringLegReplannerFactory;
	private ProbabilityFilterFactory duringLegProbabilityFilterFactory;
	private ActivityStartingFilterFactory activityStartingFilterFactory;

	
//	private VisMobsim visMobsim;
//	public Map<Id<Person>, MobsimAgent> getMobsimAgents() {
//		return visMobsim.getAgents();}
	
	@Inject private Scenario scenario;
	@Inject private Config config;
	@Inject private Provider<TripRouter> tripRouterProvider;
	@Inject private MobsimDataProvider mobsimDataProvider;
	@Inject private WithinDayEngine withinDayEngine;
//	@Inject private ActivityReplanningMap activityReplanningMap;
	@Inject private LinkReplanningMap linkReplanningMap;
	@Inject private LeastCostPathCalculatorFactory pathCalculatorFactory;
//	@Inject private Map<String,TravelDisutilityFactory> travelDisutilityFactories ;		//added RandomizingTimeDistance
	@Inject private Map<String,TravelTime> travelTimes ;
	@Inject private RoadPricingScheme roadPricingScheme;
	
	/*
	 * ===================================================================
	 * main
	 * ===================================================================
	 */
	
	public static void main(final String[] args) {
		
		Config config ;
		if ( args.length==0 || args[0]=="" ) {
			config = ConfigUtils.loadConfig("C:/Users/avandeloo/Documents/TU/Research/Workspace/Delft/input/configWD_TDN.xml", new RoadPricingConfigGroup(), new OTFVisConfigGroup()) ;
			config.controler().setLastIteration(0);
			config.controler().setOverwriteFileSetting( OverwriteFileSetting.deleteDirectoryIfExists );
		} else {
			config = ConfigUtils.loadConfig(args[0]) ;
		}
		
		Scenario scenario = ScenarioUtils.loadScenario( config) ;
		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class); 
//		otfVisConfigGroup.setMapOverlayMode(true); 
	
		final Controler controler = new Controler(scenario);
		controler.addOverridingModule(new RoadPricingModule());
		controler.addOverridingModule(new OTFVisLiveModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				install(new WithinDayModule());
				addControlerListenerBinding().to(runTest2.class);
				install(new TravelDisutilityModule());
//				addMobsimListenerBinding().to(MobsimDataProvider.class);
//				this.addEventHandlerBinding().toInstance( new MyEventHandler3() );
//				this.addEventHandlerBinding().toInstance( new CongestionDetectionEventHandler( scenario.getNetwork() )  );
		}
		});
		controler.run();
	}

	/*
	 * ===================================================================
	 * Negotiation and replanners
	 * ===================================================================
	 */

	@Override
	public void notifyStartup(StartupEvent event) {
		this.initReplanners();
	}

	/*
	All planned routes are updated every time step and constantly monitored. The number of routes that include the same link at the same time will be summed up.
	This means that agents with intersecting trajectories will be counted for each link. If the number of agents on the link would mean that the preset emission-limit will be reached,
	a small toll is added to that specific link for that specific timeframe. If at the next time step the situation has not changed – the emission on that link would still be above limit –,
	the toll level will be increased. That way, the agents that can most easily/cheaply divert will do so first.
	*/
/*	
	//fill data array with of minutes of the day
	public static void fillData(Map<Integer, Integer> linktimes) {
		for (int time=0;time<1440;time++) {			
			linktimes.put(time,1);
		}
	}
*/	
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e){
		        
		//create list of links used for each minute during the day (or other specified time interval)

		//count predicted emission per link for each interval

		//check if emissions are above limit
		
		//increase toll for critical link at specific interval 
		
        this.initReplanners();
	}
	
/*	
	public List<Id<Link>> getRoute(int id) {
		Person person = 
		Leg leg = (Leg) getCurrentPlanElement();
		NetworkRoute route = (NetworkRoute) leg.getRoute();
		return (List<Id<Link>>) route.getLinkIds();
	}
*/	
	
	private void initReplanners() {

		// get input from config
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = ConfigUtils.addOrGetModule(config, PlanCalcScoreConfigGroup.GROUP_NAME, PlanCalcScoreConfigGroup.class);
		RouteFactories routeFactory = ((PopulationFactory) this.scenario.getPopulation().getFactory()).getRouteFactories() ;
		Network network = scenario.getNetwork() ;
		// collect travel times
		TravelTime travelTime = travelTimes.get( TransportMode.car ) ;
		// create path calculator that considers all disutilities incl. roadpricing
		TravelDisutilityFactory travelDisutilityFactory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, planCalcScoreConfigGroup);
		RoadPricingTravelDisutilityFactory roadPricingTravelDisutilityFactory = new RoadPricingTravelDisutilityFactory(travelDisutilityFactory, roadPricingScheme, config);
		TravelDisutility travelDisutility = roadPricingTravelDisutilityFactory.createTravelDisutility(travelTime ) ;
		LeastCostPathCalculator pathCalculator = pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTime ) ;
		
		//DURING LEG REPLANNER
		this.duringLegIdentifierFactory = new LeaveLinkIdentifierFactory(this.linkReplanningMap, mobsimDataProvider);
//		this.duringLegIdentifierFactory = new LegStartedIdentifierFactory(this.linkReplanningMap, this.mobsimDataProvider);
		this.duringLegProbabilityFilterFactory = new ProbabilityFilterFactory(this.pDuringLegReplanning);
//		this.activityStartingFilterFactory = new ActivityStartingFilterFactory(mobsimDataProvider);
		this.duringLegIdentifierFactory.addAgentFilterFactory(this.duringLegProbabilityFilterFactory);
//		this.duringLegIdentifierFactory.addAgentFilterFactory(this.activityStartingFilterFactory);
		this.duringLegIdentifier = this.duringLegIdentifierFactory.createIdentifier();	
//		this.duringLegIdentifier.addAgentFilter(activityStartingFilter);
		this.duringLegReplannerFactory = new CurrentLegReplannerFactory(scenario, withinDayEngine, pathCalculator, routeFactory );
		this.duringLegReplannerFactory.addIdentifier(this.duringLegIdentifier);
		this.withinDayEngine.addDuringLegReplannerFactory(this.duringLegReplannerFactory);
	}
	
	
}