
# Anypoint Template: Salesforce to Salesforce Contact Broadcast

Broadcasts changes to contacts in Salesforce A to Salesforce B in real time. The detection criteria and fields to move are configurable. Additional systems can be added to be notified of the changes. Real time synchronization is achieved via rapid polling or outbound notifications. 

This template creates the parent account and relates it to the contact being synchronized if it does not exist, or can be configured to a static parent account for the contact. 

This template uses watermarking to ensure that only the most recent items are synchronized and batch to efficiently process many records at a time.

![ef98d8f2-99a0-4d06-9889-0a12eecc8bc5-image.png](https://exchange2-file-upload-service-kprod.s3.us-east-1.amazonaws.com:443/ef98d8f2-99a0-4d06-9889-0a12eecc8bc5-image.png)

[//]: # (![]\(https://www.youtube.com/embed/_Y9DhAqrow4?wmode=transparent\)

[![YouTube Video](http://img.youtube.com/vi/_Y9DhAqrow4/0.jpg)](https://www.youtube.com/watch?v=_Y9DhAqrow4)

### License Agreement

This template is subject to the conditions of the [MuleSoft License Agreement](https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf "MuleSoft License Agreement").

Review the terms of the license before downloading and using this template. You can use this template for free with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case

As a Salesforce admin I want to synchronize contacts between two Salesfoce orgs.

This template serves as a foundation for setting an online sync of contacts from one Salesforce instance to another. Each time there is new contact or a change in an existing one, the integration polls for changes in the Salesforce source instance and the integration updates Salesforce on the target org.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this template leverages the batch module and [Outbound messaging](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging.htm).

The batch job is divided into _Process_ and _On Complete_ stages.

During the Input stage the template goes to the Salesforce Org A and queries all the existing Contacts that match the filter criteria.

During the _Process_ stage, each Salesforce contact is filtered depending on, if it has an existing matching Contact in the Salesforce Org B and if the last updated date of the contact from Salesforce Org A is greater than the one in Salesforce Org B(in case that the same contact already exists).

The last step of the _Process_ stage groups the contacts and creates or updates them in Salesforce Org B.

The integration could be also triggered by HTTP inbound connector defined in the flow that is going to trigger the application and executing the batch job with received message from Salesforce source instance.

Outbound messaging in Salesforce allows you to specify that changes to fields within Salesforce can cause messages with field values to be sent to designated external servers.

Outbound messaging is part of the workflow rule functionality in Salesforce. Workflow rules watch for specific kinds of field changes and trigger automatic Salesforce actions in this case sending contacts as an outbound message to the HTTP Listener, which then further processes this message and creates contacts in the target Salesforce org.

Finally during the _On Complete_ stage the Anypoint Template logs output statistics data into the console.

# Considerations

To make this template run, there are certain preconditions that must be considered. All of them deal with the preparations in both source and destination systems, that must be made for the integration to run smoothly. Failing to do so can lead to unexpected behavior of the template.

## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work:

- Where can I check that the field configuration for my Salesforce instance is the right one? See: [Salesforce: Checking Field Accessibility for a Particular Field](https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US "Salesforce: Checking Field Accessibility for a Particular Field")
- Can I modify the Field Access Settings? How? See: [Salesforce: Modifying Field Access Settings](https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US "Salesforce: Modifying Field Access Settings")

### As a Data Source

If the user who configured the template for the source system does not have at least _read only_ permissions for the fields that are fetched, then an _InvalidFieldFault_ API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault 
[ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='Account.Phone, Account.Rating, Account.RecordTypeId, 
Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attemptingto use a custom field, be sure to append the '__c' after the custom 
field name. Reference your WSDL or the describe call for the 
appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.

# Run it!

Simple steps to get Salesforce to Salesforce Contact Broadcast running.

See below.

## Running On Premises

Complete all properties in one of the property files, for example in mule.prod.properties file in the /src/main/resources/mule folder, and run your app with the corresponding environment variable to use it. To follow the example, this is `mule.env=prod`.

Once your app is all set and started, there is no need to do anything else. The application polls Salesforce to know if there are any newly created or updated objects and synchronizes them.

### Where to Download Anypoint Studio and the Mule Runtime

If you are a newcomer to Mule, here is where to get the tools.

- [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
- [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)

### Importing a Template into Studio

In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your Anypoint Platform credentials, search for the template, and click **Open**.

### Running on Studio

After you import your template into Anypoint Studio, follow these steps to run it:

- Locate the properties file `mule.dev.properties`, in src/main/resources.
- Complete all the properties required as per the examples in the "Properties to Configure" section.
- Right click the template project folder.
- Hover your mouse over `Run as`.
- Click `Mule Application (configure)`.
- Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`.
- Click `Run`.

### Running on Mule Standalone

Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 

## Running on CloudHub

Once your app is all set and started, you will need to define Salesforce outbound messaging and a simple workflow rule. [This article will show you how to accomplish this](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging_setting_up.htm)

The most important setting here is the `Endpoint URL` which needs to point to your application running on CloudHub, for example, `http://yourapp.cloudhub.io:80`. Additionally, try to add just few fields to the `Fields to Send` to keep it simple for begin.

Once this all is done each time when you make a change in an Account in the source Salesforce org. This account sends as a SOAP message to the HTTP endpoint running the application in CloudHub.

### Deploying your Anypoint Template on CloudHub

Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this

## Properties to Configure

To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.

### Application Configuration

**Application configuration**

- http.port `9090` 
- page.size `190`
- scheduler.frequency `60000`
- scheduler.start.delay `0`
- watermark.default.expression `YESTERDAY`
- account.sync.policy `syncAccount`
- trigger.policy `push` | `poll`

**Note:** the property **account.sync.policy** can take any of the two following values: 

- **empty_value**: If the property has no value assigned to it then application does nothing in what respect to the account and it'll just move the contact over.
- **syncAccount**: It tries to create the contact's account when it is not presented in the Salesforce instance B.

**Note:** The property **trigger.policy** can take any of the three following values:

- **empty_value**: If the property has no value assigned to it, then the application does nothing in respect to the account and it just moves the contact over.
- **poll**: The Scheduler trigger flow is used.
- **push**: The Push Notification trigger flow is used.

**Salesforce Connector configuration for company A**

- sfdc.a.username `bob.dylan@orga`
- sfdc.a.password `DylanPassword123`
- sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`

**Salesforce Connector configuration for company B**

- sfdc.b.username `joan.baez@orgb`
- sfdc.b.password `JoanBaez456`
- sfdc.b.securityToken `ces56arl7apQs56XTddf34X`

# API Calls

Salesforce imposes limits on the number of API calls that can be made. Therefore calculating this amount may be an important factor to consider. The template calls to the API can be calculated using the formula:

_**1 + X + X / 200**_

_**X**_ is the number of Contacts to be synchronized on each run. 

Divide by _**200**_ because, by default, contacts are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.    

For instance if 10 records are fetched from origin instance, then 12 API calls are made (1 + 10 + 1).

# Customize It!

This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.

As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

- config.xml
- businessLogic.xml
- endpoints.xml
- errorHandling.xml

## config.xml

Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the _Global Element_ tab.

## businessLogic.xml

Functional aspect of the Anypoint Template is implemented on this XML, directed by one flow that's responsible for Salesforce creations or updates. The several message processors constitute four high level actions that fully implement the logic of this Anypoint Template:

1. During the Input stage, the template goes to Salesforce Org A and queries all the existing contacts that match the filter criteria.
2. During the Process stage, each Salesforce contact is filtered depending on if it has an existing matching contact in the SFSalesforceDC Org B.
3. The last step of the Process stage groups the contacts and creates or updates them in Salesforce Org B.
4. Finally during the On Complete stage the template logs output statistics data into the console.

## endpoints.xml

This is file is conformed by four flows.

The first we call the **push** flow. This one contains an HTTP endpoint that listens for notifications from Salesforce. Each of them will be processed and thus updates or creates contacts, and execute tshe batch job process.

The second we call the **scheduler** flow. This one contains the Scheduler endpoint that periodically triggers the **salesforceQuery** flow and  executes the batch job process.

The third we call the **salesforceQuery** flow. This one contains watermarking logic that queries Salesforce for updated or created contacts that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Object Store component and updates after each Salesforce query.

The fourth we call the  **main** flow. This one executes the Batch Job which handles all the logic of it. This flow has Error Handling that basically consists of invoking the _On Error Propagate Component_ defined in _errorHandling.xml_ file.

## errorHandling.xml

This file handles how your integration reacts depending on the different exceptions.

This file provides error handling that is referenced by the main flow in the business logic.
