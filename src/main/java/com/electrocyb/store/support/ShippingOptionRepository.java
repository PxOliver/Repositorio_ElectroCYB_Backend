package com.electrocyb.store.support;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShippingOptionRepository extends JpaRepository<ShippingOption, Long> {

    List<ShippingOption> findByActiveTrue();
}