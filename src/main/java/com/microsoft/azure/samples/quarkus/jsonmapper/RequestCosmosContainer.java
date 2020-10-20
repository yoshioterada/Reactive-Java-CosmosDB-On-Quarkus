package com.microsoft.azure.samples.quarkus.jsonmapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestCosmosContainer {
    private String containerName;
    private String partitionName;
    private int requestUnit;
}