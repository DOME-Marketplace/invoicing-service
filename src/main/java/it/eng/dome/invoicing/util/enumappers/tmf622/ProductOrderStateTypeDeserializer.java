package it.eng.dome.invoicing.util.enumappers.tmf622;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderStateType;

public class ProductOrderStateTypeDeserializer extends StdDeserializer<ProductOrderStateType> {

	public ProductOrderStateTypeDeserializer() {
		super(ProductOrderStateType.class);
	}

	@Override
	public ProductOrderStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductOrderStateType.fromValue(jp.getText());
	}

}