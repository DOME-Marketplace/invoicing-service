# Release Notes

**Release Notes** of the *Invoicing Service* software:


### <code>2.0.0</code> :calendar: 17/11/2025
**Improvements**
* Changed `/invoicing/applyTaxes` REST API response DTO. The `/invoicing/applyTaxes` returns `BillingResponseDTO`.
* Add `TMF678EnumModule` in the **JacksonModuleConfig** to *serialize* and *deserialize* the **TMForum enum types**.
* Usage of the new `Brokerage Utils` version: `2.2.2`.
* Manage of `BadRelatedPartyException`.


### <code>1.2.6</code> :calendar: 30/10/2025
**Improvements**
* Added a new flag `rate-manager.enable-country-guesser` in `application.yml` to enable/disable the CountryGuesser for organizations without a country characteristic.
* Updated `RateManager` to initialize `CountryGuesser` only if the flag is enabled, using `@PostConstruct`.


### <code>1.2.5</code> :calendar: 14/10/2025
**Improvements**
* Usage of the new `Brokerage Utils` version: `2.2.0`.
* Add `TmfApiConfig` class to avoid loading the **ProductInventoryApis** and **APIPartyApis** objects every time they are used in service classes.
* Add `TrailingSlashFilter` filter to remove trailing slash from request path.


### <code>1.2.4</code> :calendar: 06/10/2025
**Improvements**
* Usage of `AbstractHealthService` class from `Brokerage Utils` to manage **getInfo()** and **getHealth()** features.
* Add `TMF622EnumModule` and `TMF637EnumModule` classes in the **JacksonModuleConfig** to *serialize* and *deserialize* the **TMForum enum types**.


### <code>1.2.3</code> :calendar: 16/09/2025
**BugFixing**
* Bug Fixing to retrieve `relatedParty`.


### <code>1.2.2</code> :calendar: 24/07/2025
**Improvements**
* Generate automatic `REST_APIs.md` file from **Swagger APIs** using the `generate-rest-apis` profile.

**BugFixing**
* Improvement of **exception** management. 


### <code>1.2.1</code> :calendar: 15/07/2025
**Improvements**
* Display `ENV VARs` in the Listener at beginning.


### <code>1.2.0</code> :calendar: 09/06/2025
**Improvements**
* Set of `[2.1.0, 2.2.0)` version of `Brokerage Utils`.
* Update paths for TMForum internal services and `basePath` for building TMForum API URLs with or without **envoy** usage.


### <code>0.1.0</code> :calendar: 31/03/2025
**Improvements**
* Update of `2.0.0` version of `Brokerage Utils`.


### <code>0.0.9</code> :calendar: 07/03/2025
**BugFixing**
* Usage of `2.0.0` version of `TMF Reference` to avoid validation: commented all `validateJsonElement` methods.


### <code>0.0.8</code> :calendar: 19/02/2025
**BugFixing**
* Bug Fixing to manage null pointer exception in the TaxService methods.


### <code>0.0.7</code> :calendar: 10/02/2025
**Improvements**
* Add `StartupListener` listener to log (display) the current version of *Invoicing Service* at startup.

**BugFixing**
* Set `org.apache.coyote.http11: ERROR` to avoid the `Error parsing HTTP request header` info log.
* Implement **Serializable** interfaces for `Configurations`, `SearchResult`, and `TaxRate` classes in the `it.eng.dome.invoicing.tedb.model` package.


### <code>0.0.6</code> :calendar: 05/02/2025
**Improvements**
* Usage of the `BILLING_PREFIX` in the `application.yaml` file.

**BugFixing**
* Set pattern console to `%d{yyyy-MM-dd HH:mm:ss} [%-5level] %logger{36} - %msg%n`.


### <code>0.0.5</code> :calendar: 27/01/2025
**Improvements**
* Tax calculation on **AppliedCustomerBillingRate[]** and **Product** with path 'invoicing/applyTaxes' using the **ApplyTaxesRequestDTO** class.


### <code>0.0.4</code> :calendar: 17/01/2025
**Improvements**
* Configure `apiProxy` via `TMF_ENDPOINT` and `TMF_ENVOY` **environment variables**. With `TMF_ENVOY:true` the `TMF_ENDPOINT` is a unique path (proxy) accessible to any TMForum services. With `TMF_ENVOY:false` you have to set also `TMF_NAMESPACE`, `TMF_POSTFIX`, and `TMF_PORT` to access specific TMForum API services.


### <code>0.0.3</code> :calendar: 13/12/2024
**Feature**
* Tax calculation on **AppliedCustomerBillingRate[]** with path 'invoicing/applyTaxes'
* Preview feature (on ProductOrders) moved to path 'invoicing/previewTaxes'


### <code>0.0.2</code> :calendar: 12/12/2024
**Feature**
* Add **TEDB** (Taxes in Europe DB) client to calculate tax for the **ProductOrder** with path 'invoicing/applyTaxes'
* Included **actuator** in pom.xml to get the **health** info via REST APIs (http://localhost:9000/health).


### <code>0.0.1</code> :calendar: 03/12/2024
**Feature**
* Init project.
