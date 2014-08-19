/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the
 * Anypoint Template that make calls to external systems.
 * 
 * @author Vlado Andoga
 */
@SuppressWarnings("unchecked")
public class BusinessLogicPushNotificationIT extends AbstractTemplateTestCase {
	
	private static final int TIMEOUT_MILLIS = 60;
	private static final String USER_EMAIL = "sfdc2sfdcuserbroad@test.com";
	private static final String ACCOUNT_ID_IN_B = "0012000001AHHm1AAH";
	private BatchTestHelper helper;
	private Flow triggerPushFlow;
	List<Map<String, Object>> createdContactsInB = new ArrayList<Map<String, Object>>();
	
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("trigger.policy", "push");
		System.setProperty("account.sync.policy", "assignDummyAccount");
		System.setProperty("account.id.in.b", ACCOUNT_ID_IN_B);
	}

	@AfterClass
	public static void shutDown() {
		System.clearProperty("trigger.policy");
		System.clearProperty("account.sync.policy");
		System.clearProperty("account.id.in.b");
	}

	@Before
	public void setUp() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		helper = new BatchTestHelper(muleContext);
		triggerPushFlow = getFlow("triggerPushFlow");
		initialiseSubFlows();
	}

	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		deleteTestDataFromSandBox();
	}
	
	/**
	 * Inits all tests sub-flows.
	 * @throws Exception when initialisation is unsuccessful 
	 */
	private void initialiseSubFlows() throws Exception {
		retrieveContactFromBFlow = getSubFlow("retrieveContactFromBFlow");
		retrieveContactFromBFlow.initialise();
	}

	/**
	 * In test, we are creating new SOAP message to create/update an existing contact. Contact first name is always generated
	 * to ensure, that flow correctly updates contact in the Saleforce. 
	 * @throws Exception when flow error occurred
	 */
	@Test
	public void testMainFlow() throws Exception {
		// Execution
		String firstName = buildUniqueName();
		MuleMessage message = new DefaultMuleMessage(buildRequest(firstName), muleContext);
		MuleEvent testEvent = getTestEvent(message, MessageExchangePattern.REQUEST_RESPONSE);
		triggerPushFlow.process(testEvent);
		
		helper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
		helper.assertJobWasSuccessful();

		Map<String, Object> contactToRetrieveMail = new HashMap<String, Object>();
		contactToRetrieveMail.put("Email", USER_EMAIL);

		MuleEvent event = retrieveContactFromBFlow.process(getTestEvent(contactToRetrieveMail, MessageExchangePattern.REQUEST_RESPONSE));

		Map<String, Object> payload = (Map<String, Object>) event.getMessage().getPayload();
		
		// Track created records for a cleanup.
		Map<String, Object> createdContact = new HashMap<String, Object>();
		createdContact.put("Id", payload.get("Id"));
		createdContactsInB.add(createdContact);

		// Assertions
		assertEquals("The user should have been sync and new name must match", firstName, payload.get("FirstName"));
	}

	/**
	 * Builds the soap request as a string
	 * @param firstName the first name
	 * @return a soap message as string
	 */
	private String buildRequest(String firstName){
		StringBuilder request = new StringBuilder();
		request.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		request.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
		request.append("<soapenv:Body>");
		request.append("  <notifications xmlns=\"http://soap.sforce.com/2005/09/outbound\">");
		request.append("   <OrganizationId>00Dd0000000dtDqEAI</OrganizationId>");
		request.append("   <ActionId>04kd0000000PCgvAAG</ActionId>");
		request.append("   <SessionId xsi:nil=\"true\"/>");
		request.append("   <EnterpriseUrl>https://na14.salesforce.com/services/Soap/c/30.0/00Dd0000000dtDq</EnterpriseUrl>");
		request.append("   <PartnerUrl>https://na14.salesforce.com/services/Soap/u/30.0/00Dd0000000dtDq</PartnerUrl>");
		request.append("   <Notification>");
		request.append("     <Id>04l2000000KFjMbAAL</Id>");
		request.append("     <sObject xsi:type=\"sf:Contact\" xmlns:sf=\"urn:sobject.enterprise.soap.sforce.com\">");
		request.append("       <sf:Id>0032000001GgOe4AAF</sf:Id>");
		request.append("       <sf:AccountId>0012000001BJaC6AAL</sf:AccountId>");
		request.append("       <sf:Description>Description</sf:Description>");
		request.append("       <sf:FirstName>" +firstName+ "</sf:FirstName>");
		request.append("       <sf:LastName>Lehmann</sf:LastName>");
		request.append("       <sf:Email>" + USER_EMAIL + "</sf:Email>");
		request.append("     </sObject>");
		request.append("   </Notification>");
		request.append("  </notifications>");
		request.append(" </soapenv:Body>");
		request.append("</soapenv:Envelope>");
		return request.toString();
	}
	
	/**
	 * Builds unique name based on current time stamp.
	 * @return a unique name as string
	 */
	private String buildUniqueName() {
		return TEMPLATE_NAME + "-" + System.currentTimeMillis();
	}
	
	/**
	 * Deletes data created by the tests.
	 * @throws Exception when an error occurred during clean up.
	 */
	private void deleteTestDataFromSandBox() throws Exception {
		deleteTestContactsFromSandBoxB(createdContactsInB);
	}
	
}
