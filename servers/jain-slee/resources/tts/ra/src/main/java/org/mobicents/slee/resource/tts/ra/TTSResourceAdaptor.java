package org.mobicents.slee.resource.tts.ra;

import java.io.Serializable;
import java.util.HashMap;

import javax.naming.NamingException;
import javax.slee.Address;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.BootstrapContext;
import javax.slee.resource.FailureReason;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorTypeID;
import javax.slee.resource.ResourceException;
import javax.slee.resource.SleeEndpoint;

import org.apache.log4j.Logger;
import org.mobicents.slee.container.SleeContainer;
import org.mobicents.slee.resource.ResourceAdaptorActivityContextInterfaceFactory;
import org.mobicents.slee.resource.ResourceAdaptorEntity;
import org.mobicents.slee.resource.tts.ratype.TTSActivityContextInterfaceFactory;
import org.mobicents.slee.resource.tts.ratype.TTSProvider;

public class TTSResourceAdaptor implements ResourceAdaptor, Serializable {

	private static transient Logger logger = Logger
			.getLogger(TTSResourceAdaptor.class);

	private transient BootstrapContext bootstrapContext = null;

	private transient SleeEndpoint sleeEndpoint = null;

	private transient EventLookupFacility eventLookup = null;

	private transient HashMap activities = null;

	private transient TTSProvider raProvider = null;

	private transient TTSActivityContextInterfaceFactory acif = null;

	public void activityEnded(ActivityHandle activityHandle) {
		activities.remove(activityHandle);
		logger.debug("TTSResourceAdaptor.activityEnded() called.");

	}

	public void activityUnreferenced(ActivityHandle arg0) {
		logger.debug("TTSResourceAdaptor.activityUnreferenced() called.");

	}

	public void entityActivated() throws ResourceException {
		logger.debug("TTSResourceAdaptor.entityActivated() called.");
		try {
			logger.debug("Starting ");
			try {
				raProvider = new TTSProviderImpl(this);
				initializeNamingContext();
			} catch (Exception ex) {
				logger
						.error("TTSResourceAdaptor.start(): Exception caught! ");
				ex.printStackTrace();
				throw new ResourceException(ex.getMessage());
			}
			activities = new HashMap();
		} catch (ResourceException e) {
			e.printStackTrace();
			throw new javax.slee.resource.ResourceException(
					"TTSResourceAdaptor.entityActivated(): Failed to activate TTS Resource Adaptor!",
					e);
		}

	}

	public void entityCreated(BootstrapContext bootstrapContext) throws ResourceException {
		logger.debug("TTSResourceAdaptor.entityCreated() called.");
		this.bootstrapContext = bootstrapContext;
		this.sleeEndpoint = bootstrapContext.getSleeEndpoint();
		this.eventLookup = bootstrapContext.getEventLookupFacility();

	}

	public void entityDeactivated() {
		logger.debug("TTSResourceAdaptor.entityDeactivated() called.");
		try {
			cleanNamingContext();
		} catch (NamingException e) {
			logger.error("Cannot unbind naming context");
		}
		logger.debug("TTS Resource Adaptor stopped.");

	}

	public void entityDeactivating() {
		logger.debug("TTSResourceAdaptor.entityDeactivating() called.");

	}

	public void entityRemoved() {
		logger.debug("TTSResourceAdaptor.entityRemoved() called.");

	}

	public void eventProcessingFailed(ActivityHandle arg0, Object arg1,
			int arg2, Address arg3, int arg4, FailureReason arg5) {
		logger.debug("TTSResourceAdaptor.eventProcessingFailed() called.");

	}

	public void eventProcessingSuccessful(ActivityHandle arg0, Object arg1,
			int arg2, Address arg3, int arg4) {
		logger.debug("TTSResourceAdaptor.eventProcessingSuccessful() called.");

	}

	public Object getActivity(ActivityHandle activityHandle) {
		logger.debug("TTSResourceAdaptor.getActivity() called.");
		return activities.get(activityHandle);
	}

	public ActivityHandle getActivityHandle(Object arg0) {
		logger.debug("TTSResourceAdaptor.getActivityHandle(obj) called.");
		return null;
	}

	public Marshaler getMarshaler() {
		logger.debug("TTSResourceAdaptor.getMarshaler() called.");
		return null;
	}

	public Object getSBBResourceAdaptorInterface(String str) {
		logger.debug("TTSResourceAdaptor.getSBBResourceAdapterInterface(" + str
				+ ") called.");
		return raProvider;
	}

	public void queryLiveness(ActivityHandle arg0) {
		logger.debug("TTSResourceAdaptor.queryLifeness() called.");

	}

	public void serviceActivated(String arg0) {
		logger.debug("TTSResourceAdaptor.serviceActivated() called.");

	}

	public void serviceDeactivated(String arg0) {
		logger.debug("TTSResourceAdaptor.serviceDeactivated() called.");

	}

	public void serviceInstalled(String arg0, int[] arg1, String[] arg2) {
		logger.debug("TTSResourceAdaptor.serviceInstalled() called.");

	}

	public void serviceUninstalled(String arg0) {
		logger.debug("TTSResourceAdaptor.serviceUninstalled() called.");

	}

	// set up the JNDI naming context
	private void initializeNamingContext() throws Exception {
		// get the reference to the SLEE container from JNDI
		SleeContainer container = SleeContainer.lookupFromJndi();
		// get the entities name
		String entityName = bootstrapContext.getEntityName();

		final ResourceAdaptorEntity resourceAdaptorEntity = container
				.getResourceManagement().getResourceAdaptorEntity(entityName);

		ResourceAdaptorTypeID raTypeId = resourceAdaptorEntity
				.getInstalledResourceAdaptor().getRaType()
				.getResourceAdaptorTypeID();

		// create the ActivityContextInterfaceFactory
		acif = new TTSActivityContextInterfaceFactoryImpl(resourceAdaptorEntity
				.getServiceContainer(), entityName);

		// set the ActivityContextInterfaceFactory
		resourceAdaptorEntity.getServiceContainer().getResourceManagement()
				.getActivityContextInterfaceFactories().put(raTypeId, (ResourceAdaptorActivityContextInterfaceFactory)acif);

		try {
			if (this.acif != null) {
				// parse the string =
				// java:slee/resources/TTSResourceAdaptor/ttsraacif
				String jndiName = ((ResourceAdaptorActivityContextInterfaceFactory) acif)
						.getJndiName();
				int begind = jndiName.indexOf(':');
				int toind = jndiName.lastIndexOf('/');
				String prefix = jndiName.substring(begind + 1, toind);
				String name = jndiName.substring(toind + 1);
				logger.debug("jndiName prefix =" + prefix + "; jndiName = "
						+ name);
				SleeContainer.registerWithJndi(prefix, name, this.acif);
			}
		} catch (IndexOutOfBoundsException e) {
			// not register with JNDI
			logger.debug(e);
		}
	}

	// clean the JNDI naming context
	private void cleanNamingContext() throws NamingException {
		try {
			if (this.acif != null) {
				// parse the string =
				// java:slee/resources/TTSResourceAdaptor/ttsraacif
				String jndiName = ((ResourceAdaptorActivityContextInterfaceFactory) this.acif)
						.getJndiName();
				// remove "java:" prefix
				int begind = jndiName.indexOf(':');
				String javaJNDIName = jndiName.substring(begind + 1);
				logger.debug("JNDI name to unregister: " + javaJNDIName);
				SleeContainer.unregisterWithJndi(javaJNDIName);
				logger.debug("JNDI name unregistered.");
			}
		} catch (IndexOutOfBoundsException e) {
			logger.debug(e);
		}
	}

	SleeEndpoint getSleeEndpoint() {
		return sleeEndpoint;
	}

}
