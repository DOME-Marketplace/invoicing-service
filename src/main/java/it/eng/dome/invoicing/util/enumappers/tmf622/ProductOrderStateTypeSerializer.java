package it.eng.dome.invoicing.util.enumappers.tmf622;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderStateType;

public class ProductOrderStateTypeSerializer extends StdSerializer<ProductOrderStateType> {

	public ProductOrderStateTypeSerializer() {
		super(ProductOrderStateType.class);
	}

	@Override
	public void serialize(ProductOrderStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}