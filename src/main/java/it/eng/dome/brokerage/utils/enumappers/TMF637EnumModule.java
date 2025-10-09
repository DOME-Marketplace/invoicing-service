package it.eng.dome.brokerage.utils.enumappers;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf637.v4.model.ProductStatusType;

class ProductStatusTypeDeserializer extends StdDeserializer<ProductStatusType> {

	public ProductStatusTypeDeserializer() {
		super(ProductStatusType.class);
	}

	@Override
	public ProductStatusType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductStatusType.fromValue(jp.getText());
	}

}

class ProductStatusTypeSerializer extends StdSerializer<ProductStatusType> {

	public ProductStatusTypeSerializer() {
		super(ProductStatusType.class);
	}

	@Override
	public void serialize(ProductStatusType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

public class TMF637EnumModule extends SimpleModule {

    public TMF637EnumModule() {
        super(TMF637EnumModule.class.getName());
		this.addDeserializer(ProductStatusType.class, new ProductStatusTypeDeserializer());
		this.addSerializer(ProductStatusType.class, new ProductStatusTypeSerializer());
    }

}
