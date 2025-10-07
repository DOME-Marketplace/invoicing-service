package it.eng.dome.invoicing.util.enumappers.tmf622;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItemStateType;

public class ProductOrderItemStateTypeSerializer extends StdSerializer<ProductOrderItemStateType> {

	public ProductOrderItemStateTypeSerializer() {
		super(ProductOrderItemStateType.class);
	}

	@Override
	public void serialize(ProductOrderItemStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}