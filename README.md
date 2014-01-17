# Mule Kick: SFDC to SFDC Automatic Contact Sync

+ [Use Case](#usecase)
+ [Run it!](#runit)
    * [Running on CloudHub](#runoncloudhub)
    * [Running on premise](#runonopremise)
    * [Properties to be configured](#propertiestobeconfigured)
+ [Customize It!](#customizeit)
    * [config.xml](#configxml)
    * [endpoints.xml](#endpointsxml)
    * [businessLogic.xml](#businesslogicxml)
    * [errorHandling.xml](#errorhandlingxml)


# Use Case <a name="usecase"/>
As a Salesforce admin I want to syncronize contacts between two Salesfoce orgs.

This Kick (template) should serve as a foundation for setting an online sync of Contacts from one SalesForce instance to another. The integration will be polling during a certain defined period to find if there are new Contacs or modified ones that meet the requirements configured in the query, since the last query (by using watermarking Mule feature). If there are results, they will be either created or updated in the target instance. 

Requirements have been set not only to be used as examples, but also to stablish starting point to adapt your integration to your requirements.

# Run it!

On either environment (CloudHub or On Premise) this Kick is configured to filter Contacts with a certain criteria that should be used as an example to build your own. 
In this case, Contacts will be synced from origin instance only if they have an Email set and the Mailing Country is either "United States", "US" or "U.S.". All this are set in the Query so the filtering is done on SalesForce side lowering the load of data transfered to the integration, in order to make it more efficient.



## Running on CloudHub <a name="runoncloudhub"/>

While [creating your application on CloudHub](http://www.mulesoft.org/documentation/display/current/Hello+World+on+CloudHub) (Or you can do it later as a next step), you need to go to Deployment > Advanced to set all environment variables detailed in **Properties to be configured** as well as the **mule.env**. 

Once your app is all set and started, there is no need to do anything else. Every time a Contact is created or modified, it will be automatically synchronised to SFDC Org B as long as it has an Email.


## Running on premise <a name="runonopremise"/>
Complete all properties in one of the property files, for example in [mule.prod.properties] (../blob/master/src/main/resources/mule.prod.properties) and run your app with the corresponding environment variable to use it. To follow the example, this will be `mule.env=prod`.

Once your app is all set and started, there is no need to do anything else. Every time a Contact is created or modified, it will be automatically synchronised to SFDC Org B as long as it has an Email.


## Properties to be configured (With examples) <a name="propertiestobeconfigured"/>

In order to use this Mule Kick you need to configure properties (Credentials, configurations, etc.) either in properties file or in CloudHub as Environment Variables. 
Polling Frecuency is expressed in miliseconds (different time units can be used) and the Watermark Default Expression defines the date to be used to query the first time the integration runs. [More details about polling and watermarking.](http://www.mulesoft.org/documentation/display/current/Poll+Reference)
The date format accepted in SFDC Query Language is either YYYY-MM-DDThh:mm:ss+hh:mm or you can use Constans. [More information about Dates in SFDC.](http://www.salesforce.com/us/developer/docs/officetoolkit/Content/sforce_api_calls_soql_select_dateformats.htm)
Detail list with examples:

### Application configuration
+ polling.frequency `10000`  
+ watermark.default.expression `YESTERDAY`

#### SalesForce Connector configuration for company A
+ sfdc.a.username `bob.dylan@orga`
+ sfdc.a.password `DylanPassword123`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/26.0`

#### SalesForce Connector configuration for company B
+ sfdc.b.username `joan.baez@orgb`
+ sfdc.b.password `JoanBaez456`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
+ sfdc.b.url `https://login.salesforce.com/services/Soap/u/26.0`



# Customize It!<a name="customizeit"/>

This brief guide intends to give a high level idea of how this Kick is built and how you can change it according to your needs.
As mule applications are based on XML files, this page will be organized by describing all the XML that conform the Kick.
Of course more files will be found such as Test Classes and [Mule Application Files](http://www.mulesoft.org/documentation/display/current/Application+Format), but to keep it simple we will focus on the XMLs.

Here is a list of the main XML files you'll find in this application:

* [config.xml](#configxml)
* [endpoints.xml](#endpointsxml)
* [businessLogic.xml](#businesslogicxml)
* [errorHandling.xml](#errorhandlingxml)


## config.xml<a name="configxml"/>
Configuration for Connectors and [Properties Place Holders](http://www.mulesoft.org/documentation/display/current/Configuring+Properties) are set in this file. **Even you can change the configuration here, all parameters that can be modified here are in properties file, and this is the recommended place to do it so.** Of course if you want to do core changes to the logic you will probably need to modify this file.

In the visual editor they can be found on the *Global Element* tab.

## endpoints.xml<a name="endpointsxml"/>
This flows consists mainly on the logic to bring created and updated Contacts from SFDC Instance a:
+ SFDC Query with filters applied to bring only Contacts with an email to be used to identify exiting customer on target instance. Also Mailing Country as "US", "U.S." or "United Stated" is applied as a filter in the query to provide and example as explained before.
+ Polling configured to execute the query every certain period of time.
+ Query will bring results since last integration run in oder to have only **new** updated or creations. This is done by using Mule Watermarking feature: watermark will be used in SFDC query and updated everytime the integration runs without exceptions. The actual watermark is the greatest LastModifiedDate of the Contacts brought in the query done. This can be addapted to different needs and there is an interesting [blog](http://blogs.mulesoft.org/data-synchronizing-made-easy-with-mule-watermarks/) about this feature.
+ Default watermark expression is configured here as well.

## businessLogic.xml<a name="businesslogicxml"/>
Creation/update of Contacts is managed on this file. For all the records received on the inbound stage, two high level steps (flows) do the work here:

+ **processDataFlow:** For each Contact received the integration check if it does not exists on target system or if it does to add the foreign Id (Query to SFDC Target instance). After this, LastModifiedDate value is removed to accomplish SFDC API restrictions and is added to a List that contains all Contacts to be synced.

+ **outboundFlow:** Once all the records have been processed, the list where they were being gathered is set as payload and they are migrated to the target instance using Upsert method of SFDC Connector.


## errorHandling.xml<a name="errorhandlingxml"/>
Contains a [Catch Exception Strategy](http://www.mulesoft.org/documentation/display/current/Catch+Exception+Strategy) that is only Logging the exception thrown (If so). As you imagine, this is the right place to handle how your integration will react depending on the different exceptions. 