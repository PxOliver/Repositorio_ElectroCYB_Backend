package com.electrocyb.store.support;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SupportInfoService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final ShippingOptionRepository shippingOptionRepository;

    public SupportInfoService(PaymentMethodRepository paymentMethodRepository,
                              ShippingOptionRepository shippingOptionRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.shippingOptionRepository = shippingOptionRepository;
    }

    public String buildPaymentMethodsText() {
        List<PaymentMethod> methods = paymentMethodRepository.findByActiveTrue();

        if (methods.isEmpty()) {
            return "Por el momento no tengo métodos de pago registrados. Por favor, intenta más tarde o contacta con un asesor.";
        }

        String lista = methods.stream()
                .map(m -> "- " + m.getName() +
                        (m.getDescription() != null && !m.getDescription().isBlank()
                                ? ": " + m.getDescription()
                                : ""))
                .collect(Collectors.joining("\n"));

        return "Estos son los métodos de pago disponibles actualmente en ElectroCYB:\n\n"
                + lista
                + "\n\nSi tienes dudas sobre algún método en particular, dime cuál y te ayudo.";
    }

    public String buildShippingOverviewText() {
        List<ShippingOption> options = shippingOptionRepository.findByActiveTrue();

        if (options.isEmpty()) {
            return "Por ahora no tengo configuradas las opciones de envío. Te recomiendo contactar con un asesor para información exacta.";
        }

        String lista = options.stream()
                .map(o -> "- " + o.getRegion() + " con " + o.getCarrier()
                        + ": entre " + o.getMinDays() + " y " + o.getMaxDays() + " días, aprox. S/ " + o.getPrice())
                .collect(Collectors.joining("\n"));

        return "Estas son las opciones de envío que tenemos registradas:\n\n"
                + lista
                + "\n\nLos tiempos son aproximados y pueden variar un poco según la zona y la fecha.";
    }
}