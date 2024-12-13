# Release Notes

**Release Notes** of the *Invoicing Service* software:

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
