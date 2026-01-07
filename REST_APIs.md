# Invoicing Service

**Version:** 2.0.4  
**Description:** Swagger REST APIs for the invoicing-service software  


## REST API Endpoints

### Calculate Taxes Controller
| Verb | Path | Task |
|------|------|------|
| POST | `/invoicing/previewTaxes` | previewTaxes |
| POST | `/invoicing/applyTaxes` | applyTaxes |

### Get Invoices Controller
| Verb | Path | Task |
|------|------|------|
| GET | `/invoicing/invoices` | getInvoices |
| GET | `/invoicing/invoices/{billId}` | getInvoice |

### Invoicing Service Controller
| Verb | Path | Task |
|------|------|------|
| GET | `/invoicing/info` | getInfo |
| GET | `/invoicing/health` | getHealth |

