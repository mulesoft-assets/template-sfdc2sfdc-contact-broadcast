package org.mule.kicks.integration;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.kicks.builders.SfdcObjectBuilder.aContact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import java.util.Date;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.context.notification.NotificationException;
import org.mule.kicks.builders.SfdcObjectBuilder;
import org.mule.kicks.test.utils.ListenerProbe;
import org.mule.kicks.test.utils.PipelineSynchronizeListener;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.streaming.ConsumerIterator;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transport.NullPayload;

import com.mulesoft.module.batch.api.BatchJobInstance;
import com.mulesoft.module.batch.api.notification.BatchNotification;
import com.mulesoft.module.batch.api.notification.BatchNotificationListener;
import com.mulesoft.module.batch.engine.BatchJobInstanceAdapter;
import com.mulesoft.module.batch.engine.BatchJobInstanceStore;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule Kick that make calls to external systems.
 * 
 * @author miguel.oliva
 */
public class ContactOneWaySyncIT extends AbstractKickTestCase {

	private static SubflowInterceptingChainLifecycleWrapper checkContactflow;
	private static List<Map<String, Object>> createdContactInA = new ArrayList<Map<String, Object>>();

	private static final String POLL_FLOW_NAME = "triggerFlow";
	private static final String KICK_NAME = "ContactOneWaySync";
	private static final int TIMEOUT = 60;

	private final PipelineSynchronizeListener pipelineListener = new PipelineSynchronizeListener(POLL_FLOW_NAME);
	private final Prober workingPollProber = new PollingProber(60000, 1000l);
	
	private Prober prober;
	protected Boolean failed;
	protected BatchJobInstanceStore jobInstanceStore;

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("mule.env", "test");

		// Setting Default Watermark Expression to query SFDC with
		// LastModifiedDate greater than ten seconds before current time
		System.setProperty("watermark.default.expression", "#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");
		System.setProperty("polling.startDelayMillis", "10000");

		// Setting Polling Frecuency to 10 seconds period
		System.setProperty("polling.frequency", "10000");
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {

		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		
		failed = null;
		jobInstanceStore = muleContext.getRegistry()
										.lookupObject(BatchJobInstanceStore.class);
		muleContext.registerListener(new BatchWaitListener());

		
		// Flow to retrieve contacts from target system after syncing
		checkContactflow = getSubFlow("retrieveContactFlow");
		checkContactflow.initialise();

		createEntities();
	}


	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);

		deleteEntities();
	}


	@Test
	public void testMainFlow() throws Exception {

		runSchedulersOnce(POLL_FLOW_NAME);

		waitForPollToRun();

		// Wait for the batch job executed by the poll flow to finish
		awaitJobTermination();
		assertTrue("Batch job was not successful", wasJobSuccessful());
		
		// Assert first object was not synced
		assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContactInA.get(0)));

		// Assert second object was not synced
		assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContactInA.get(1)));

		// Assert third object was created in target system
		Map<String, Object> payload = invokeRetrieveContactFlow(checkContactflow, createdContactInA.get(2));
		assertEquals("The contact should have been sync", createdContactInA.get(2)
																			.get("Email"), payload.get("Email"));

		// Assert fourth object was updated in target system
		final Map<String, Object> fourthContact = createdContactInA.get(3);
		payload = invokeRetrieveContactFlow(checkContactflow, fourthContact);
		assertEquals("The contact should have been sync (Email)", fourthContact.get("Email"), payload.get("Email"));
		assertEquals("The contact should have been sync (FirstName)", fourthContact.get("FirstName"), payload.get("FirstName"));

	}


	@SuppressWarnings("unchecked")
	private Map<String, Object> invokeRetrieveContactFlow(final SubflowInterceptingChainLifecycleWrapper flow, final Map<String, Object> contact) throws Exception {
		final Map<String, Object> contactMap = new HashMap<String, Object>();

		contactMap.put("Email", contact.get("Email"));
		final MuleEvent event = flow.process(getTestEvent(contactMap, MessageExchangePattern.REQUEST_RESPONSE));
		final Object payload = event.getMessage()
									.getPayload();
		System.out.println("Retrieve Contacts Result for: " + contact.get("Email") + " is " + payload);
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, Object>) payload;
		}
	}

	private String buildUniqueEmail(String user) {
		String server = "fakemail";
		String kickName = KICK_NAME;
		String timeStamp = new Long(new Date().getTime()).toString();

		StringBuilder builder = new StringBuilder();
		builder.append(user);
		builder.append(".");
		builder.append(timeStamp);
		builder.append("@");
		builder.append(server);
		builder.append(kickName);
		builder.append(".com");

		return builder.toString();

	}

	private void createEntities() throws InitialisationException, MuleException, Exception {
		// Create object in target system to be updated
		final SubflowInterceptingChainLifecycleWrapper flowB = getSubFlow("createContactFlowB");
		flowB.initialise();
		
		final List<Map<String, Object>> createdContactInB = new ArrayList<Map<String, Object>>();
		// This contact should BE synced (updated) as the mailing country is US and the record exists in the target system
		 createdContactInB.add(aContact().with("FirstName", "Mario")
				.with("LastName", "Bofil")
				.with("Email", buildUniqueEmail("mario"))
				.with("MailingCountry", "United States")
				.build());
		
		flowB.process(getTestEvent(createdContactInB, MessageExchangePattern.REQUEST_RESPONSE));
		
		// Create contacts in source system to be or not to be synced
		final SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createContactFlowA");
		flow.initialise();
		
		// This contact should not be synced as the mailing country is not US, U.S. or United States
		createdContactInA.add(aContact().with("FirstName", "Raul")
				.with("LastName", "Barboza")
				.with("Email", buildUniqueEmail("raul"))
				.with("MailingCountry", "Argentina")
				.build());
		
		// This contact should not be synced as the mailing country is not US,
		// U.S. or United States
		createdContactInA.add(aContact().with("FirstName", "Julian")
				.with("LastName", "Zini")
				.with("Email", buildUniqueEmail("julian"))
				.with("MailingCountry", "Argentina")
				.build());
		
		// This contact should BE synced (inserted) as the mailing country is
		// U.S. and the record doesn't exist in the target system
		createdContactInA.add(aContact().with("FirstName", "Rodolfo")
				.with("LastName", "Regunaga")
				.with("Email", buildUniqueEmail("rodolfo"))
				.with("MailingCountry", "U.S.")
				.build());
		
		// This contact should BE synced (updated) as the mailing country is
		// U.S. and the record exists in the target system
		createdContactInA.add(aContact().with("FirstName", "Marito")
				.with("LastName", "Bonfil")
				.with("Email", createdContactInB.get(0).get("Email"))
				.with("MailingCountry", "United States")
				.build());
		
		final MuleEvent event = flow.process(getTestEvent(createdContactInA, MessageExchangePattern.REQUEST_RESPONSE));
		final List<SaveResult> results = (List<SaveResult>) event.getMessage()
				.getPayload();
		System.out.println("Results from creation in A" + results.toString());
		for (int i = 0; i < results.size(); i++) {
			createdContactInA.get(i)
			.put("Id", results.get(i)
					.getId());
		}
		System.out.println("Results after adding" + createdContactInA.toString());
	}

	private void deleteEntities() throws InitialisationException, MuleException, Exception {
		// Delete the created contacts in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteContactFromAFlow");
		flow.initialise();

		final List<String> idList = new ArrayList<String>();
		for (final Map<String, Object> c : createdContactInA) {
			idList.add(c.get("Id").toString());
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created contacts in B
		flow = getSubFlow("deleteContactFromBFlow");
		flow.initialise();

		idList.clear();
		for (final Map<String, Object> c : createdContactInA) {
			final Map<String, Object> contact = invokeRetrieveContactFlow(checkContactflow, c);
			if (contact != null) {
				idList.add(contact.get("Id").toString());
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}
	
	private void registerListeners() throws NotificationException {
		muleContext.registerListener(pipelineListener);
	}
	
	private void waitForPollToRun() {
		System.out.println("Waiting for poll to run ones...");
		workingPollProber.check(new ListenerProbe(pipelineListener));
		System.out.println("Poll flow done");
	}
	
	protected class BatchWaitListener implements BatchNotificationListener {

		public synchronized void onNotification(ServerNotification notification) {
			final int action = notification.getAction();

			if (action == BatchNotification.JOB_SUCCESSFUL || action == BatchNotification.JOB_STOPPED) {
				failed = false;
			} else if (action == BatchNotification.JOB_PROCESS_RECORDS_FAILED || action == BatchNotification.LOAD_PHASE_FAILED || action == BatchNotification.INPUT_PHASE_FAILED
					|| action == BatchNotification.ON_COMPLETE_FAILED) {

				failed = true;
			}
		}
	}

	protected void awaitJobTermination() throws Exception {
		this.awaitJobTermination(TIMEOUT);
	}

	protected void awaitJobTermination(long timeoutSecs) throws Exception {
		this.prober = new PollingProber(timeoutSecs * 1000, 500);
		this.prober.check(new Probe() {

			@Override
			public boolean isSatisfied() {
				return failed != null;
			}

			@Override
			public String describeFailure() {
				return "batch job timed out";
			}
		});
	}
	
	protected boolean wasJobSuccessful() {
		return this.failed != null ? !this.failed : false;
	}

	protected BatchJobInstanceAdapter getUpdatedInstance(BatchJobInstance jobInstance) {
		return this.jobInstanceStore.getJobInstance(jobInstance.getOwnerJobName(), jobInstance.getId());
	}

}
