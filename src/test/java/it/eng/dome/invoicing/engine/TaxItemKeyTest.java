package it.eng.dome.invoicing.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import it.eng.dome.invoicing.engine.model.TaxItemKey;
import it.eng.dome.tmforum.tmf678.v4.model.Money;
import it.eng.dome.tmforum.tmf678.v4.model.TaxItem;

public class TaxItemKeyTest {
	
	public static void main(String[] args) {
		
		TaxItemKeyTest test=new TaxItemKeyTest();
		
		
		  try { 
			  List<TaxItem> taxItems=new ArrayList<TaxItem>();
		  
			  Money moneyA=new Money();
			  moneyA.setUnit("EUR");
			  moneyA.setValue(2f);
			  
			  TaxItem a=new TaxItem(); 
			  a.setTaxCategory("VAT"); 
			  a.setTaxRate(0.21f);
			  a.setTaxAmount(moneyA);
		  
			  Money moneyB=new Money();
			  moneyB.setUnit("EUR");
			  moneyB.setValue(1f);
			  
			  TaxItem b=new TaxItem(); 
			  b.setTaxCategory("VAT"); 
			  b.setTaxRate(0.21f);
			  b.setTaxAmount(moneyB);
		  
			  Money moneyC=new Money();
			  moneyC.setUnit("EUR");
			  moneyC.setValue(1.5f);
			  
			  TaxItem c=new TaxItem(); 
			  c.setTaxCategory("VAT2"); 
			  c.setTaxRate(0.21f);
			  c.setTaxAmount(moneyC);
		  
			  Money moneyD=new Money();
			  moneyD.setUnit("EUR");
			  moneyD.setValue(3.2f);
			  
			  TaxItem d=new TaxItem();
			  d.setTaxCategory("VAT"); 
			  d.setTaxRate(0.22f);
			  d.setTaxAmount(moneyD);
		  
			  Money moneyE=new Money();
			  moneyE.setUnit("EUR");
			  moneyE.setValue(1f);
			  
			  TaxItem e=new TaxItem(); 
			  e.setTaxCategory("VAT2");
			  e.setTaxRate(0.23f);
			  e.setTaxAmount(moneyE);
		  
			  Money moneyF=new Money();
			  moneyF.setUnit("EUR");
			  moneyF.setValue(2f);
			  
			  TaxItem f=new TaxItem(); 
			  f.setTaxCategory("VAT"); 
			  f.setTaxRate(0.22f);
			  f.setTaxAmount(moneyF);
		  
			  taxItems.add(a); 
			  taxItems.add(b); 
			  taxItems.add(c);
			  taxItems.add(d);
			  taxItems.add(e); 
			  taxItems.add(f);
		  
			 /* List<TaxItem> distinctTaxItems = 
					  taxItems.stream() 
					  .collect(Collectors.toMap(
							  ti -> new TaxItemKey(ti.getTaxRate(), ti.getTaxCategory()), 
							  ti -> ti,
							  (existing, duplicate) -> existing )) 
					  		.values() 
					  		.stream() 
					  		.toList();*/
			  List<TaxItem> aggregatedTaxItems= test.aggregateTaxItems(taxItems);
		  
			  for (TaxItem ti : aggregatedTaxItems) { 
				  System.out.println( "Rate: " +
						  ti.getTaxRate() + ", Category: " + ti.getTaxCategory() + ", Amount: " + ti.getTaxAmount().getValue()); 
				  } 
			  }catch(Exception e) {
				  e.printStackTrace();
			  }
		}
	
	/*public List<TaxItem> aggregateTaxItems(List<TaxItem> taxItems){
		List<TaxItem> aggregatedTaxItems=new ArrayList<TaxItem>();
		
		Map<TaxItemKey, TaxItem> aggregatedMap =
                taxItems.stream()
                        .collect(Collectors.toMap(
                                item -> new TaxItemKey(item.getTaxRate(), item.getTaxCategory()),
                                item -> {
                                    TaxItem copy = new TaxItem();
                                    copy.setTaxRate(item.getTaxRate());
                                    copy.setTaxCategory(item.getTaxCategory());
                                    copy.setTaxAmount(item.getTaxAmount());
                                    return copy;
                                },
                                (existing, incoming) -> {
                                    existing.setTaxAmount(
                                            existing.getTaxAmount().add(incoming.getTaxAmount())
                                    );
                                    return existing;
                                }
                        ));
		
		return aggregatedTaxItems;
	}*/
	
	public List<TaxItem> aggregateTaxItems(List<TaxItem> taxItems){
		
		Map<TaxItemKey, List<TaxItem>> grouped =
		        taxItems.stream()
		                .collect(Collectors.groupingBy(
		                        item -> new TaxItemKey(item.getTaxRate(), item.getTaxCategory())
		                ));

		List<TaxItem> result = new ArrayList<>();

		for (Map.Entry<TaxItemKey, List<TaxItem>> entry : grouped.entrySet()) {
		    TaxItem first = entry.getValue().get(0);

		    float sum = 0f;
		    for (TaxItem item : entry.getValue()) {
		        sum += item.getTaxAmount().getValue();
		    }

		    Money money = new Money();
		    money.setValue(sum);
		    money.setUnit(first.getTaxAmount().getUnit());
		    
		    TaxItem aggregated = new TaxItem();
		    aggregated.setTaxRate(entry.getKey().getTaxRate());
		    aggregated.setTaxCategory(entry.getKey().getTaxCategory());
		    aggregated.setTaxAmount(money);

		    result.add(aggregated);
		}
		
		return result;
	}

	}
