package it.eng.dome.invoicing.util.enumappers.tmf622;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import it.eng.dome.tmforum.tmf622.v4.model.OrderItemActionType;

public class OrderItemActionTypeSerializer extends StdSerializer<OrderItemActionType> {

	public OrderItemActionTypeSerializer() {
		super(OrderItemActionType.class);
	}

	@Override
	public void serialize(OrderItemActionType value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeString(value.getValue());
	}
}