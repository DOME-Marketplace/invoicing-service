package it.eng.dome.invoicing.util.enumappers.tmf637;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf637.v4.model.ProductStatusType;

public class ProductStatusTypeSerializer extends StdSerializer<ProductStatusType> {

	public ProductStatusTypeSerializer() {
		super(ProductStatusType.class);
	}

	@Override
	public void serialize(ProductStatusType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}