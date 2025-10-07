package it.eng.dome.invoicing.util.enumappers.tmf622;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItemStateType;

public class ProductOrderItemStateTypeDeserializer extends StdDeserializer<ProductOrderItemStateType> {

	public ProductOrderItemStateTypeDeserializer() {
		super(ProductOrderItemStateType.class);
	}

	@Override
	public ProductOrderItemStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductOrderItemStateType.fromValue(jp.getText());
	}

}