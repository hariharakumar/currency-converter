
* This application calls bankersalgo API to fetch conversion rate and send an email with conversion value

* In application.properties,
    * specify from currency in from.currency parameter (this value should be standard currency symbol)
    * specify to currency in to.currency parameter (this value should be standard currency symbol)
    * bankersalgo.baseurl - this should not change unless bankersalgo changes it

* create application.properties in the path below on the machine running this application :
    * path of application.properties : /var/personal_projects/currency_converter/application.properties
    * in this file , specify the access key you get from bankersalgo website in bankersalgo.accesskey - we need an account setup
          to generate the access key


* Service used for currency conversion : https://bankersalgo.com/account/

* Sample GET request URL : https://bankersalgo.com/apirates2/apiAccessKey/fromCurrency