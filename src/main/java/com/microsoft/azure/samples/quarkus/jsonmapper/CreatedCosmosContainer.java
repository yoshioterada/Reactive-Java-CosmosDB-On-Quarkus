package com.microsoft.azure.samples.quarkus.jsonmapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.json.bind.annotation.JsonbProperty;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatedCosmosContainer {
    @JsonbProperty
    private String containerName;
    @JsonbProperty
    @JsonbDateFormat(value="yyyy/MM/dd HH:mm:ss", locale = "Locale.JAPAN")
    private LocalDateTime executedDateTime;
}