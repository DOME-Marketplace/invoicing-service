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
import it.eng.dome.tmforum.tmf622.v4.model.ProductStatusType;
import it.eng.dome.tmforum.tmf622.v4.model.TaskStateType;

/**
 * PRODUCT ORDER STATE
 */

class TMF622ProductOrderStateTypeDeserializer extends StdDeserializer<ProductOrderStateType> {

	public TMF622ProductOrderStateTypeDeserializer() {
		super(ProductOrderStateType.class);
	}

	@Override
	public ProductOrderStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductOrderStateType.fromValue(jp.getText());
	}

}

class TMF622ProductOrderStateTypeSerializer extends StdSerializer<ProductOrderStateType> {

	public TMF622ProductOrderStateTypeSerializer() {
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

class TMF622ProductOrderItemStateTypeSerializer extends StdSerializer<ProductOrderItemStateType> {

	public TMF622ProductOrderItemStateTypeSerializer() {
		super(ProductOrderItemStateType.class);
	}

	@Override
	public void serialize(ProductOrderItemStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

class TMF622ProductOrderItemStateTypeDeserializer extends StdDeserializer<ProductOrderItemStateType> {

	public TMF622ProductOrderItemStateTypeDeserializer() {
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

 class TMF622OrderItemActionTypeDeserializer extends StdDeserializer<OrderItemActionType> {

	public TMF622OrderItemActionTypeDeserializer() {
		super(OrderItemActionType.class);
	}

	@Override
	public OrderItemActionType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return OrderItemActionType.fromValue(jp.getText());
	}

}

class TMF622OrderItemActionTypeSerializer extends StdSerializer<OrderItemActionType> {

	public TMF622OrderItemActionTypeSerializer() {
		super(OrderItemActionType.class);
	}

	@Override
	public void serialize(OrderItemActionType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

/**
 * PRODUCT STATUS
 */

 class TMF622ProductStatusTypeDeserializer extends StdDeserializer<ProductStatusType> {

	public TMF622ProductStatusTypeDeserializer() {
		super(ProductStatusType.class);
	}

	@Override
	public ProductStatusType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return ProductStatusType.fromValue(jp.getText());
	}

}

class TMF622ProductStatusTypeSerializer extends StdSerializer<ProductStatusType> {

	public TMF622ProductStatusTypeSerializer() {
		super(ProductStatusType.class);
	}

	@Override
	public void serialize(ProductStatusType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

/**
 * TASK STATE
 */

 class TMF622TaskStateTypeDeserializer extends StdDeserializer<TaskStateType> {

	public TMF622TaskStateTypeDeserializer() {
		super(TaskStateType.class);
	}

	@Override
	public TaskStateType deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return TaskStateType.fromValue(jp.getText());
	}

}

class TMF622TaskStateTypeSerializer extends StdSerializer<TaskStateType> {

	public TMF622TaskStateTypeSerializer() {
		super(TaskStateType.class);
	}

	@Override
	public void serialize(TaskStateType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}

public class TMF622EnumModule extends SimpleModule {

    public TMF622EnumModule() {

        super(TMF622EnumModule.class.getName());

		this.addDeserializer(ProductOrderStateType.class, new TMF622ProductOrderStateTypeDeserializer());
		this.addSerializer(ProductOrderStateType.class, new TMF622ProductOrderStateTypeSerializer());

		this.addDeserializer(ProductOrderItemStateType.class, new TMF622ProductOrderItemStateTypeDeserializer());
		this.addSerializer(ProductOrderItemStateType.class, new TMF622ProductOrderItemStateTypeSerializer());

		this.addDeserializer(OrderItemActionType.class, new TMF622OrderItemActionTypeDeserializer());
		this.addSerializer(OrderItemActionType.class, new TMF622OrderItemActionTypeSerializer());

		this.addDeserializer(ProductStatusType.class, new TMF622ProductStatusTypeDeserializer());
		this.addSerializer(ProductStatusType.class, new TMF622ProductStatusTypeSerializer());

		this.addDeserializer(TaskStateType.class, new TMF622TaskStateTypeDeserializer());
		this.addSerializer(TaskStateType.class, new TMF622TaskStateTypeSerializer());

    }

}
