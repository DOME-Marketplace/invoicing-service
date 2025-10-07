package it.eng.dome.invoicing.util.enumappers.tmf622;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf622.v4.model.OrderItemActionType;

@Component
public class TMF622OrderItemActionTypeMapper {

	@Bean("TMF622OrderItemActionTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(OrderItemActionType.class, new OrderItemActionTypeDeserializer());
		module.addSerializer(OrderItemActionType.class, new OrderItemActionTypeSerializer());
		return module;
	}

}
