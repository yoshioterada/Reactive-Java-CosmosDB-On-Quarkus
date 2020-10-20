package com.microsoft.azure.samples.quarkus.jsonmapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TwitterMessageForMe {
    private String id;
    private String name;
    private String message;
}