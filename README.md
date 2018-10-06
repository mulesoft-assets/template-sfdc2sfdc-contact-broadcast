
# Anypoint Template: Salesforce to Salesforce Contact Broadcast

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
As a Salesforce admin I want to synchronize contacts between two Salesfoce orgs.

This Anypoint Template should serve as a foundation for setting an online sync of Contact from one Salesforce instance to another. Everytime there is new contact or a change in an already existing one, the integration will poll for changes in Salesforce source instance and it will be responsible for updating the Salesforce on the target org.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this Anypoint Template leverages the [Batch Module](http://www.mulesoft.org/documentation/display/current/Batch+Processing) and [Outbound messaging](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging.htm)
The batch job is divided in Process and On Complete stages.
During the Input stage the Anypoint Template will go to the Salesforce Org A and query all the existing Contacts that match the filter criteria.
During the Process stage, each SFDC Contact will be filtered depending on, if it has an existing matching Contact in the SFDC Org B and if the last updated date of the contact from SFDC Org A is greater than the one in SFDC Org B(in case that the same contact already exists).
The last step of the Process stage will group the contacts and create/update them in SFDC Org B.
The integration could be also triggered by HTTP inbound connector defined in the flow that is going to trigger the application and executing the batch job with received message from Salesforce source instance.
Outbound messaging in Salesforce allows you to specify that changes to fields within Salesforce can cause messages with field values to be sent to designated external servers.
Outbound messaging is part of the workflow rule functionality in Salesforce. Workflow rules watch for specific kinds of field changes and trigger automatic Salesforce actions in this case sending contacts as an outbound message to Mule HTTP inbound connector,
which will then further process this message and creates Contacts in target Salesforce org.
Finally during the On Complete stage the Anypoint Template will log output statistics data into the console.

# Considerations

To make this Anypoint Template run, there are certain preconditions that must be considered. All of them deal with the preparations in both source and destination systems, that must be made in order for all to run smoothly. **Failling to do so could lead to unexpected behavior of the template.**



## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Reference your WSDL or the describe call for the appropriate names.'
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
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

Once your app is all set and started, there is no need to do anything else. The application will poll Salesforce to know if there are any newly created or updated objects and synchronice them.


### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)


### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.


### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`


### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub
Once your app is all set and started, you will need to define Salesforce outbound messaging and a simple workflow rule. [This article will show you how to accomplish this](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging_setting_up.htm)
The most important setting here is the `Endpoint URL` which needs to point to your application running on Cloudbhub, eg. `http://yourapp.cloudhub.io:80`. Additionaly, try to add just few fields to the `Fields to Send` to keep it simple for begin.
Once this all is done every time when you will make a change on Account in source Salesforce org. This account will be sent as a SOAP message to the Http endpoint of running application in Cloudhub.


### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this


## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
**Application configuration**

+ http.port `9090` 
+ page.size `190`
+ scheduler.frequency `60000`
+ scheduler.start.delay `0`
+ watermark.default.expression `YESTERDAY`
+ account.sync.policy `syncAccount`
+ trigger.policy `push` | `poll`

**Note:** the property **account.sync.policy** can take any of the two following values: 

+ **empty_value**: if the propety has no value assigned to it then application will do nothing in what respect to the account and it'll just move the contact over.
+ **syncAccount**: it will try to create the contact's account when it is not pressented in the Salesforce instance B.

**Note:** the property **trigger.policy** can take any of the three following values:

+ **empty_value**: if the propety has no value assigned to it then application will do nothing in what respect to the account and it'll just move the contact over.
+ **poll**: the Scheduler trigger flow will be used
+ **push**: the Push Notification trigger flow will be used


**Salesforce Connector configuration for company A**

+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`

**Salesforce Connector configuration for company B**

+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`

# API Calls
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The Anypoint Template calls to the API can be calculated using the formula:

***1 + X + X / 200***

Being ***X*** the number of Contacts to be synchronized on each run. 

The division by ***200*** is because, by default, Contacts are gathered in groups of 200 for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).


# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml


## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.


## businessLogic.xml
Functional aspect of the Anypoint Template is implemented on this XML, directed by one flow that will be responsible for Salesforce creations/updates. The several message processors constitute four high level actions that fully implement the logic of this Anypoint Template:

1. During the Input stage the Anypoint Template will go to the Salesforce Org A and query all the existing Contacts that match the filter criteria.
2. During the Process stage, each SFDC contact will be filtered depending on, if it has an existing matching contact in the SFDC Org B.
3. The last step of the Process stage will group the contacts and create/update them in Salesforce Org B.
4. Finally during the On Complete stage the Anypoint Template will log output statistics data into the console.



## endpoints.xml
This is file is conformed by four flows.

The first one we'll call it **push** flow. This one contains an HTTP endpoint that will be listening for notifications from Salesforce . Each of them will be processed and thus update/create Contacts, and then executing the batch job process.

The second one we'll call it **scheduler** flow. This one contains the Scheduler endpoint that will periodically trigger **salesforceQuery** flow and then executing the batch job process.

The third one we'll call it **salesforceQuery** flow. This one contains watermarking logic that will be querying Salesforce for updated/created Contacts that meet the defined criteria in the query since the last polling. The last invocation timestamp is stored by using Objectstore Component and updated after each Salesforce query.

The fourth one we'll call it  **main** flow. This one executes the Batch Job which handles all the logic of it. This flow has Error Handling that basically consists on invoking the *On Error Propagate Component* defined in *errorHandling.xml* file.



## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.




