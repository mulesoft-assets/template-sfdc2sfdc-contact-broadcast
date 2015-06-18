/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is to validate the correct behavior of the Anypoint Template that make calls to external systems.
 * 
 * @author miguel.oliva
 */
public class BusinessLogicTestDoNotCreateAccountIT extends AbstractTemplateTestCase {

	private BatchTestHelper helper;

	private List<Map<String, Object>> createdContactsInA = new ArrayList<Map<String, Object>>();

	@BeforeClass
	public static void beforeClass() {
		System.setProperty("account.sync.policy", "");
		System.setProperty("trigger.policy", "poll");
	}

	@AfterClass
	public static void shutDown() {
		System.clearProperty("account.sync.policy");
		System.clearProperty("trigger.policy");
	}

	@Before
	public void setUp() throws Exception {
		helper = new BatchTestHelper(muleContext);
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();

		// Flow to retrieve contacts from target system after sync in g
		retrieveContactFromBFlow = getSubFlow("retrieveContactFromBFlow");
		retrieveContactFromBFlow.initialise();

		retrieveAccountFlowFromB = getSubFlow("retrieveAccountFlowFromB");
		retrieveAccountFlowFromB.initialise();

		createTestDataInSandBox();
	}

	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		deleteTestDataFromSandBox();
	}

	@Test
	public void testMainFlow() throws Exception {
		// Run poll and wait for it to run
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();

		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();

		assertEquals("The contact should not have been sync", null, invokeRetrieveFlow(retrieveContactFromBFlow, createdContactsInA.get(0)));

		assertEquals("The contact should not have been sync", null, invokeRetrieveFlow(retrieveContactFromBFlow, createdContactsInA.get(1)));

		Map<String, Object> payload = invokeRetrieveFlow(retrieveContactFromBFlow, createdContactsInA.get(2));
		assertEquals("The contact should have been sync", createdContactsInA.get(2)
																			.get("Email"), payload.get("Email"));

		Map<String, Object> fourthContact = createdContactsInA.get(3);
		payload = invokeRetrieveFlow(retrieveContactFromBFlow, fourthContact);
		assertEquals("The contact should have been sync (Email)", fourthContact.get("Email"), payload.get("Email"));
		assertEquals("The contact should have been sync (FirstName)", fourthContact.get("FirstName"), payload.get("FirstName"));

	}

	@SuppressWarnings("unchecked")
	private void createTestDataInSandBox() throws InitialisationException, MuleException, Exception {
		// Create object in target system to be updated
		Map<String, Object> contact_3_B = createContact("B", 3);
		contact_3_B.put("MailingCountry", "United States");
		List<Map<String, Object>> createdContactInB = new ArrayList<Map<String, Object>>();
		createdContactInB.add(contact_3_B);

		SubflowInterceptingChainLifecycleWrapper createContactInBFlow = getSubFlow("createContactFlowB");
		createContactInBFlow.initialise();
		createContactInBFlow.process(getTestEvent(createdContactInB, MessageExchangePattern.REQUEST_RESPONSE));

		// Create contacts in source system to be or not to be synced

		// This contact should not be sync
		Map<String, Object> contact_0_A = createContact("A", 0);
		contact_0_A.put("MailingCountry", "Argentina");

		// This contact should not be sync
		Map<String, Object> contact_1_A = createContact("A", 1);
		contact_1_A.put("MailingCountry", "Argentina");

		// This contact should BE sync
		Map<String, Object> contact_2_A = createContact("A", 2);

		// This contact should BE sync (updated)
		Map<String, Object> contact_3_A = createContact("A", 3);
		contact_3_A.put("Email", contact_3_B.get("Email"));

		createdContactsInA.add(contact_0_A);
		createdContactsInA.add(contact_1_A);
		createdContactsInA.add(contact_2_A);
		createdContactsInA.add(contact_3_A);

		SubflowInterceptingChainLifecycleWrapper createContactInAFlow = getSubFlow("createContactFlowA");
		createContactInAFlow.initialise();

		MuleEvent event = createContactInAFlow.process(getTestEvent(createdContactsInA, MessageExchangePattern.REQUEST_RESPONSE));

		List<SaveResult> results = (List<SaveResult>) event.getMessage()
															.getPayload();
		System.out.println("Results from creation in A" + results.toString());
		for (int i = 0; i < results.size(); i++) {
			createdContactsInA.get(i)
								.put("Id", results.get(i)
													.getId());
		}
		System.out.println("Results after adding" + createdContactsInA.toString());
	}

	private void deleteTestDataFromSandBox() throws MuleException, Exception {
		deleteTestContactsFromSandBoxA(createdContactsInA);
		deleteTestContactsFromSandBoxB(createdContactsInA);
	}

}
