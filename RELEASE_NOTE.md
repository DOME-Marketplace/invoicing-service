# Release Notes

**Release Notes** of the *Invoicing Service* software:

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
