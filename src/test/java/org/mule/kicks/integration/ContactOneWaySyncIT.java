package org.mule.kicks.integration;

import static junit.framework.Assert.assertEquals;

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
import org.mule.api.schedule.Scheduler;
import org.mule.api.schedule.Schedulers;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.transport.NullPayload;

import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Mule Kick that make calls to external systems.
 * 
 * @author miguel.oliva
 */
public class ContactOneWaySyncIT extends AbstractKickTestCase {

	private static SubflowInterceptingChainLifecycleWrapper checkContactflow;
	private static List<Map<String, String>> createdContactInA = new ArrayList<Map<String, String>>();
	private static final String POLL_FLOW_NAME = "businessLogicBatch";
	private static final String KICK_NAME = "ContactOneWaySync";
	

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("mule.env", "test");

		// Setting Default Watermark Expression to query SFDC with
		// LastModifiedDate greater than ten seconds before current time
		System.setProperty("watermark.default.expression", "#[groovy: new Date(System.currentTimeMillis() - 10000).format(\"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\", TimeZone.getTimeZone('UTC'))]");

		// Setting Polling Frecuency to 10 seconds period
		System.setProperty("polling.frequency", "10000");
	}

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {

		// Flow to retrieve contacts from target system after syncing
		checkContactflow = getSubFlow("retrieveContactFlow");
		checkContactflow.initialise();

		// Create object in target system to be updated
		final SubflowInterceptingChainLifecycleWrapper flowB = getSubFlow("createContactFlowB");
		flowB.initialise();

		final List<Map<String, String>> createdContactInB = new ArrayList<Map<String, String>>();
		// This contact should BE synced (updated) as the mailing country is US and the record exists in the target system
		createdContactInB.add(aContact().withProperty("FirstName", buildUniqueName(KICK_NAME,"Mario"))
										.withProperty("LastName", buildUniqueName(KICK_NAME,"Bofil"))
										.withProperty("Email", buildUniqueEmail("mario"))
										.withProperty("MailingCountry", "United States")
										.build());

		flowB.process(getTestEvent(createdContactInB, MessageExchangePattern.REQUEST_RESPONSE));

		// Create contacts in source system to be or not to be synced
		final SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("createContactFlowA");
		flow.initialise();

		// This contact should not be synced as the mailing country is not US, U.S. or United States
		createdContactInA.add(aContact().withProperty("FirstName", buildUniqueName(KICK_NAME,"Raul"))
										.withProperty("LastName", buildUniqueName(KICK_NAME,"Barboza"))
										.withProperty("Email", buildUniqueEmail("raul"))
										.withProperty("MailingCountry", "Argentina")
										.build());

		// This contact should not be synced as the mailing country is not US,
		// U.S. or United States
		createdContactInA.add(aContact().withProperty("FirstName", buildUniqueName(KICK_NAME,"Julian"))
										.withProperty("LastName", buildUniqueName(KICK_NAME,"Zini"))
										.withProperty("Email", buildUniqueEmail("julian"))
										.withProperty("MailingCountry", "Argentina")
										.build());

		// This contact should BE synced (inserted) as the mailing country is
		// U.S. and the record doesn't exist in the target system
		createdContactInA.add(aContact().withProperty("FirstName", buildUniqueName(KICK_NAME,"Rodolfo"))
										.withProperty("LastName", buildUniqueName(KICK_NAME,"Regunaga"))
										.withProperty("Email", buildUniqueEmail("rodolfo"))
										.withProperty("MailingCountry", "U.S.")
										.build());

		// This contact should BE synced (updated) as the mailing country is
		// U.S. and the record exists in the target system
		createdContactInA.add(aContact().withProperty("FirstName", buildUniqueName(KICK_NAME,"Marito"))
										.withProperty("LastName", buildUniqueName(KICK_NAME,"Bofil"))
										.withProperty("Email", buildUniqueEmail("marito"))
										.withProperty("MailingCountry", "United States")
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

	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);

		// Delete the created contacts in A
		SubflowInterceptingChainLifecycleWrapper flow = getSubFlow("deleteContactFromAFlow");
		flow.initialise();

		final List<String> idList = new ArrayList<String>();
		for (final Map<String, String> c : createdContactInA) {
			idList.add(c.get("Id"));
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));

		// Delete the created contacts in B
		flow = getSubFlow("deleteContactFromBFlow");
		flow.initialise();

		idList.clear();
		for (final Map<String, String> c : createdContactInA) {
			final Map<String, String> contact = invokeRetrieveContactFlow(checkContactflow, c);
			if (contact != null) {
				idList.add(contact.get("Id"));
			}
		}
		flow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void testMainFlow() throws Exception {

		System.out.println("About to run poll");

		runSchedulersOnce(POLL_FLOW_NAME);

		System.out.println("Poll runned");

		// Assert first object was not synced
		assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContactInA.get(0)));

		// Assert second object was not synced
		assertEquals("The contact should not have been sync", null, invokeRetrieveContactFlow(checkContactflow, createdContactInA.get(1)));

		// Assert third object was created in target system
		Map<String, String> payload = invokeRetrieveContactFlow(checkContactflow, createdContactInA.get(2));
		assertEquals("The contact should have been sync", createdContactInA.get(2)
																			.get("Email"), payload.get("Email"));

		// Assert fourth object was updated in target system
		final Map<String, String> fourthContact = createdContactInA.get(3);
		payload = invokeRetrieveContactFlow(checkContactflow, fourthContact);
		assertEquals("The contact should have been sync (Email)", fourthContact.get("Email"), payload.get("Email"));
		assertEquals("The contact should have been sync (FirstName)", fourthContact.get("FirstName"), payload.get("FirstName"));

	}

//	private void stopSchedulers() throws MuleException {
//		final Collection<Scheduler> schedulers = muleContext.getRegistry()
//															.lookupScheduler(Schedulers.flowPollingSchedulers("businessLogicFlow"));
//
//		for (final Scheduler scheduler : schedulers) {
//			scheduler.stop();
//		}
//	}

	@SuppressWarnings("unchecked")
	private Map<String, String> invokeRetrieveContactFlow(final SubflowInterceptingChainLifecycleWrapper flow, final Map<String, String> contact) throws Exception {
		final Map<String, String> contactMap = new HashMap<String, String>();

		contactMap.put("Email", contact.get("Email"));
		flow.initialise();
		final MuleEvent event = flow.process(getTestEvent(contactMap, MessageExchangePattern.REQUEST_RESPONSE));
		final Object payload = event.getMessage()
									.getPayload();
		System.out.println("Retrieve Contacts Result for: " + contact.get("Email") + " is " + payload);
		if (payload instanceof NullPayload) {
			return null;
		} else {
			return (Map<String, String>) payload;
		}
	}

	// ***************************************************************
	// ======== ContactBuilder class ========
	// ***************************************************************

	private String buildUniqueEmail(String user) {
		String server = "fakemail";
		String kickName = "automaticcontactsync";
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

	private ContactBuilder aContact() {
		return new ContactBuilder();
	}

	private static class ContactBuilder {

		private final Map<String, String> contact = new HashMap<String, String>();

		public ContactBuilder withProperty(final String key, final String value) {
			contact.put(key, value);
			return this;
		}

		public Map<String, String> build() {
			return contact;
		}

	}

}
