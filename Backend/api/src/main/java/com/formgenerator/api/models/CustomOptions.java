package com.formgenerator.api.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "CustomOptions")
public class CustomOptions {
  @Id
  private long id;
  private String optionLabel;
  private String optionValue;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getOptionLabel() {
    return optionLabel;
  }

  public void setOptionLabel(String optionLabel) {
    this.optionLabel = optionLabel;
  }

  public String getOptionValue() {
    return optionValue;
  }

  public void setOptionValue(String optionValue) {
    this.optionValue = optionValue;
  }
}
