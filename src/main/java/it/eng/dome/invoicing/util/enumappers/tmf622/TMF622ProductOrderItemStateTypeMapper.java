package it.eng.dome.invoicing.util.enumappers.tmf622;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.module.SimpleModule;

import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItemStateType;

@Component
public class TMF622ProductOrderItemStateTypeMapper {

	@Bean("TMF622ProductOrderItemStateTypeMapperModule")
	public com.fasterxml.jackson.databind.Module getModule() {
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ProductOrderItemStateType.class, new ProductOrderItemStateTypeDeserializer());
		module.addSerializer(ProductOrderItemStateType.class, new ProductOrderItemStateTypeSerializer());
		return module;
	}

}
