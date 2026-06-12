package com.tairui.server.device.dehumidifier;

public class ThData {
    private Double temperature;
    private Double humidity;

    public ThData(Double temperature, Double humidity) {
        this.temperature = temperature;
        this.humidity = humidity;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }
}
