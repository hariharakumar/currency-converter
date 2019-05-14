
* This application calls bankersalgo API to fetch conversion rate and send an email with conversion value

* Service used for currency conversion : https://bankersalgo.com/account/

* Sample GET request URL : https://bankersalgo.com/apirates2/apiAccessKey/fromCurrency

* I am using google app to send email.
  * Google app password can be generated by following steps under `How to generate app password` section in
   : https://support.google.com/accounts/answer/185833

* In application.properties,
    * specify from currency in from.currency parameter (this value should be standard currency symbol)
    * specify to currency in to.currency parameter (this value should be standard currency symbol)
    * bankersalgo.baseurl - this should not change unless bankersalgo changes it
    * mail properties can be the same if you plan to use gmail for sending emails

* Create application.properties on the machine running this application in path : /var/personal_projects/currency_converter/application.properties
* This file contains parameters that are not safe to be made public.
    * bankersalgo.accesskey - This can be fetched from bankersalgo account page (https://bankersalgo.com/account/)
    * support.email - email associated to the account for which google app is created
    * spring.mail.username - same as above
    * spring.mail.password - google app password generated in point 4 above.

* sample-application.properties in the project has placeholders for properties that are not safe to be made public