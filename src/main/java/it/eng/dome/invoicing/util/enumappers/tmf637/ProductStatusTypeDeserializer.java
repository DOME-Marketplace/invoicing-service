package it.eng.dome.invoicing.util.enumappers.tmf637;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import it.eng.dome.tmforum.tmf637.v4.model.ProductStatusType;

public class ProductStatusTypeDeserializer extends StdDeserializer<ProductStatusType> {

	public ProductStatusTypeDeserializer() {
		super(ProductStatusType.class);
	}

	@Override
	public ProductStatusType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductStatusType.fromValue(jp.getText());
	}

}