package it.eng.dome.invoicing.engine.controller;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderStateType;

class ProductOrderStateDeserializer extends StdDeserializer<ProductOrderStateType> {

	public ProductOrderStateDeserializer() {
		super(ProductOrderStateType.class);
	}

	@Override
	public ProductOrderStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductOrderStateType.fromValue(jp.getText());
	}

}

class ProductOrderStateSerializer extends StdSerializer<ProductOrderStateType> {

	public ProductOrderStateSerializer() {
		super(ProductOrderStateType.class);
	}

	@Override
	public void serialize(ProductOrderStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

@Component
public class TmfEnumMapper {

	@Bean
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();

		// ProductOrderStateType
		module.addDeserializer(ProductOrderStateType.class, new ProductOrderStateDeserializer());
		module.addSerializer(ProductOrderStateType.class, new ProductOrderStateSerializer());

		// TODO: add mappers for all used TMF enumerations

		// TODO: investigate if (de-)serializers above can be done with java generics 

		return module;
	}

}
