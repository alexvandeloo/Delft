/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package test.java.org.matsim.run;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.Facility;
import org.matsim.roadpricing.RoadPricingConfigGroup;
import org.matsim.roadpricing.RoadPricingModule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.withinday.controller.WithinDayModule;
import org.matsim.withinday.mobsim.MobsimDataProvider;
import org.matsim.withinday.mobsim.WithinDayEngine;
import org.matsim.withinday.replanning.identifiers.LeaveLinkIdentifierFactory;
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
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringActivityReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplannerFactory;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplannerFactory;

import javax.inject.Inject;
import java.net.URL;
import java.util.Map;

public class runTest3 {
	
	public static void main(final String[] args) {


		Config config = ConfigUtils.loadConfig("C:/Users/avandeloo/Documents/TU/Research/Workspace/Delft/input/configWD_TDN.xml", new RoadPricingConfigGroup(), new OTFVisConfigGroup()) ;
		Scenario scenario = ScenarioUtils.loadScenario( config) ;
		// set some config stuff:
		config.controler().setLastIteration(0) ;
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		// OTFVis Config:
		OTFVisConfigGroup otfVisConfigGroup = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME, OTFVisConfigGroup.class); 

		// controler:
		final Controler controler = new Controler( scenario ) ;
		controler.addOverridingModule(new RoadPricingModule());
		controler.addOverridingModule(new OTFVisLiveModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {

					@Inject Scenario scenario;
					@Inject EventsManager eventsManager;

					@Override
					public Mobsim get() {
//						scenario.getConfig().qsim().setEndTime(25 * 60 * 60);
//						scenario.getConfig().controler().setLastIteration(0);
//						scenario.getPopulation().getPersons().clear();
						final QSim qsim = QSimUtils.createDefaultQSim(scenario, eventsManager);
						qsim.addAgentSource(new AgentSource() {
							@Override
							public void insertAgentsIntoMobsim() {
								// router.  In order to be thread safe, one needs one router per agent.  Since, on the other hand, routers are heavy-weight objects,
								// this will not scale.  For large numbers of replanning agents, one needs to think of a better software architecture here. kai, nov'14
								final TripRouter router = controler.getTripRouterProvider().get();

								// guidance.  Will need one instance per agent in order to be thread safe
								final MyGuidance guidance = new MyGuidance(router, scenario);

								// insert traveler agent:
								final MobsimAgent ag = new MyMobsimAgent(guidance, qsim.getSimTimer(), scenario);
								qsim.insertAgentIntoMobsim(ag);

								// insert vehicle:
								final Vehicle vehicle = VehicleUtils.getFactory().createVehicle(Id.create(ag.getId(), Vehicle.class), VehicleUtils.getDefaultVehicleType());
								final Id<Link> linkId4VehicleInsertion = Id.createLinkId(1);
								qsim.createAndParkVehicleOnLink(vehicle, linkId4VehicleInsertion);
							}
						});
						return qsim;
					}
				});
			}
		});

		controler.run();

	}

}


/**
 * See {@link tutorial.mobsim.ownMobsimAgentWithPerception} and {@link tutorial.mobsim.ownMobsimAgentUsingRouter} for
 * more complete examples.
 *
 * @author nagel
 *
 */
/*
class MyAgent implements MobsimDriverAgent {
	private static Logger log = Logger.getLogger("MyAgent") ;

	private MobsimVehicle vehicle;
	private Scenario sc;
	private EventsManager ev;
	private Id<Link> currentLinkId;
	private Id<Person> myId;
	private State state = State.ACTIVITY;
	private Netsim netsim;
	private Id<Link> destinationLinkId = Id.create("dummy", Link.class);
	private Id<Vehicle> plannedVehicleId ;

	private double activityEndTime = 1. ;

	MyAgent( Scenario sc, EventsManager ev, Netsim netsim, Id<Link> startLinkId, MobsimVehicle veh ) {
		log.info( "calling MyAgent" ) ;
		this.sc = sc ;
		this.ev = ev ;
		this.myId = Id.create("testveh", Person.class) ;
		this.netsim = netsim ;
		this.currentLinkId = startLinkId ;
		this.plannedVehicleId = veh.getId() ;
	}

	@Override
	public void setStateToAbort(double now) {
		this.state = State.ABORT ;
		log.info( "calling abort; setting state to: " + this.state ) ;
	}

	@Override
	public void endActivityAndComputeNextState(double now) {
		this.state = State.LEG ; // want to move
		log.info( "calling endActivityAndComputeNextState; setting state to: " + this.state ) ;
	}

	@Override
	public void endLegAndComputeNextState(double now) {
		this.state = State.ACTIVITY ;
		this.activityEndTime = Double.POSITIVE_INFINITY ;
		log.info( "calling endLegAndComputeNextState; setting state to: " + this.state ) ;
	}

	@Override
	public double getActivityEndTime() {
		log.info ("calling getActivityEndTime; answer: " + this.activityEndTime ) ;
		return this.activityEndTime ;
	}

	@Override
	public Double getExpectedTravelTime() {
		return 0. ;  // what does this matter for?
	}

	@Override
	public Double getExpectedTravelDistance() {
		return null;
	}

	@Override
	public String getMode() {
		return TransportMode.car ; // either car or nothing
	}

	@Override
	public State getState() {
		log.info( "calling getState; answer: " + this.state ) ;
		return this.state ;
	}

	@Override
	public void notifyArrivalOnLinkByNonNetworkMode(Id<Link> linkId) {
		this.currentLinkId = linkId ;
	}

	@Override
	public Id<Link> getCurrentLinkId() {
		return this.currentLinkId ;
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return this.destinationLinkId ;
	}

	@Override
	public Id<Person> getId() {
		return this.myId ;
	}

	@Override
	public Id<Link> chooseNextLinkId() {
		Link currentLink = sc.getNetwork().getLinks().get(this.currentLinkId) ;
		Object[] outLinks = currentLink.getToNode().getOutLinks().keySet().toArray() ;
		int idx = MatsimRandom.getRandom().nextInt(outLinks.length) ;
		if ( this.netsim.getSimTimer().getTimeOfDay() < 24.*3600 ) {
			return (Id<Link>) outLinks[idx] ;
		} else {
			this.destinationLinkId  = (Id<Link>) outLinks[idx] ;
			return null ;
		}
	}
	
	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return this.plannedVehicleId ;
	}

	@Override
	public MobsimVehicle getVehicle() {
		return this.vehicle ;
	}

	@Override
	public void notifyMoveOverNode(Id<Link> newLinkId) {
		this.currentLinkId = newLinkId ;
	}

	@Override
	public void setVehicle(MobsimVehicle veh) {
		this.vehicle = veh ;
	}

	@Override
	public boolean isWantingToArriveOnCurrentLink() {
		return false ;
	}

	@Override
	public Facility<? extends Facility<?>> getCurrentFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Facility<? extends Facility<?>> getDestinationFacility() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

}
*/
