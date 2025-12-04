package com.electrocyb.store.support;

import jakarta.persistence.*;

@Entity
@Table(name = "shipping_options")
public class ShippingOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String region;      // Ej: "Lima", "Provincia"
    private String carrier;     // Ej: "Olva", "Serpost"
    private int minDays;        // Ej: 2
    private int maxDays;        // Ej: 5
    private double price;       // Ej: 15.0
    private boolean active;

    // getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public int getMinDays() { return minDays; }
    public void setMinDays(int minDays) { this.minDays = minDays; }

    public int getMaxDays() { return maxDays; }
    public void setMaxDays(int maxDays) { this.maxDays = maxDays; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
