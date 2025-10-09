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

import it.eng.dome.tmforum.tmf622.v4.model.OrderItemActionType;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItemStateType;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderStateType;

/**
 * PRODUCT ORDER STATE
 */

class ProductOrderStateTypeDeserializer extends StdDeserializer<ProductOrderStateType> {

	public ProductOrderStateTypeDeserializer() {
		super(ProductOrderStateType.class);
	}

	@Override
	public ProductOrderStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductOrderStateType.fromValue(jp.getText());
	}

}

class ProductOrderStateTypeSerializer extends StdSerializer<ProductOrderStateType> {

	public ProductOrderStateTypeSerializer() {
		super(ProductOrderStateType.class);
	}

	@Override
	public void serialize(ProductOrderStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

/**
 * PRODUCT ORDER ITEM STATE
 */

class ProductOrderItemStateTypeSerializer extends StdSerializer<ProductOrderItemStateType> {

	public ProductOrderItemStateTypeSerializer() {
		super(ProductOrderItemStateType.class);
	}

	@Override
	public void serialize(ProductOrderItemStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

class ProductOrderItemStateTypeDeserializer extends StdDeserializer<ProductOrderItemStateType> {

	public ProductOrderItemStateTypeDeserializer() {
		super(ProductOrderItemStateType.class);
	}

	@Override
	public ProductOrderItemStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductOrderItemStateType.fromValue(jp.getText());
	}

}

/**
 * ORDER ITEM ACTION
 */

 class OrderItemActionTypeDeserializer extends StdDeserializer<OrderItemActionType> {

	public OrderItemActionTypeDeserializer() {
		super(OrderItemActionType.class);
	}

	@Override
	public OrderItemActionType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return OrderItemActionType.fromValue(jp.getText());
	}

}

class OrderItemActionTypeSerializer extends StdSerializer<OrderItemActionType> {

	public OrderItemActionTypeSerializer() {
		super(OrderItemActionType.class);
	}

	@Override
	public void serialize(OrderItemActionType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

public class TMF622EnumModule extends SimpleModule {

    public TMF622EnumModule() {

        super(TMF622EnumModule.class.getName());

		this.addDeserializer(ProductOrderStateType.class, new ProductOrderStateTypeDeserializer());
		this.addSerializer(ProductOrderStateType.class, new ProductOrderStateTypeSerializer());

		this.addDeserializer(ProductOrderItemStateType.class, new ProductOrderItemStateTypeDeserializer());
		this.addSerializer(ProductOrderItemStateType.class, new ProductOrderItemStateTypeSerializer());

		this.addDeserializer(OrderItemActionType.class, new OrderItemActionTypeDeserializer());
		this.addSerializer(OrderItemActionType.class, new OrderItemActionTypeSerializer());

    }

}
